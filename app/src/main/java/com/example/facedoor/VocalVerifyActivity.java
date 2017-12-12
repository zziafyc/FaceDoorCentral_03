package com.example.facedoor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.IdentityListener;
import com.iflytek.cloud.IdentityResult;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.record.PcmRecorder;
import com.iflytek.cloud.record.PcmRecorder.PcmRecordListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 声纹验证页面
 * 
 * @author iFlytek &nbsp;&nbsp;&nbsp;<a href="http:/www.xfyun.cn/">讯飞语音云</a>
 */
public class VocalVerifyActivity extends Activity implements OnClickListener {
	private static final String TAG = VocalVerifyActivity.class.getSimpleName();

	// 密码类型
	// 默认为数字密码
	private int mPwdType = 3;
	// 数字密码类型为3，其他类型暂未开放
	private static final int PWD_TYPE_NUM = 3;
	// 会话类型
	private int mSST = 0;
	// 注册
	private static final int SST_ENROLL = 0;
	// 模型操作类型
	private int mModelCmd;
	// 查询模型
	private static final int MODEL_QUE = 0;
	// 用户id，唯一标识
	private String Users_Id;
	// 身份验证对象
	private IdentityVerifier mIdVerifier;
	// 数字声纹密码
	private String mNumPwd = "";
	// 数字声纹密码段，默认有5段
	private String[] mNumPwdSegs;
	// 用于验证的数字密码
	private String mVerifyNumPwd = "";
	// UI控件
	private TextView mResultEditText;
	private RadioGroup mSstTypeGroup;

	private AlertDialog mTextPwdSelectDialog;
	private Toast mToast;

	// 是否可以录音
	private boolean mCanStartRecord = false;
	// 是否可以录音
	private boolean isStartWork = false;
	// 录音采样率
	private final int SAMPLE_RATE = 16000;
	// pcm录音机
	private PcmRecorder mPcmRecorder;
	// 进度对话框
	private ProgressDialog mProDialog;
	private Button mPressToTalkButton;

	/**
	 * 下载密码监听器
	 */
	private IdentityListener mDownloadPwdListener = new IdentityListener() {

		@Override
		public void onResult(IdentityResult result, boolean islast) {
			Log.d(TAG, result.getResultString());
			mProDialog.dismiss();
			// 下载密码时，恢复按住说话触摸
			mPressToTalkButton.setClickable(true);

			switch (mPwdType) {
			case PWD_TYPE_NUM:
				StringBuffer numberString = new StringBuffer();
				try {
					JSONObject object = new JSONObject(result.getResultString());
					if (!object.has("num_pwd")) {
						mNumPwd = null;
						return;
					}

					JSONArray pwdArray = object.optJSONArray("num_pwd");
					numberString.append(pwdArray.get(0));
					for (int i = 1; i < pwdArray.length(); i++) {
						numberString.append("-" + pwdArray.get(i));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				mNumPwd = numberString.toString();
				mNumPwdSegs = mNumPwd.split("-");

				mResultEditText.setText("您的注册密码：\n" + mNumPwd + "\n请长按“按住说话”按钮进行注册\n");
				break;
			default:
				break;
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
		}

		@Override
		public void onError(SpeechError error) {
			mProDialog.dismiss();
			// 下载密码时，恢复按住说话触摸
			mPressToTalkButton.setClickable(true);
			mResultEditText.setText("密码下载失败！" + error.getPlainDescription(true));

			showTip("请检测网络是否链接,如已连接，点击“按住说话”进行重新注册");
		}
	};

	/**
	 * 声纹注册监听器
	 */
	private IdentityListener mEnrollListener = new IdentityListener() {

		@Override
		public void onResult(IdentityResult result, boolean islast) {
			Log.d(TAG, result.getResultString());

			JSONObject jsonResult = null;
			try {
				jsonResult = new JSONObject(result.getResultString());
				int ret = jsonResult.getInt("ret");

				if (ErrorCode.SUCCESS == ret) {

					final int suc = Integer.parseInt(jsonResult.optString("suc"));
					final int rgn = Integer.parseInt(jsonResult.optString("rgn"));

					if (suc == rgn) {
						mResultEditText.setText("注册成功");
						mCanStartRecord = false;
						isStartWork = false;
						mPcmRecorder.stopRecord(true);

						finish();

					} else {
						int nowTimes = suc + 1;
						int leftTimes = 5 - nowTimes;

						StringBuffer strBuffer = new StringBuffer();
						strBuffer.append("请长按“按住说话”按钮！\n");
						strBuffer.append("请读出：" + mNumPwdSegs[nowTimes - 1] + "\n");
						strBuffer.append("训练 第" + nowTimes + "遍，剩余" + leftTimes + "遍");
						mResultEditText.setText(strBuffer.toString());
					}

				} else {
					showTip(new SpeechError(ret).getPlainDescription(true));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle bundle) {
			if (SpeechEvent.EVENT_VOLUME == eventType) {
				showTip("音量：" + arg1);
			} else if (SpeechEvent.EVENT_VAD_EOS == eventType) {
				showTip("录音结束");
			}

		}

		@Override
		public void onError(SpeechError error) {
			isStartWork = false;

			StringBuffer errorResult = new StringBuffer();
			errorResult.append("注册失败！\n");
			errorResult.append("错误信息：" + error.getPlainDescription(true) + "\n");
			errorResult.append("请长按“按住说话”重新注册!");
			mResultEditText.setText(errorResult.toString());
		}

	};

	/**
	 * 声纹模型操作监听器
	 */
	private IdentityListener mModelListener = new IdentityListener() {

		@Override
		public void onResult(IdentityResult result, boolean islast) {
			Log.d(TAG, "model operation:" + result.getResultString());

			mProDialog.dismiss();

			JSONObject jsonResult = null;
			int ret = ErrorCode.SUCCESS;
			try {
				jsonResult = new JSONObject(result.getResultString());
				ret = jsonResult.getInt("ret");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			switch (mModelCmd) {
			case MODEL_QUE:
				if (ErrorCode.SUCCESS == ret) {
					showTip("模型存在");
				} else {
					showTip("模型不存在");
				}
				break;

			default:
				break;
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
		}

		@Override
		public void onError(SpeechError error) {
			mProDialog.dismiss();
			showTip(error.getPlainDescription(true));
		}
	};

	/**
	 * 录音机监听器
	 */
	private PcmRecordListener mPcmRecordListener = new PcmRecordListener() {

		@Override
		public void onRecordStarted(boolean success) {
		}

		@Override
		public void onRecordReleased() {
		}

		@Override
		public void onRecordBuffer(byte[] data, int offset, int length) {
			StringBuffer params = new StringBuffer();

			switch (mSST) {
			case SST_ENROLL:
				params.append("rgn=5,");
				params.append("ptxt=" + mNumPwd + ",");
				params.append("pwdt=" + mPwdType + ",");
				mIdVerifier.writeData("ivp", params.toString(), data, 0, length);
				break;
			default:
				break;
			}
		}

		@Override
		public void onError(SpeechError e) {
		}
	};

	/**
	 * 按压监听器
	 */
	private OnTouchListener mPressTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!isStartWork) {
					// 根据业务类型调用服务
					if (mSST == SST_ENROLL) {
						if (null == mNumPwdSegs) {
							// 启动录音机时密码为空，中断此次操作，下载密码
							downloadPwd();
							break;
						}
						vocalEnroll();
					} else {
						showTip("请先选择相应业务！");
						break;
					}
					isStartWork = true;
					mCanStartRecord = true;
				}
				if (mCanStartRecord) {
					try {
						mPcmRecorder = new PcmRecorder(SAMPLE_RATE, 40);
						mPcmRecorder.startRecording(mPcmRecordListener);
					} catch (SpeechError e) {
						e.printStackTrace();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				v.performClick();

				mIdVerifier.stopWrite("ivp");
				if (null != mPcmRecorder) {

					mPcmRecorder.stopRecord(true);
				}
				break;

			default:
				break;
			}
			return false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vocal_demo);

		// 获取用户从人脸识别界面注册的id
		Intent intent = getIntent();
		Users_Id = intent.getStringExtra("ID");

		initUI();

		mIdVerifier = IdentityVerifier.createVerifier(VocalVerifyActivity.this, new InitListener() {

			@Override
			public void onInit(int errorCode) {
				if (ErrorCode.SUCCESS == errorCode) {
					showTip("引擎初始化成功");
				} else {
					showTip("引擎初始化失败，错误码：" + errorCode);
				}
			}
		});

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void initUI() {

		mResultEditText = (TextView) findViewById(R.id.vocal_edt_result);

		mPressToTalkButton = (Button) findViewById(R.id.btn_vocal_press_to_talk);
		mPressToTalkButton.setOnTouchListener(mPressTouchListener);

		Button query = (Button) findViewById(R.id.btn_vocal_query);
		query.setOnClickListener(VocalVerifyActivity.this);

		mProDialog = new ProgressDialog(VocalVerifyActivity.this);
		mProDialog.setCancelable(true);
		mProDialog.setTitle("请稍候");
		// cancel进度框时，取消正在进行的操作
		mProDialog.setOnCancelListener(new OnCancelListener() {

			@Override
			public void onCancel(DialogInterface dialog) {
				if (null != mIdVerifier) {
					mIdVerifier.cancel();
				}
			}
		});

		// 密码选择RadioGroup初始化
		mSstTypeGroup = (RadioGroup) findViewById(R.id.vocal_radioGroup1);
		mSstTypeGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// 取消之前操作
				if (mIdVerifier.isWorking()) {
					mIdVerifier.cancel();
				}
				cancelOperation();
				switch (checkedId) {
				case R.id.vocal_radioEnroll:
					// 设置会话类型为验证
					mSST = SST_ENROLL;
					if (null == mNumPwdSegs) {
						// 首次注册密码为空时，调用下载密码
						downloadPwd();
					} else {
						mResultEditText.setText("请长按“按住说话”按钮进行注册\n");
					}
					break;
				default:
					break;
				}
			}
		});

		mToast = Toast.makeText(VocalVerifyActivity.this, "", Toast.LENGTH_SHORT);
		mToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
	}

	@Override
	public void onClick(View v) {
		// 取消先前操作
		cancelOperation();

		switch (v.getId()) {
		case R.id.btn_vocal_query:
			// 执行查询模型
			mModelCmd = MODEL_QUE;
			executeModelCommand("query");
			break;
		default:
			break;
		}
	}

	private void downloadPwd() {
		// 获取密码之前先终止之前的操作
		mIdVerifier.cancel();
		mNumPwd = null;
		// 下载密码时，按住说话触摸无效
		mPressToTalkButton.setClickable(false);
		mProDialog.setMessage("下载中...");
		mProDialog.show();
		// 设置下载密码参数
		// 清空参数
		mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
		// 设置会话场景
		mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");
		// 子业务执行参数，若无可以传空字符传
		StringBuffer params = new StringBuffer();
		// 设置模型操作的密码类型
		params.append("pwdt=" + mPwdType + ",");
		// 执行密码下载操作
		mIdVerifier.execute("ivp", "download", params.toString(), mDownloadPwdListener);
	}

	private void vocalEnroll() {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("请长按“按住说话”按钮！\n");
		strBuffer.append("请读出：" + mNumPwdSegs[0] + "\n");
		strBuffer.append("训练 第" + 1 + "遍，剩余4遍\n");
		mResultEditText.setText(strBuffer.toString());

		// 设置声纹注册参数
		// 清空参数
		mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
		// 设置会话场景
		mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");
		// 设置会话类型
		mIdVerifier.setParameter(SpeechConstant.MFV_SST, "enroll");
		// 用户id
		mIdVerifier.setParameter(SpeechConstant.AUTH_ID, Users_Id);
		// 设置监听器，开始会话
		mIdVerifier.startWorking(mEnrollListener);
	}

	private void cancelOperation() {
		isStartWork = false;
		mIdVerifier.cancel();

		if (null != mPcmRecorder) {
			mPcmRecorder.stopRecord(true);
		}
	}

	private void executeModelCommand(String cmd) {
		if ("query".equals(cmd)) {
			mProDialog.setMessage("查询中...");
		}
		mProDialog.show();
		// 设置声纹模型参数
		// 清空参数
		mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
		// 设置会话场景
		mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");
		// 用户id
		mIdVerifier.setParameter(SpeechConstant.AUTH_ID, Users_Id);

		// 子业务执行参数，若无可以传空字符传
		StringBuffer params3 = new StringBuffer();
		// 设置模型操作的密码类型
		params3.append("pwdt=" + mPwdType + ",");
		// 执行模型操作
		mIdVerifier.execute("ivp", cmd, params3.toString(), mModelListener);
	}

	@Override
	public void finish() {
		if (null != mTextPwdSelectDialog) {
			mTextPwdSelectDialog.dismiss();
		}

		setResult(RESULT_OK);
		super.finish();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (null != mPcmRecorder) {
			mPcmRecorder.stopRecord(true);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != mIdVerifier) {
			mIdVerifier.destroy();
			mIdVerifier = null;
		}
	}

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}
}
