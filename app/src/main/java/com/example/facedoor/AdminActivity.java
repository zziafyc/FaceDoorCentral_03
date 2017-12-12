package com.example.facedoor;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.example.facedoor.base.BaseAppCompatActivity;

import butterknife.Bind;

public class AdminActivity extends BaseAppCompatActivity implements OnClickListener {
    @Bind(R.id.btn_register)
    Button personManageBtn;
    @Bind(R.id.btn_manage_group)
    Button groupManageBtn;
    @Bind(R.id.btn_query)
    Button queryBtn;

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_admin;
    }

    @Override
    protected void initViewsAndEvents() {
        initEvents();
        initListener();


    }

    private void initEvents() {

    }

    private void initListener() {
        personManageBtn.setOnClickListener(this);
        groupManageBtn.setOnClickListener(this);
        queryBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        //隐藏键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive() && getCurrentFocus() != null) {
            if (getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        switch (view.getId()) {

            case R.id.btn_manage_group:
                Intent intentGroup = new Intent(this, GroupManageActivity.class);
                startActivity(intentGroup);
                break;
            case R.id.btn_register:
                Intent intentReg = new Intent(this, RegisterActivity.class);
                startActivity(intentReg);
                break;
            case R.id.btn_query:
                Intent intentQuery = new Intent(this, QueryActivity.class);
                startActivity(intentQuery);
                break;
        }
    }
}
