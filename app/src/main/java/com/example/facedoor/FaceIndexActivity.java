package com.example.facedoor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.facedoor.base.BaseAppCompatActivity;
import com.example.facedoor.ui.SlideUnlockView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by fyc on 2017/11/2.
 */

public class FaceIndexActivity extends BaseAppCompatActivity implements DialogInterface.OnClickListener {
    @Bind(R.id.slideUnlockView)
    SlideUnlockView slideUnlockView;
    @Bind(R.id.afi_click_img)
    ImageView clickImg;
    @Bind(R.id.img_more)
    ImageView moreImg;
    private Vibrator vibrator;
    private EditText editText;
    private static final String PASS_WORD = "123";
    PopupWindow mPopupWindow;

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_face_index;
    }

    @Override
    public void initViewsAndEvents() {

        // 获取系统振动器服务
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        // 设置滑动解锁-解锁的监听
        slideUnlockView.setOnUnLockListener(new SlideUnlockView.OnUnLockListener() {
            @Override
            public void setUnLocked(boolean unLock) {
                // 如果是true，证明解锁
                if (unLock) {
                    // 启动震动器 100ms
                    vibrator.vibrate(100);
                    // 当解锁的时候，执行逻辑操作，在这里仅仅是将图片进行展示
                    Intent intent = new Intent(FaceIndexActivity.this, VideoDetect.class);
                    startActivity(intent);

                }
            }
        });
        clickImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FaceIndexActivity.this, VideoDetect.class);
                startActivity(intent);
            }
        });
        moreImg.setVisibility(View.VISIBLE);
        moreImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPop(FaceIndexActivity.this, moreImg);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 重置一下滑动解锁的控件
        slideUnlockView.reset();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            editText = new EditText(FaceIndexActivity.this);
            new AlertDialog.Builder(FaceIndexActivity.this).setTitle("请输入密码").setIcon(R.drawable.ic_launcher)
                    .setView(editText).setPositiveButton("确定", FaceIndexActivity.this).setNegativeButton("取消", null).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (editText.getText().toString().equals(PASS_WORD)) {
            Intent intent = new Intent(FaceIndexActivity.this, MainActivity.class);
            startActivity(intent);
            mPopupWindow.dismiss();
            finish();
        } else {
            Toast.makeText(FaceIndexActivity.this, "密码错", Toast.LENGTH_SHORT).show();
        }
    }

    public void showPop(final Activity context, View parent) {
        final View contentView = LayoutInflater.from(context).inflate(R.layout.pop_exit, null, false);
        TextView textView = (TextView) contentView.findViewById(R.id.exit);
        textView.setText("进入管理员页");
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText = new EditText(FaceIndexActivity.this);
                new AlertDialog.Builder(FaceIndexActivity.this).setTitle("请输入密码").setIcon(R.drawable.ic_launcher)
                        .setView(editText).setPositiveButton("确定", FaceIndexActivity.this).setNegativeButton("取消", null).show();
            }
        });
        mPopupWindow = new PopupWindow(contentView, 150, 40, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.showAsDropDown(parent, 20, 10);

    }
}
