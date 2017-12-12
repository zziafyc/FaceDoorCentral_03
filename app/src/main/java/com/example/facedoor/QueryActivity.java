package com.example.facedoor;

import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.facedoor.base.BaseAppCompatActivity;

import butterknife.Bind;

public class QueryActivity extends BaseAppCompatActivity implements OnClickListener {
    @Bind(R.id.btn_query_groups)
    Button queryGroupsBtn;
    @Bind(R.id.btn_query_local_users)
    Button queryUsersBtn;

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_query;
    }

    @Override
    protected void initViewsAndEvents() {
        queryGroupsBtn.setOnClickListener(this);
        queryUsersBtn.setOnClickListener(this);

    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.btn_query_groups:
                Intent intent1 = new Intent(this, AllGroupsActivity.class);
                startActivity(intent1);
                break;
            case R.id.btn_query_local_users:
                Intent intent2 = new Intent(this, AllUsersActivity.class);
                startActivity(intent2);
                break;

            default:
                break;
        }
    }

}
