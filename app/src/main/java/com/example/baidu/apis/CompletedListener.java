package com.example.baidu.apis;

import com.example.baidu.base.ErrorResult;

/**
 * Created by fyc on 2017/12/8.
 */

public interface CompletedListener {

    void onCompleted(String result);

    void onError(ErrorResult errorResult);
}
