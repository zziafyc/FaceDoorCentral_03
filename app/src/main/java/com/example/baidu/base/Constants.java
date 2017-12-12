package com.example.baidu.base;

/**
 * Created by fyc on 2017/12/8.
 */

public class Constants {
    public static final String CLIENT_ID = "XmrGwOD2gcubdlNuFEnisNRB";
    public static final String CLIENT_SECRET = "6MnMO0zyGLHfuDiIAaXGyVXA0SohwNqi";

    public static class Url {
        //获取TOKEN
        public static final String FACE_GET_TOKEN = "https://aip.baidubce.com/oauth/2.0/token?";
        //人脸注册
        public static final String FACE_REGISTER = "https://aip.baidubce.com/rest/2.0/face/v2/faceset/user/add";
        //人脸删除（删除所有组中）
        public static final String FACE_DELETE = "https://aip.baidubce.com/rest/2.0/face/v2/faceset/user/delete";
        //人脸删除(在某一个组内的)
        public static final String FACE_DELETE_BY_GROUP = "https://aip.baidubce.com/rest/2.0/face/v2/faceset/group/deleteuser";
        //人脸检测
        public static final String FACE_DETECT = "https://aip.baidubce.com/rest/2.0/face/v2/detect";
        //人脸验证
        public static final String FACE_IDENTIFY = "https://aip.baidubce.com/rest/2.0/face/v2/identify";
    }
    public static class errorCode{

    }

}
