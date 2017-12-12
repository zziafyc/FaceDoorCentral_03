package com.example.facedoor;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facedoor.base.BaseAppCompatActivity;
import com.example.facedoor.util.StringUtils;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeakerVerifier;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechListener;
import com.iflytek.cloud.VerifierListener;
import com.iflytek.cloud.VerifierResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 声纹密码示例
 *
 * @author iFlytek &nbsp;&nbsp;&nbsp;
 *         <a href="http://http://www.xfyun.cn/">讯飞语音云</a>
 */
public class IsvDemo extends BaseAppCompatActivity implements OnClickListener {
    private static final String TAG = IsvDemo.class.getSimpleName();

    private static final int PWD_TYPE_TEXT = 1;
    // 当前声纹密码类型，1、2、3分别为文本、自由说和数字密码
    private int mPwdType = PWD_TYPE_TEXT;
    // 声纹识别对象
    private SpeakerVerifier mVerifier;
    // 声纹AuthId，用户在云平台的身份标识，也是声纹模型的标识
    // 请使用英文字母或者字母和数字的组合，勿使用中文字符
    private String mAuthId = "";
    // 文本声纹密码
    private String mTextPwd = "芝麻开门";
    // 数字声纹密码
    private String mNumPwd = "";
    // 数字声纹密码段，默认有5段
    private String[] mNumPwdSegs;

    private TextView mResultEditText;
    private TextView mAuthIdTextView;
    private RadioGroup mPwdTypeGroup;
    private TextView mShowPwdTextView;
    private TextView mShowMsgTextView;
    private TextView mShowRegFbkTextView;
    private TextView mRecordTimeTextView;
    private AlertDialog mTextPwdSelectDialog;
    private Toast mToast;

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_isv_demo;
    }

    @Override
    protected void initViewsAndEvents() {
        initUi();
        // 获取用户从人脸识别界面注册的id
        Intent intent = getIntent();
        mAuthId = intent.getStringExtra("ID");

        // 初始化SpeakerVerifier，InitListener为初始化完成后的回调接口
        mVerifier = SpeakerVerifier.createVerifier(IsvDemo.this, new InitListener() {

            @Override
            public void onInit(int errorCode) {
                if (ErrorCode.SUCCESS == errorCode) {
                    //showTip("引擎初始化成功");
                } else {
                    showTip("引擎初始化失败，错误码：" + errorCode);
                }
            }
        });
    }

    private void initUi() {
        mResultEditText = (TextView) findViewById(R.id.edt_result);
        mShowPwdTextView = (TextView) findViewById(R.id.showPwd);
        mShowMsgTextView = (TextView) findViewById(R.id.showMsg);
        mShowRegFbkTextView = (TextView) findViewById(R.id.showRegFbk);
        mRecordTimeTextView = (TextView) findViewById(R.id.recordTime);

        findViewById(R.id.isv_register).setOnClickListener(IsvDemo.this);
        findViewById(R.id.isv_getpassword).setOnClickListener(IsvDemo.this);
        findViewById(R.id.isv_search).setOnClickListener(IsvDemo.this);

        // 密码选择RadioGroup初始化
        mPwdTypeGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mPwdTypeGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                initTextView();
                switch (checkedId) {
                    case R.id.radioText:
                        mPwdType = PWD_TYPE_TEXT;
                        break;
                    default:
                        break;
                }
            }
        });

        mToast = Toast.makeText(IsvDemo.this, "", Toast.LENGTH_SHORT);
    }

    /**
     * 初始化TextView和密码文本
     */
    private void initTextView() {
        mTextPwd = null;
        mResultEditText.setText("");
        mShowPwdTextView.setText("");
        mShowMsgTextView.setText("");
        mShowRegFbkTextView.setText("");
        mRecordTimeTextView.setText("");
    }

    /**
     * 设置radio的状态
     */
    private void setRadioClickable(boolean clickable) {
        // 设置RaioGroup状态为非按下状态
       /* mPwdTypeGroup.setPressed(false);
        findViewById(R.id.radioText).setClickable(clickable);*/
    }

    /**
     * 执行模型操作
     *
     * @param operation 操作命令
     * @param listener  操作结果回调对象
     */
    private void performModelOperation(String operation, SpeechListener listener) {
        // 清空参数
        mVerifier.setParameter(SpeechConstant.PARAMS, null);
        mVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);

        if (mPwdType == PWD_TYPE_TEXT) {
            // 文本密码删除需要传入密码
            if (TextUtils.isEmpty(mTextPwd)) {
                showTip("请获取密码后进行操作");
                return;
            }
            mVerifier.setParameter(SpeechConstant.ISV_PWD, mTextPwd);
        }
        setRadioClickable(false);
        // 设置auth_id，不能设置为空
        mVerifier.sendRequest(operation, mAuthId, listener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.isv_getpassword:
                // 获取密码之前先终止之前的注册或验证过程
                mVerifier.cancel();
                initTextView();
                setRadioClickable(false);
                // 清空参数
                mVerifier.setParameter(SpeechConstant.PARAMS, null);
                mVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);
                mVerifier.getPasswordList(mPwdListenter);
                break;
            case R.id.isv_search:
                performModelOperation("que", mModelOperationListener);
                break;
            case R.id.isv_register:
                // 清空参数
                mVerifier.setParameter(SpeechConstant.PARAMS, null);
                mVerifier.setParameter(SpeechConstant.ISV_AUDIO_PATH,
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/test.pcm");
                // 对于某些麦克风非常灵敏的机器，如nexus、samsung i9300等，建议加上以下设置对录音进行消噪处理
                // mVerify.setParameter(SpeechConstant.AUDIO_SOURCE, "" +
                // MediaRecorder.AudioSource.VOICE_RECOGNITION);
                if (mPwdType == PWD_TYPE_TEXT) {
                    // 文本密码注册需要传入密码
                    if (TextUtils.isEmpty(mTextPwd)) {
                        showTip("请“获取密码”后进行操作");
                        return;
                    }
                    mVerifier.setParameter(SpeechConstant.ISV_PWD, mTextPwd);
                    mShowPwdTextView.setText("请读出：" + mTextPwd);
                    mShowMsgTextView.setText("训练 第" + 1 + "遍，剩余4遍");
                }

                setRadioClickable(false);
                // 设置auth_id，不能设置为空
                mVerifier.setParameter(SpeechConstant.AUTH_ID, mAuthId);
                // 设置业务类型为注册
                mVerifier.setParameter(SpeechConstant.ISV_SST, "train");
                // 设置声纹密码类型
                mVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);
                // 开始注册
                mVerifier.startListening(mRegisterListener);
                break;

            default:
                break;
        }
    }

    private String[] items;
    private SpeechListener mPwdListenter = new SpeechListener() {
        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            setRadioClickable(true);

            String result = new String(buffer);
            switch (mPwdType) {
                case PWD_TYPE_TEXT:
                    try {
                        JSONObject object = new JSONObject(result);
                        if (!object.has("txt_pwd")) {
                            initTextView();
                            return;
                        }

                        JSONArray pwdArray = object.optJSONArray("txt_pwd");
                        items = new String[pwdArray.length()];
                        for (int i = 0; i < pwdArray.length(); i++) {
                            items[i] = pwdArray.getString(i);
                        }
                        mTextPwdSelectDialog = new AlertDialog.Builder(IsvDemo.this).setTitle("请选择密码文本")
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface arg0, int arg1) {
                                        mTextPwd = items[arg1];
                                        mResultEditText.setText("您的密码：" + mTextPwd);
                                        //设置光标始终靠右
                                        if (!StringUtils.isEmpty(mResultEditText.getText().toString())) {
                                            CharSequence text = mResultEditText.getText();
                                            if (text instanceof Spannable) {
                                                Spannable spanText = (Spannable) text;
                                                Selection.setSelection(spanText, text.length());
                                            }
                                        }
                                    }
                                }).create();
                        mTextPwdSelectDialog.show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onCompleted(SpeechError error) {
            setRadioClickable(true);

            if (null != error && ErrorCode.SUCCESS != error.getErrorCode()) {
                showTip("获取失败：" + error.getErrorCode());
            }
        }
    };

    private SpeechListener mModelOperationListener = new SpeechListener() {

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            setRadioClickable(true);

            String result = new String(buffer);
            try {
                JSONObject object = new JSONObject(result);
                String cmd = object.getString("cmd");
                int ret = object.getInt("ret");

                if ("del".equals(cmd)) {
                    if (ret == ErrorCode.SUCCESS) {
                        showTip("删除成功");
                        mResultEditText.setText("");
                    } else if (ret == ErrorCode.MSP_ERROR_FAIL) {
                        showTip("删除失败，模型不存在");
                    }
                } else if ("que".equals(cmd)) {
                    if (ret == ErrorCode.SUCCESS) {
                        showTip("模型存在");
                    } else if (ret == ErrorCode.MSP_ERROR_FAIL) {
                        showTip("模型不存在");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCompleted(SpeechError error) {
            setRadioClickable(true);

            if (null != error && ErrorCode.SUCCESS != error.getErrorCode()) {
                showTip("操作失败：" + error.getPlainDescription(true));
            }
        }
    };


    private VerifierListener mRegisterListener = new VerifierListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onResult(VerifierResult result) {
            ((TextView) findViewById(R.id.showMsg)).setText(result.source);

            if (result.ret == ErrorCode.SUCCESS) {
                switch (result.err) {
                    case VerifierResult.MSS_ERROR_IVP_GENERAL:
                        mShowMsgTextView.setText("内核异常");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_EXTRA_RGN_SOPPORT:
                        mShowRegFbkTextView.setText("训练达到最大次数");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TRUNCATED:
                        mShowRegFbkTextView.setText("出现截幅");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_MUCH_NOISE:
                        mShowRegFbkTextView.setText("太多噪音");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_UTTER_TOO_SHORT:
                        mShowRegFbkTextView.setText("录音太短");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TEXT_NOT_MATCH:
                        mShowRegFbkTextView.setText("训练失败，您所读的文本不一致");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TOO_LOW:
                        mShowRegFbkTextView.setText("音量太低");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_NO_ENOUGH_AUDIO:
                        mShowMsgTextView.setText("音频长达不到自由说的要求");
                    default:
                        mShowRegFbkTextView.setText("");
                        break;
                }

                if (result.suc == result.rgn) {
                    setRadioClickable(true);
                    mShowMsgTextView.setText("注册成功");
                    finish();

                    if (PWD_TYPE_TEXT == mPwdType) {
                        mResultEditText.setText("您的文本密码声纹ID：\n" + result.vid);
                    }

                } else {
                    int nowTimes = result.suc + 1;
                    int leftTimes = result.rgn - nowTimes;

                    if (PWD_TYPE_TEXT == mPwdType) {
//						mShowPwdTextView.setText("请读出：" + mTextPwd);
                    }

                    mShowMsgTextView.setText("训练 第" + nowTimes + "遍，剩余" + leftTimes + "遍");
                }

            } else {
                setRadioClickable(true);

                mShowMsgTextView.setText("注册失败，请重新开始。");
            }
        }

        // 保留方法，暂不用
        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle arg3) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            // String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            // Log.d(TAG, "session id =" + sid);
            // }
        }

        @Override
        public void onError(SpeechError error) {
            setRadioClickable(true);

            if (error.getErrorCode() == ErrorCode.MSP_ERROR_ALREADY_EXIST) {
                showTip("模型已存在，如需重新注册，请先删除");
            } else {
                showTip("onError Code：" + error.getPlainDescription(true));
            }
        }

        @Override
        public void onEndOfSpeech() {
            showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }
    };

    @Override
    public void finish() {
        if (null != mTextPwdSelectDialog) {
            mTextPwdSelectDialog.dismiss();
        }
        super.finish();
    }

    @Override
    protected void onDestroy() {
        if (null != mVerifier) {
            mVerifier.stopListening();
            mVerifier.destroy();
        }
        super.onDestroy();
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

}
