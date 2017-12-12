package com.example.facedoor;

import android.content.Intent;
import android.text.Selection;
import android.text.Spannable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.example.facedoor.base.BaseAppCompatActivity;
import com.example.facedoor.util.StringUtils;
import com.example.facedoor.util.ToastShow;

import butterknife.Bind;

public class MainActivity extends BaseAppCompatActivity implements OnClickListener {
    @Bind(R.id.admin)
    EditText adminEdt;
    @Bind(R.id.password)
    EditText passwordEdt;
    @Bind(R.id.btn_start)
    Button startBtn;
    @Bind(R.id.btn_admin)
    Button manageBtn;
    private static final String ACCOUNT = "Admin";
    private static final String PASSWORD = "a";



    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_main;
    }

    @Override
    protected void initViewsAndEvents() {
        initViews();
        initListener();
    }

    private void initViews() {
        if (adminEdt.getText().toString().trim().length() > 0) {
            CharSequence text = adminEdt.getText();
            if (text instanceof Spannable) {
                Spannable spanText = (Spannable) text;
                Selection.setSelection(spanText, text.length());
            }
        }
    }

    private void initListener() {
        startBtn.setOnClickListener(this);
        manageBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_admin:
                if (StringUtils.isEmpty(adminEdt.getText().toString())) {
                    ToastShow.showTip(MainActivity.this, "账号不能为空");
                    return;
                } else {
                    if (!adminEdt.getText().toString().equals(ACCOUNT)) {
                        ToastShow.showTip(MainActivity.this, "账号错误");
                        return;
                    }
                }
                if (StringUtils.isEmpty(passwordEdt.getText().toString())) {
                    ToastShow.showTip(MainActivity.this, "密码不能为空");
                    return;
                } else {
                    if (!passwordEdt.getText().toString().equals(PASSWORD)) {
                        ToastShow.showTip(MainActivity.this, "密码错误");
                        return;
                    } else {
                        passwordEdt.setText("");
                        Intent intent = new Intent(this, AdminActivity.class);
                        startActivity(intent);
                    }
                }

                break;
            case R.id.btn_start:
                passwordEdt.setText("");
                Intent intent = new Intent(this, FaceIndexActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
