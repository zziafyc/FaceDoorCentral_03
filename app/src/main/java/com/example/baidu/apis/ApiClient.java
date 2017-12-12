package com.example.baidu.apis;

import android.content.Context;

/**
 * Created by fyc on 2017/12/8.
 */

public class ApiClient {
    private static ApiService mApiService;

    public static ApiService getApiService(Context mContext) {
        if (mApiService == null) {
            synchronized (ApiService.class) {
                if (mApiService == null) {
                    mApiService = new ApiServiceImpl(mContext);
                }
            }
        }
        return mApiService;
    }
}
