package com.example.baidu.apis;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.baidu.base.Constants;
import com.example.baidu.base.ErrorResult;
import com.example.baidu.utils.Base64Util;
import com.example.baidu.utils.HttpUtil;
import com.example.facedoor.MyApp;
import com.example.facedoor.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by fyc on 2017/12/8.
 */

public class ApiServiceImpl implements ApiService {
    private Context mContext;

    public ApiServiceImpl(Context context) {
        mContext = context;
    }

    @Override
    public String getToken() {
        SharedPreferences config = mContext.getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
        if (!TextUtils.isEmpty(config.getString(MyApp.BAIDU_TOKEN, ""))) {
            return config.getString(MyApp.BAIDU_TOKEN, "");
        }

        // 获取token地址
        String authHost = Constants.Url.FACE_GET_TOKEN;
        // 官网获取的 API Key 更新为你注册的
        String clientId = Constants.CLIENT_ID;
        // 官网获取的 Secret Key 更新为你注册的
        String clientSecret = Constants.CLIENT_SECRET;
        String getAccessTokenUrl = authHost
                // 1. grant_type为固定参数
                + "grant_type=client_credentials"
                // 2. 官网获取的 API Key
                + "&client_id=" + clientId
                // 3. 官网获取的 Secret Key
                + "&client_secret=" + clientSecret;
        try {
            URL realUrl = new URL(getAccessTokenUrl);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.out.println(key + "--->" + map.get(key));
            }
            // 定义 BufferedReader输入流来读取URL的响应
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = "";
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            /**
             * 返回结果示例
             */
            System.out.println("result:" + result);
            JSONObject jsonObject = new JSONObject(result);
            String access_token = jsonObject.getString("access_token");
            //将百度令牌保存到偏好设置
            SharedPreferences.Editor editor = config.edit();
            editor.putString(MyApp.BAIDU_TOKEN, access_token);
            editor.commit();
            return access_token;
        } catch (Exception e) {
            System.out.printf("获取token失败！");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void faceRegister(final byte[] imgData, final User user, final CompletedListener mListener) {
        String imgStr = Base64Util.encode(imgData);
        String uid = user.getUserID();
        String userInfo = "用户工号：" + user.getUserID() + " 用户姓名：" + user.getName();
        String groupId = user.getGroupId();
        try {
            final String params = "uid=" + uid + "&user_info=" + userInfo + "&group_id=" + groupId + "&"
                    + URLEncoder.encode("images", "UTF-8") + "=" + URLEncoder.encode(imgStr, "UTF-8");
            try {
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        final String accessToken = getToken();
                        if (accessToken != null) {
                            try {
                                String result = HttpUtil.post(Constants.Url.FACE_REGISTER, accessToken, params);
                                JSONObject jsonObject = new JSONObject(result);
                                if (result.contains("error_code") &&jsonObject.getInt("error_code") == 110) {
                                    //令牌失效,重新获取
                                    SharedPreferences config = mContext.getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                                    SharedPreferences.Editor editor = config.edit();
                                    editor.putString(MyApp.BAIDU_TOKEN, "");
                                    editor.commit();
                                    faceRegister(imgData, user, mListener);
                                }
                                subscriber.onNext(result);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(String result) {
                                if (result.contains("error_code")) {
                                    mListener.onError(dealWithError(result));

                                } else {
                                    mListener.onCompleted(result);
                                }

                            }


                        });
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void deleteFace(final String userId, final CompletedListener mListener) {
        final String params = "uid=" + userId;
        try {
            Observable.create(new Observable.OnSubscribe<String>() {
                @Override
                public void call(Subscriber<? super String> subscriber) {
                    final String accessToken = getToken();
                    if (accessToken != null) {
                        try {
                            String result = HttpUtil.post(Constants.Url.FACE_DELETE, accessToken, params);
                            JSONObject jsonObject = new JSONObject(result);
                            if (result.contains("error_code") &&jsonObject.getInt("error_code") == 110) {
                                //令牌失效,重新获取
                                SharedPreferences config = mContext.getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                                SharedPreferences.Editor editor = config.edit();
                                editor.putString(MyApp.BAIDU_TOKEN, "");
                                editor.commit();
                                deleteFace(userId, mListener);
                            }
                            subscriber.onNext(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<String>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(String result) {
                            if (result.contains("error_code")) {
                                mListener.onError(dealWithError(result));

                            } else {
                                mListener.onCompleted(result);
                            }

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void deleteFace(final String userId, final String groupId, final CompletedListener mListener) {
        final String params = "uid=" + userId + "&group_id=" + groupId;

        try {
            Observable.create(new Observable.OnSubscribe<String>() {
                @Override
                public void call(Subscriber<? super String> subscriber) {
                    final String accessToken = getToken();
                    if (accessToken != null) {
                        try {
                            String result = HttpUtil.post(Constants.Url.FACE_DELETE_BY_GROUP, accessToken, params);
                            JSONObject jsonObject = new JSONObject(result);
                            if (result.contains("error_code") &&jsonObject.getInt("error_code") == 110) {
                                //令牌失效,重新获取
                                SharedPreferences config = mContext.getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                                SharedPreferences.Editor editor = config.edit();
                                editor.putString(MyApp.BAIDU_TOKEN, "");
                                editor.commit();
                                deleteFace(userId, groupId, mListener);
                            }
                            subscriber.onNext(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<String>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(String result) {
                            if (result.contains("error_code")) {
                                mListener.onError(dealWithError(result));

                            } else {
                                mListener.onCompleted(result);
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean verify(final byte[] imgData, final String groupId, final CompletedListener mListener) {
        try {
            // 返回用户top数，默认为1
            String userTopNum = "1";
            // 单用户人脸匹配得分top数，默认为1
            String faceTopNum = "1";
            String imgStr = Base64Util.encode(imgData);
            final String params = "group_id=" + groupId + "&user_top_num=" + userTopNum + "&face_top_num" + faceTopNum
                    + "&images=" + URLEncoder.encode(imgStr, "UTF-8");
            try {
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        final String accessToken = getToken();
                        if (accessToken != null) {
                            try {
                                final String result = HttpUtil.post(Constants.Url.FACE_IDENTIFY, accessToken, params);
                                JSONObject jsonObject = new JSONObject(result);
                                if (result.contains("error_code") && jsonObject.getInt("error_code") == 110) {
                                    //令牌失效,重新获取
                                    SharedPreferences config = mContext.getSharedPreferences(MyApp.CONFIG, MODE_PRIVATE);
                                    SharedPreferences.Editor editor = config.edit();
                                    editor.putString(MyApp.BAIDU_TOKEN, "");
                                    editor.commit();
                                    verify(imgData, groupId, mListener);
                                }
                                subscriber.onNext(result);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<String>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(String result) {
                                if (result.contains("error_code")) {
                                    mListener.onError(dealWithError(result));

                                } else {
                                    mListener.onCompleted(result);
                                }

                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public ErrorResult dealWithError(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            int errorCode = jsonObject.getInt("error_code");
            int logId = jsonObject.getInt("log_id");
            String message = jsonObject.getString("error_msg");
            return new ErrorResult(errorCode, logId, message);


        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
