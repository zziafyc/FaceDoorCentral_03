package com.example.facedoor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.baidu.apis.ApiClient;
import com.example.baidu.apis.CompletedListener;
import com.example.baidu.base.ErrorResult;
import com.example.facedoor.base.BaseAppCompatActivity;
import com.example.facedoor.db.DBUtil;
import com.example.facedoor.model.Group;
import com.example.facedoor.model.User;
import com.example.facedoor.util.ProgressShow;
import com.example.facedoor.util.ToastShow;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.IdentityListener;
import com.iflytek.cloud.IdentityResult;
import com.iflytek.cloud.IdentityVerifier;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeakerVerifier;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RegisterActivity extends BaseAppCompatActivity implements OnClickListener {

    private final static String TAG = RegisterActivity.class.getSimpleName();
    private static final int REQUEST_GROUP_CHOOSE = 88;
    // 选择图片后返回
    public static final int REQUEST_PICK_PICTURE = 1;
    // 拍照后返回
    private final static int REQUEST_CAMERA_IMAGE = 2;
    // 裁剪图片成功后返回
    public static final int REQUEST_INTENT_CROP = 3;
    // 模型操作类型
    private int mModelCmd;
    // 删除模型
    private static final int MODEL_DEL = 1;
    private static final int PWD_TYPE_TEXT = 1;
    // 密码类型
    // 默认为文字密码
    private int mPwdType = PWD_TYPE_TEXT;
    // 文本密码
    private String mTextPwd = "芝麻开门";

    private Toast mToast;
    private ProgressDialog mProDialog;
    private LinearLayout mGroups;
    private View mLayout;

    private File mPictureFile;
    private byte[] mImageData;
    private Bitmap mImageBitmap = null;

    private IdentityVerifier mIdVerifier;
    private String mAuthId;
    private String mGroupId;
    private HashMap<String, String> mName2ID = new HashMap<String, String>();
    private ArrayList<String> mGroupJoined = new ArrayList<String>();
    private volatile boolean mIsStaffExist;

    private final static int JOIN_GROUP = 1001;
    private final static int QUIT_GROUP = 1002;
    private String registerMessage = "";
    private String deleteMessage = "";
    private List<Group> choosedGroups = new ArrayList<>();
    private TextView chooseTv;
    //执行声纹识别的模型删除
    private final static int DELETE = 1000;
    private Handler deleteHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case DELETE:
                    //执行声纹删除模型
                    performModelDelete("del");
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_register;
    }

    @Override
    protected void initViewsAndEvents() {
        Button btnReg = (Button) findViewById(R.id.online_reg);
        Button btnDelete = (Button) findViewById(R.id.online_delete);
        btnReg.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        findViewById(R.id.online_pick).setOnClickListener(this);
        findViewById(R.id.online_camera).setOnClickListener(this);
        chooseTv = (TextView) findViewById(R.id.groupChoose);
        chooseTv.setOnClickListener(this);

        SharedPreferences config = getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        String dbIP = config.getString(MyApp.DBIP_KEY, "");
        if (TextUtils.isEmpty(dbIP)) {
            btnReg.setEnabled(false);
            btnDelete.setEnabled(false);
        }

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mGroups = (LinearLayout) findViewById(R.id.online_groups);
        mLayout = findViewById(R.id.register_layout);

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
        mVerifier = SpeakerVerifier.createVerifier(RegisterActivity.this, null);
        mIdVerifier = IdentityVerifier.createVerifier(this, new InitListener() {

            @Override
            public void onInit(int errorCode) {
                // TODO Auto-generated method stub
                if (errorCode == ErrorCode.SUCCESS) {
                    // ToastShow.showTip(mToast, "引擎初始化成功");
                } else {
                    ToastShow.showTip(mToast, "引擎初始化失败。错误码：" + errorCode);
                }
            }
        });
        mToast = Toast.makeText(RegisterActivity.this, "", Toast.LENGTH_SHORT);

        // do not put it in onResume(), cropPicture() cause quickly switch from onResume() to onPause()
        // at that time, mName2ID and mGroups are still empty in onPause()
        Observable.create(new OnSubscribe<ArrayList<String>>() {
            @Override
            public void call(Subscriber<? super ArrayList<String>> arg0) {
                DBUtil dbUtil = new DBUtil(RegisterActivity.this);
                ArrayList<String> id = new ArrayList<String>();
                ArrayList<String> name = new ArrayList<String>();
                dbUtil.queryGroups(id, name);
                int length = id.size();
                for (int i = 0; i < length; i++) {
                    mName2ID.put(name.get(i), id.get(i));
                }
                arg0.onNext(name);
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<String>>() {
                    @Override
                    public void call(ArrayList<String> name) {
                      /*  int length = name.size();
                        for (int i = 0; i < length; i++) {
                            CheckBox checkBox = new CheckBox(RegisterActivity.this);
                            checkBox.setBackgroundColor(ContextCompat.getColor(RegisterActivity.this, R.color.white));
                            checkBox.setTextColor(ContextCompat.getColor(RegisterActivity.this, R.color.black));
                            checkBox.setText(name.get(i));
                            mGroups.addView(checkBox);
                        }*/
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mIdVerifier) {
            mIdVerifier.destroy();
            mIdVerifier = null;
        }
        mName2ID.clear();
        mGroups.removeAllViews();
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.groupChoose:
                Intent intentChoose = new Intent(this, ChooseGroupActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("choosedGroups", (Serializable) choosedGroups);
                intentChoose.putExtras(bundle);
                startActivityForResult(intentChoose, REQUEST_GROUP_CHOOSE);
                break;
            case R.id.online_pick:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(intent, REQUEST_PICK_PICTURE);
                break;
            case R.id.online_camera:
                // 设置相机拍照后照片保存路径
                mPictureFile = new File(Environment.getExternalStorageDirectory(),
                        "picture" + System.currentTimeMillis() / 1000 + ".jpg");
                // 启动拍照,并保存到临时文件
                Intent mIntent = new Intent();
                mIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                mIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPictureFile));
                mIntent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
                startActivityForResult(mIntent, REQUEST_CAMERA_IMAGE);
                break;
            case R.id.online_reg:
                faceRegister();
                break;
            case R.id.online_delete:
                deleteUser();
                break;
        }
    }

    private void faceRegister() {
        // 人脸注册
        final String staffID = ((EditText) findViewById(R.id.online_number)).getText().toString();
        if (TextUtils.isEmpty(staffID)) {
            ToastShow.showTip(mToast, "工号不能为空");
            return;
        }

        final String staffName = ((EditText) findViewById(R.id.online_name)).getText().toString();
        if (TextUtils.isEmpty(staffName)) {
            ToastShow.showTip(mToast, "用户名不能为空");
            return;
        }
       /* for (int i = 0; i < mGroups.getChildCount(); i++) {
            CheckBox child = (CheckBox) mGroups.getChildAt(i);
            if (child != null && child.isChecked()) {
                String groupName = child.getText().toString();
                mGroupJoined.add(mName2ID.get(groupName));
            }
        }*/
        if (mGroupJoined.size() == 0) {
            ToastShow.showTip(mToast, "请勾选组");
            return;
        }
        if (mImageData == null) {
            ToastShow.showTip(mToast, "请选择图片后再注册");
            return;
        }

        mIsStaffExist = false;
        Runnable queryStaffID = new Runnable() {
            public void run() {
                DBUtil dbUtil = new DBUtil(RegisterActivity.this);
                try {
                    mIsStaffExist = dbUtil.isStaffExist(staffID);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        Thread queryThread = new Thread(queryStaffID);
        queryThread.start();
        try {
            queryThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mIsStaffExist) {
            ToastShow.showTip(mToast, "用户已存在");
            return;
        }
        ProgressShow.show(mProDialog, "注册中...");
        Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> arg0) {
                DBUtil dbUtil = new DBUtil(RegisterActivity.this);
                dbUtil.insertUser(staffID, staffName);
                int userID = dbUtil.queryUserID(staffID);
                if (userID == -1) {
                    ToastShow.showTip(mToast, "工号长度超出限制！");
                    return;
                }
                arg0.onNext(userID);
            }
        }).map(new Func1<Integer, String>() {
            @Override
            public String call(final Integer userID) {
                //加入本地数据库中
                DBUtil dbUtil = new DBUtil(RegisterActivity.this);
                dbUtil.insertUserGroup(userID, mGroupJoined);
                //注册到百度云端
                for (int i = 0; i < mGroupJoined.size(); i++) {
                    //指定user的组名
                    String groupId = mGroupJoined.get(i);
                    ApiClient.getApiService(getApplication()).faceRegister(mImageData, new User(String.valueOf(userID), staffName, groupId), new CompletedListener() {
                        @Override
                        public void onCompleted(String result) {
                            Log.d(TAG, "注册成功了一个。 ");

                        }

                        @Override
                        public void onError(ErrorResult errorResult) {
                            ProgressShow.stop(mProDialog);
                            registerMessage = errorResult.getErrorCode() + ":" + errorResult.getErrorMessage();
                            ToastShow.showTip(RegisterActivity.this, registerMessage);
                            return;
                        }
                    });
                }
                if (TextUtils.isEmpty(registerMessage)) {
                    return "注册成功，已加入所有指定组";
                } else {
                    return "对不起，注册失败!" + registerMessage;
                }
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        ProgressShow.stop(mProDialog);
                        ToastShow.showTip(RegisterActivity.this, s);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        String fileSrc = null;
        if (requestCode == REQUEST_PICK_PICTURE) {
            if ("file".equals(data.getData().getScheme())) {
                // 有些低版本机型返回的Uri模式为file
                fileSrc = data.getData().getPath();
            } else {
                // Uri模型为content
                String[] proj = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(data.getData(), proj,
                        null, null, null);
                cursor.moveToFirst();
                int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                fileSrc = cursor.getString(idx);
                cursor.close();
            }
            // 跳转到图片裁剪页面
            cropPicture(this, Uri.fromFile(new File(fileSrc)));
        } else if (requestCode == REQUEST_CAMERA_IMAGE) {
            if (null == mPictureFile) {
                ToastShow.showTip(mToast, "拍照失败，请重试");
                return;
            }

            fileSrc = mPictureFile.getAbsolutePath();
            updateGallery(fileSrc);
            // 跳转到图片裁剪页面,需要先进行图片镜像翻转
            Bitmap bitmap = BitmapFactory.decodeFile(fileSrc);
            bitmap = flipBitmap(bitmap);
            File file = new File(getImagePath2());//将要保存图片的路径
            try {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                bos.flush();
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            cropPicture(this, Uri.fromFile(new File(getImagePath2())));

        } else if (requestCode == REQUEST_INTENT_CROP) {
            // 获取返回数据
            Bitmap bmp = data.getParcelableExtra("data");

            // 获取裁剪后图片保存路径
            fileSrc = getImagePath();

            // 若返回数据不为null，保存至本地，防止裁剪时未能正常保存
            if (null != bmp) {
                saveBitmapToFile(bmp);
            }

            // 获取图片的宽和高
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            mImageBitmap = BitmapFactory.decodeFile(fileSrc, options);

            // 压缩图片
            options.inSampleSize = Math.max(1, (int) Math.ceil(Math.max(
                    (double) options.outWidth / 1024f,
                    (double) options.outHeight / 1024f)));
            options.inJustDecodeBounds = false;
            mImageBitmap = BitmapFactory.decodeFile(fileSrc, options);

            // 若mImageBitmap为空则图片信息不能正常获取
            if (null == mImageBitmap) {
                ToastShow.showTip(this, "图片信息无法正常获取！");
                return;
            }

            // 部分手机会对图片做旋转，这里检测旋转角度
            int degree = readPictureDegree(fileSrc);
            if (degree != 0) {
                // 把图片旋转为正的方向
                mImageBitmap = rotateImage(degree, mImageBitmap);

            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //可根据流量及网络状况对图片进行压缩
            mImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            mImageData = baos.toByteArray();

            ((ImageView) findViewById(R.id.online_img)).setImageBitmap(mImageBitmap);

        } else if (requestCode == REQUEST_GROUP_CHOOSE) {
            if (data != null) {
                choosedGroups = (List<Group>) data.getSerializableExtra("choosedGroups");
                if (choosedGroups != null) {
                    chooseTv.setText("已选择" + choosedGroups.size() + "个组,点击重选？");
                    if (choosedGroups.size() > 0) {
                        for (Group group : choosedGroups) {
                            mGroupJoined.add(group.getId());
                        }
                    }
                }
            }
        }
    }

    private void identify() {
        if (mImageData == null) {
            ToastShow.showTip(mToast, "请选择图片后再验证");
            return;
        }
        ArrayList<String> groupIdList = MyApp.getDBManage(this).getGroupId();
        mGroupId = groupIdList.get(0);
        if (mGroupId == null) {
            ToastShow.showTip(mToast, "请先建立组");
            return;
        }
        ProgressShow.show(mProDialog, "鉴别中。。。");

        // 清空参数
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置业务场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ifr");
        // 设置业务类型
        mIdVerifier.setParameter(SpeechConstant.MFV_SST, "identify");
        // 设置监听器，开始会话
        mIdVerifier.startWorking(mSearchListener);
        // 子业务执行参数，若无可以传空字符传
        StringBuffer params = new StringBuffer();
        params.append(",group_id=" + mGroupId + ",topc=3");
        // 向子业务写入数据，人脸数据可以一次写入
        mIdVerifier.writeData("ifr", params.toString(), mImageData, 0, mImageData.length);
        // 写入完毕
        mIdVerifier.stopWrite("ifr");
    }

    private void verify() {
        // 人脸验证
        String authName = ((EditText) findViewById(R.id.online_name)).getText().toString();
        if (TextUtils.isEmpty(authName)) {
            ToastShow.showTip(mToast, "用户名不能为空");
            return;
        }
        String authNameUTF8 = null;
        try {
            byte[] nameBytes = authName.getBytes("UTF-8");
            authNameUTF8 = new String(nameBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (mImageData == null) {
            ToastShow.showTip(mToast, "请选择图片后再验证");
            return;
        }
        int userId = MyApp.getDBManage(this).queryUserId(authNameUTF8);
        if (userId == 0) {
            ToastShow.showTip(mToast, "用户不存在");
            return;
        }
        mAuthId = userIdToAuthId(userId);
        ProgressShow.show(mProDialog, "验证中");
        // 设置人脸验证参数
        mIdVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mIdVerifier.setParameter(SpeechConstant.MFV_SCENES, "ifr");
        // 设置会话类型
        mIdVerifier.setParameter(SpeechConstant.MFV_SST, "verify");
        // 设置验证模式，单一验证模式：sin
        mIdVerifier.setParameter(SpeechConstant.MFV_VCM, "sin");
        // 用户id
        mIdVerifier.setParameter(SpeechConstant.AUTH_ID, mAuthId);
        // 设置监听器，开始会话
        mIdVerifier.startWorking(mVerifyListener);
        // 子业务执行参数，若无可以传空字符传
        StringBuffer params = new StringBuffer();
        // 向子业务写入数据，人脸数据可以一次写入
        mIdVerifier.writeData("ifr", params.toString(), mImageData, 0, mImageData.length);
        // 停止写入
        mIdVerifier.stopWrite("ifr");
    }

    private void deleteUser() {
        final String staffID = ((EditText) findViewById(R.id.online_number)).getText().toString();
        if (TextUtils.isEmpty(staffID)) {
            ToastShow.showTip(mToast, "工号不能为空");
            return;
        }
        ProgressShow.show(mProDialog, "删除中。。。");
        Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> arg0) {
                DBUtil dbUtil = new DBUtil(RegisterActivity.this);
                int userID = dbUtil.queryUserID(staffID);
                if (userID == -1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastShow.showTip(mToast, "用户不存在");
                        }
                    });
                    ProgressShow.stop(mProDialog);
                    return;
                }
                arg0.onNext(userID);
            }
        }).map(new Func1<Integer, String>() {
            @Override
            public String call(Integer userID) {
                //删除user表中的数据、同时删除userGroup表中的数据
                DBUtil dbUtil = new DBUtil(RegisterActivity.this);
                dbUtil.deleteUser(userID);
                dbUtil.deleteUserGroup(userID);
                //删除云端user
                ApiClient.getApiService(getApplication()).deleteFace(String.valueOf(userID), new CompletedListener() {
                    @Override
                    public void onCompleted(String result) {
                        Log.d(TAG, "删除成功");

                    }

                    @Override
                    public void onError(ErrorResult errorResult) {
                        ProgressShow.stop(mProDialog);
                        deleteMessage = errorResult.getErrorCode() + ":" + errorResult.getErrorMessage();
                        ToastShow.showTip(RegisterActivity.this, deleteMessage);
                        return;
                    }
                });
                if (TextUtils.isEmpty(deleteMessage)) {
                    return "删除成功";
                } else {
                    return "对不起，删除失败!" + deleteMessage;
                }
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String result) {
                        ProgressShow.stop(mProDialog);
                        ToastShow.showTip(RegisterActivity.this, result);
                    }
                });
    }

    /**
     * 人脸鉴别监听器
     */
    private IdentityListener mSearchListener = new IdentityListener() {

        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d(TAG, result.getResultString());

            if (mProDialog != null) {
                ProgressShow.stop(mProDialog);
            }
            identifyResult(result);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

        @Override
        public void onError(SpeechError error) {
            if (mProDialog != null) {
                ProgressShow.stop(mProDialog);
            }
            ToastShow.showTip(mToast, error.getPlainDescription(true));
        }

    };

    /**
     * 人脸验证监听器
     */
    private IdentityListener mVerifyListener = new IdentityListener() {

        @Override
        public void onResult(IdentityResult result, boolean islast) {
            Log.d(TAG, result.getResultString());

            if (null != mProDialog) {
                ProgressShow.stop(mProDialog);
            }

            try {
                JSONObject object = new JSONObject(result.getResultString());
                String decision = object.getString("decision");

                if ("accepted".equalsIgnoreCase(decision)) {
                    ToastShow.showTip(mToast, "通过验证");
                } else {
                    ToastShow.showTip(mToast, "验证失败");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

        @Override
        public void onError(SpeechError error) {
            if (null != mProDialog) {
                ProgressShow.stop(mProDialog);
            }
            ToastShow.showTip(mToast, error.getPlainDescription(true));
        }

    };

    private void performModelDelete(String operation) {

        mVerifier.setParameter(SpeechConstant.PARAMS, null);
        mVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);

        if (mPwdType == PWD_TYPE_TEXT) {
            mVerifier.setParameter(SpeechConstant.ISV_PWD, mTextPwd);
        }
        mVerifier.sendRequest(operation, mAuthId, listener);
    }

    private SpeechListener listener = new SpeechListener() {

        @Override
        public void onEvent(int arg0, Bundle arg1) {

        }

        @Override
        public void onCompleted(SpeechError error) {
            if (null != error && ErrorCode.SUCCESS != error.getErrorCode()) {
                ToastShow.showTip(mToast, "操作失败：" + error.getPlainDescription(true));
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            String result = new String(buffer);
            try {
                JSONObject object = new JSONObject(result);
                String cmd = object.getString("cmd");
                int ret = object.getInt("ret");

                if ("del".equals(cmd)) {
                    if (ret == ErrorCode.SUCCESS) {
                        ToastShow.showTip(mToast, "声纹删除成功");
                    } else if (ret == ErrorCode.MSP_ERROR_FAIL) {
                        ToastShow.showTip(mToast, "声纹删除失败，模型不存在");
                    }
                    //fyc声纹删除之后需要finish界面
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 人脸注册监听器
     */
    private SpeakerVerifier mVerifier;

    /***
     * 裁剪图片
     * @param activity Activity
     * @param uri 图片的Uri
     */
    public void cropPicture(Activity activity, Uri uri) {
        Intent innerIntent = new Intent("com.android.camera.action.CROP");
        innerIntent.setDataAndType(uri, "image/*");
        innerIntent.putExtra("crop", "true");// 才能出剪辑的小方框，不然没有剪辑功能，只能选取图片
        innerIntent.putExtra("aspectX", 1); // 放大缩小比例的X
        innerIntent.putExtra("aspectY", 1);// 放大缩小比例的X   这里的比例为：   1:1
        innerIntent.putExtra("outputX", 320);  //这个是限制输出图片大小
        innerIntent.putExtra("outputY", 320);
        innerIntent.putExtra("return-data", false);
        // 切图大小不足输出，无黑框
        innerIntent.putExtra("scale", true);
        innerIntent.putExtra("scaleUpIfNeeded", true);
        innerIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(getImagePath())));
        innerIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        activity.startActivityForResult(innerIntent, REQUEST_INTENT_CROP);
    }

    /**
     * 设置保存图片路径
     *
     * @return
     */
    private String getImagePath() {
        String path;
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FaceDoor/";
        File folder = new File(path);
        if (folder != null && !folder.exists()) {
            folder.mkdirs();
        }
        path += "crop.jpg";
        return path;
    }

    private String getImagePath2() {
        String path;
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FaceDoor/";
        File folder = new File(path);
        if (folder != null && !folder.exists()) {
            folder.mkdirs();
        }
        path += "flip.jpg";
        return path;
    }

    private void updateGallery(String filename) {
        MediaScannerConnection.scanFile(this, new String[]{filename}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

    /**
     * 保存Bitmap至本地
     *
     * @param
     */
    private void saveBitmapToFile(Bitmap bmp) {
        String file_path = getImagePath();
        File file = new File(file_path);
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param path 图片绝对路径
     * @return degree 旋转的角度
     */
    public static int readPictureDegree(String path) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 旋转图片
     *
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    public static Bitmap rotateImage(int angle, Bitmap bitmap) {
        // 图片旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 得到旋转后的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return resizedBitmap;
    }

    private String userIdToAuthId(int userId) {
        return "a" + userId;
    }

    private int AuthIdToUserId(String authId) {
        return Integer.parseInt(authId.substring(1));
    }

    private void identifyResult(IdentityResult result) {
        String resultStr = result.getResultString();
        try {
            JSONObject resultJson = new JSONObject(resultStr);
            if (ErrorCode.SUCCESS == resultJson.getInt("ret")) {
                JSONObject candidateOne = resultJson.getJSONObject("ifv_result").getJSONArray("candidates").getJSONObject(0);
                String authId = candidateOne.getString("user");
                int userId = AuthIdToUserId(authId);
                String userName = MyApp.getDBManage(this).queryUserName(userId);
                if (userName == null) {
                    ToastShow.showTip(mToast, "数据库无此用户");
                } else {
                    ToastShow.showTip(mToast, "你好" + userName);
                }
            } else {
                ToastShow.showTip(mToast, "对不起，鉴别失败");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Bitmap flipBitmap(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1);
        Bitmap flip = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        return flip;
    }


}
