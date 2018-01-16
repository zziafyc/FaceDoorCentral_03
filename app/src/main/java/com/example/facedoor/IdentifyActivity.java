package com.example.facedoor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.baidu.apis.ApiClient;
import com.example.baidu.apis.CompletedListener;
import com.example.baidu.base.ErrorResult;
import com.example.facedoor.db.DBUtil;
import com.example.facedoor.util.ImageFile;
import com.example.facedoor.util.ProgressShow;
import com.example.facedoor.util.ToastShow;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.SpeakerVerifier;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.VerifierListener;
import com.iflytek.cloud.VerifierResult;
import com.iflytek.cloud.record.PcmRecorder;
import com.iflytek.cloud.record.PcmRecorder.PcmRecordListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class IdentifyActivity extends Activity {

    private static final String TAG = IdentifyActivity.class.getSimpleName();
    private static final String CROP_FACE_PATH = "/mnt/sdcard/FaceVocal/crop.jpg";

    private Toast mToast;
    private ProgressDialog mProDialog;
    private View mLayout;
    private TextView mScore;
    private Bitmap mFacePic;
    private byte[] mImageData;

    private IdentityVerifier mIdVerifier;
    private SpeechSynthesizer mSpeaker;
    private SpeechRecognizer mRecognizer;
    private String mGroupId;
    private TextView mResultEditText;
    private PcmRecorder mPcmRecorder;
    private String authId;
    private SpeakerVerifier mVerifier;
    private TextView name;
    private TextView num;
    //标志位，放置在声音监听的时候按返回键，声音监听线程还在继续
    private boolean flag = false;
    // 会话类型
    private int mSST = 0;
    // 验证
    private static final int SST_VERIFY = 1;
    private String mTextPwd = "芝麻开门";
    // 用于验证的数字密码
    private String mVerifyNumPwd = "";
    // 是否可以录音
    private boolean isStartWork = false;
    // 是否可以录音
    private boolean mCanStartRecord = false;
    // 录音采样率
    private final int SAMPLE_RATE = 16000;
    // 密码类型
    // 默认为数字密码
    private int mPwdType = PWD_TYPE_TEXT;
    private static final int PWD_TYPE_TEXT = 1;
    // 声纹验证通过时间
    private static final int VOICE_SUCCESS = 5000;
    // 声纹验证失败时间
    private static final int VOICE_FAILED = 1000;
    // 声纹验证出错时间
    private static final int VOICE_ERROR = 2000;
    // 人脸鉴别失败
    private static final int FACE_FAILED = 2000;
    // 客户等待时间
    private static final int CUSTOMER_WAITING_TIME = 20000;
    // 等待播报时间
    private static final int WAITING_TIME = 2000;
    // 声纹验证 验证码 说出时间
    private static final int IDENTIFYING_CODE = 4000;
    // 没有人脸时间
    private static final int NO_FACE = 2000;
    // 没有说话时间
    private static final int NO_SPEAKING_TIME = 10000;
    // 数据库无此人
    private static final int NOT_FOUND_IN_DB = 1000;

    private static DatagramSocket server = null;
    private static DatagramPacket recvPacket = null;
    private static InetAddress mAddress;
    private static String ip = "192.168.1.49";
    private static final int UDPPort = 5050;

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
                case SST_VERIFY:
                    params.append("ptxt=" + mVerifyNumPwd + ",");
                    params.append("pwdt=" + mPwdType + ",");
                    mIdVerifier.writeData("ivp", params.toString(), data, 0, length);
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onError(SpeechError arg0) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);
        mLayout = findViewById(R.id.layout_identify);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mScore = (TextView) findViewById(R.id.id_score);
        mResultEditText = (TextView) findViewById(R.id.vocal_edit_result);

        initUI();
        initUDP();
        getPhoto();
        mVerifier = SpeakerVerifier.createVerifier(IdentifyActivity.this, null);

        mProDialog = new ProgressDialog(this);
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
        mRecognizer = SpeechRecognizer.createRecognizer(IdentifyActivity.this, null);
        mRecognizer.setParameter(SpeechConstant.DOMAIN, "iat");
        mRecognizer.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        mRecognizer.setParameter(SpeechConstant.ACCENT, "mandarin");
        mRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "plain");

        mImageData = ImageFile.readImageFromFile("crop.jpg");
        mIdVerifier = IdentityVerifier.createVerifier(this, null);
        mSpeaker = SpeechSynthesizer.createSynthesizer(this, null);
        mSpeaker.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        mSpeaker.setParameter(SpeechConstant.SPEED, "40");
        mSpeaker.setParameter(SpeechConstant.PITCH, "50");
        mSpeaker.setParameter(SpeechConstant.VOLUME, "50");

        //在onCreate的时候就进行鉴别
        identify();
    }

    private void getPhoto() {
        ImageView imageView = (ImageView) findViewById(R.id.image);
        String path = ImageFile.getImagePath("crop.jpg");
        File file = new File(path);
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            imageView.setImageBitmap(bitmap);
        }
    }

    private void initUI() {
        name = (TextView) findViewById(R.id.name);
        num = (TextView) findViewById(R.id.num);
    }

    private static void initUDP() {
        try {
            server = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            mAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        flag = true;
        if (null != mIdVerifier) {
            mIdVerifier.destroy();
            mIdVerifier = null;
        }
        if (null != mSpeaker) {
            mSpeaker.destroy();
            mSpeaker = null;
        }
        if (null != mRecognizer) {
            mRecognizer.destroy();
            mRecognizer = null;
            server.close();
        }
    }

    private void identify() {
        if (mImageData == null) {
            ToastShow.showTip(mToast, "请选择图片后再验证");
            return;
        }
        ArrayList<String> groupIdList = null;
        groupIdList = MyApp.getDBManage(this).getGroupId();
        if (groupIdList != null && groupIdList.size() > 0) {
            mGroupId = groupIdList.get(0);
        }
        if (mGroupId == null) {
            ToastShow.showTip(mToast, "请先建立组");
            delayedFinish(3000);
        }
        ProgressShow.show(mProDialog, "鉴别中。。。");
        //人脸鉴别
        ApiClient.getApiService(getApplication()).verify(mImageData, mGroupId, new CompletedListener() {
            @Override
            public void onCompleted(String result) {
                //鉴别成功，通过result得到信息
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray jsonResult = jsonObject.getJSONArray("result");
                    JSONObject jsonResult0 = jsonResult.getJSONObject(0);
                    JSONArray jsonArray = jsonResult0.getJSONArray("scores");

                    if (jsonArray.getDouble(0) > 88.0) {
                        mScore.setText("" + jsonArray.getDouble(0));
                        String useId = jsonResult0.getString("uid");
                        faceOnlySuccess(Integer.parseInt(useId));
                    } else {
                        ProgressShow.stop(mProDialog);
                        mScore.setText("" + jsonArray.getDouble(0) + " 分数太低，验证不通过");
                        delayedFinish(3000);
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(ErrorResult errorResult) {

            }
        });
    }

    private int AuthIdToUserId(String authId) {
        return Integer.parseInt(authId.substring(1));
    }

    private void delayedFinish(final int mills) {
        if (!flag) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
                        Thread.sleep(mills);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                 /*   Intent intent = new Intent(IdentifyActivity.this, IndexActivity.class);
                    startActivity(intent);*/
                    finish();

                }
            }.start();
        }
    }

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != mPcmRecorder) {
            mPcmRecorder.stopRecord(true);
        }
    }

    private void faceOnlySuccess(int userId) {
        Observable.just(userId).map(new Func1<Integer, String>() {
            @Override
            public String call(Integer arg0) {
                DBUtil dbUtil = new DBUtil(IdentifyActivity.this);
                String userName = dbUtil.queryStaffIdAndName(arg0);
                if (userName != null) {
                    String staffID = userName.split("[;]")[0];
                    File image = new File(CROP_FACE_PATH);
                  /*  DBPorxyComm dbPorxyComm = new DBPorxyComm(IdentifyActivity.this);
                    dbPorxyComm.sendNormalMessage(staffID, image);*/
                  /*  PlatformComm platformComm = new PlatformComm(IdentifyActivity.this);
                    platformComm.sendNormalMessage(staffID, image);
                    Openable door = new DoorJH(IdentifyActivity.this);
                    door.open();*/
                }
                return userName;
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String arg0) {
                        ProgressShow.stop(mProDialog);
                        if (arg0 == null) {
                            ToastShow.showTip(mToast, "数据库无此人");
                            delayedFinish(NOT_FOUND_IN_DB);
                        } else {
                            String[] str = arg0.split("[;]");
                            for (int i = 0; i < str.length; i++) {
                                String num1 = str[0];
                                String name1 = str[1];
                                mResultEditText.setText("验证通过");
                                mSpeaker.startSpeaking("你好" + name1, null);
                                num.setText(num1);
                                name.setText(name1);
                                delayedFinish(VOICE_SUCCESS);
                            }

                        }
                    }
                });
    }

    private void faceVocalSuceess() {
        InitVerifier();
        mVerifier.startListening(mVerifyListener);


    }

    private void InitVerifier() {
        // 清空参数
        mVerifier.setParameter(SpeechConstant.PARAMS, null);
        mVerifier.setParameter(SpeechConstant.ISV_AUDIO_PATH,
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/test.pcm");
        mVerifier = SpeakerVerifier.getVerifier();
        // 设置业务类型为验证
        mVerifier.setParameter(SpeechConstant.ISV_SST, "verify");
        // 对于某些麦克风非常灵敏的机器，如nexus、samsung i9300等，建议加上以下设置对录音进行消噪处理
        // mVerify.setParameter(SpeechConstant.AUDIO_SOURCE, "" +
        // MediaRecorder.AudioSource.VOICE_RECOGNITION);
        mVerifier.setParameter(SpeechConstant.ISV_PWD, mTextPwd);
        // 设置auth_id，不能设置为空
        mVerifier.setParameter(SpeechConstant.AUTH_ID, authId);
        mVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);
        // 开始验证
    }

    private VerifierListener mVerifyListener = new VerifierListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            if (!flag) {
                showTip("语音监听中.....");
            }
        }

        @Override
        public void onResult(final VerifierResult result) {
            Log.e(TAG, "onResult: " + result.ret + "");
            if (result.ret == 0 && !flag) {
                SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                String voiceValue = config.getString(MyApp.VOICE_VALUE, "60");
                if (result.score < Double.parseDouble(voiceValue)) {
                    Log.e(TAG, "scroe: " + result.score + "");
                    mResultEditText.setText("分数太低，验证不通过");
                    delayedFinish(3000);
                    return;
                }
                int userId = AuthIdToUserId(authId);
                Integer usrID = new Integer(userId);
                Log.e(TAG, "进来了: " + "usrID:" + usrID);
                Observable.just(usrID).map(new Func1<Integer, String>() {
                    @Override
                    public String call(Integer arg0) {
                        DBUtil dbUtil = new DBUtil(IdentifyActivity.this);
                        String userName = dbUtil.queryStaffIdAndName(arg0);
                        if (userName != null) {
                            String staffID = userName.split("[;]")[0];
                            File image = new File(CROP_FACE_PATH);
                           /* DBPorxyComm dbPorxyComm = new DBPorxyComm(IdentifyActivity.this);
                            dbPorxyComm.sendNormalMessage(staffID, image);*/
                            /*PlatformComm platformComm = new PlatformComm(IdentifyActivity.this);
                            platformComm.sendNormalMessage(staffID, image);
                            Openable door = new DoorJH(IdentifyActivity.this);
                            door.open();*/
                        }
                        return userName;
                    }
                }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String arg0) {
                                if (arg0 == null) {
                                    ToastShow.showTip(mToast, "数据库无此人");
                                    delayedFinish(NOT_FOUND_IN_DB);
                                } else {
                                    String[] str = arg0.split("[;]");
                                    for (int i = 0; i < str.length; i++) {
                                        final String num1 = str[0];
                                        final String name1 = str[1];
                                        mResultEditText.setText("验证通过，声纹得分：" + result.score);
                                        ToastShow.showTip(mToast, "你好" + name1);
                                        //Openable door = new DoorZY("192.168.1.112", 6001);
                                        //Openable door = new DoorJH(IdentifyActivity.this);
                                        //door.open();
                                        num.setText(num1);
                                        name.setText(name1);
                                        mSpeaker.startSpeaking("你好" + name1, new SynthesizerListener() {
                                            @Override
                                            public void onSpeakBegin() {

                                            }

                                            @Override
                                            public void onBufferProgress(int i, int i1, int i2, String s) {

                                            }

                                            @Override
                                            public void onSpeakPaused() {

                                            }

                                            @Override
                                            public void onSpeakResumed() {

                                            }

                                            @Override
                                            public void onSpeakProgress(int i, int i1, int i2) {

                                            }

                                            @Override
                                            public void onCompleted(SpeechError speechError) {
                                                delayedFinish(VOICE_SUCCESS);
                                            }

                                            @Override
                                            public void onEvent(int i, int i1, int i2, Bundle bundle) {

                                            }
                                        });

                                    }
                                }
                            }
                        });
            } else {
                // 验证不通过
                switch (result.err) {
                    case VerifierResult.MSS_ERROR_IVP_GENERAL:
                        mResultEditText.setText("内核异常");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TRUNCATED:
                        mResultEditText.setText("出现截幅");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_MUCH_NOISE:
                        mResultEditText.setText("太多噪音");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_UTTER_TOO_SHORT:
                        mResultEditText.setText("录音太短");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TEXT_NOT_MATCH:
                        mResultEditText.setText("验证不通过，您所读的文本不一致");
                        delayedFinish(VOICE_ERROR);
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TOO_LOW:
                        mResultEditText.setText("音量太低");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_NO_ENOUGH_AUDIO:
                        mResultEditText.setText("音频长达不到自由说的要求");
                        break;
                    default:
                        Log.e(TAG, "result.err:" + result.err);
                        mResultEditText.setText("验证不通过");
                        delayedFinish(VOICE_FAILED);
                        break;
                }
                new Thread() {
                    public void run() {
                        identifyFailedUploadImages("D");
                    }
                }.start();
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

            switch (error.getErrorCode()) {
                case ErrorCode.MSP_ERROR_NOT_FOUND:
                    mResultEditText.setText("模型不存在，请先注册");
                    delayedFinish(3000);
                    break;

                default:
                    //  showTip("onError Code：" + error.getPlainDescription(true));
                    delayedFinish(3000);
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }
    };

    private void identifyFailedUploadImages(String type) {
      /*  PlatformComm platformComm = new PlatformComm(IdentifyActivity.this);
        platformComm.sendAbnormalMessage(type, new File(CROP_FACE_PATH));*/
    }
}
