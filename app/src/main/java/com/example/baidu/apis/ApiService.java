package com.example.baidu.apis;

import com.example.facedoor.model.User;

/**
 * Created by fyc on 2017/11/16.
 */

public interface ApiService {
    //得到token值
    String getToken();

    //注册人脸到人脸库
    void faceRegister(byte[] imgData, User user, CompletedListener mListener);

    //删除所有组中人脸
    void deleteFace(String userId, CompletedListener mListener);

    //删除某一个组中的人脸
    void deleteFace(String userId, String groupId, CompletedListener mListener);

    //人脸验证
    boolean verify(byte[] imgData, String groupId, CompletedListener mListener);

}
