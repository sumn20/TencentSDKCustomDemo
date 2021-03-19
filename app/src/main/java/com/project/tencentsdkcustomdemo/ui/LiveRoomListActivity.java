package com.project.tencentsdkcustomdemo.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.project.tencentsdkcustomdemo.R;
import com.project.tencentsdkcustomdemo.constants.Constant;
import com.project.tencentsdkcustomdemo.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class LiveRoomListActivity extends BaseActivity implements LiveRoomManager.RoomListListener {
    protected static final int REQ_PERMISSION_CODE = 0x1000;
    // 权限个数计数，获取Android系统权限
    protected int mGrantedCount = 0;

    private ListView mRoomListView;        //【控件】显示房间列表
    private TextView mTextTip;             //【控件】没有直播间的提示
    private LiveRoomManager mLiveRoomManager;     // 房间管理
    private LiveRoomListAdapter mLiveRoomListAdapter; // 房间列表填充器
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_room_list);
        mLiveRoomManager = new LiveRoomManager();
        mLiveRoomManager.setRoomListListener(this);
        initView();
        checkPermission();
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        bindToolbarWithNoBack(toolbar);
        mRoomListView = findViewById(R.id.lv_room_list);
        mTextTip = findViewById(R.id.room_tip_null_list_textview);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            mLiveRoomManager.queryLiveRoomList();
        });
        mRoomListView.setOnItemLongClickListener((parent, view, position, id) -> {
            swipeRefreshLayout.setRefreshing(true);
            String roomId = (String) mLiveRoomListAdapter.getItem(position);
            mLiveRoomManager.destoryLiveRoom(roomId);
            return true;
        });

        findViewById(R.id.bt_enter_live).setOnClickListener(view -> {
            if (checkPermission()) {
                mLiveRoomManager.createLiveRoom();
            }
        });
        findViewById(R.id.bt_enter_liveplayer).setOnClickListener(view -> {
            if (checkPermission()) {
                CustomLivePlayerActivity.openActivity(LiveRoomListActivity.this);
            }
        });
        findViewById(R.id.ic_setting).setOnClickListener(view -> {
            SettingActivity.openActivity(this);
        });
        mLiveRoomManager.queryLiveRoomList();
    }

    @Override
    public void onCreateRoomSuccess(String roomId) {
        CustomAnchorActivity.openActivity(LiveRoomListActivity.this, roomId);
    }

    @Override
    public void onQueryRoomListSuccess(List<String> list) {
        runOnUiThread(() -> {
            swipeRefreshLayout.setRefreshing(false);
            if (list.size() == 0) {
                mTextTip.setVisibility(View.VISIBLE);
            } else {
                mTextTip.setVisibility(View.GONE);
            }
            mLiveRoomListAdapter = new LiveRoomListAdapter(LiveRoomListActivity.this, list);
            mRoomListView.setAdapter(mLiveRoomListAdapter);
            mRoomListView.setOnItemClickListener((adapterView, view, position, l) -> {
                String roomId = list.get(position);
                int render = SPUtils.getInstance(this).getInt(Constant.RENDER, Constant.RENDER_SDK);
                if (render == Constant.RENDER_SDK) {
                    AudienceLiveActivity.openActivity(this, roomId);
                } else {
                    CustomAudienceActivity.openActivity(this, roomId);
                }
            });
            mRoomListView.setOnItemLongClickListener((parent, view, position, id) -> {
                swipeRefreshLayout.setRefreshing(true);
                String roomId = (String) mLiveRoomListAdapter.getItem(position);
                mLiveRoomManager.destoryLiveRoom(roomId);
                return true;
            });
        });
    }

    @Override
    public void onDestroyRoomSuccess() {
        mLiveRoomManager.queryLiveRoomList();
    }

    @Override
    public void onError(String errorInfo) {
        mTextTip.setVisibility(View.VISIBLE);
        runOnUiThread(() -> Toast.makeText(LiveRoomListActivity.this, errorInfo, Toast.LENGTH_LONG).show());
    }
    //////////////////////////////////    动态权限申请   ////////////////////////////////////////

    protected boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(this,
                        permissions.toArray(new String[0]),
                        REQ_PERMISSION_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION_CODE:
                for (int ret : grantResults) {
                    if (PackageManager.PERMISSION_GRANTED == ret) {
                        mGrantedCount++;
                    }
                }
                if (mGrantedCount == permissions.length) {
                    mLiveRoomManager.createLiveRoom();
                } else {
                    Toast.makeText(this, "用户没有允许需要的权限，加入通话失败", Toast.LENGTH_SHORT).show();
                }
                mGrantedCount = 0;
                break;
            default:
                break;
        }
    }
}
