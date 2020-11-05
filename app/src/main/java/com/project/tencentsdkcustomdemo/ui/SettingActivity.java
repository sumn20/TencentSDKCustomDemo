package com.project.tencentsdkcustomdemo.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.project.tencentsdkcustomdemo.R;
import com.project.tencentsdkcustomdemo.constants.Constant;
import com.project.tencentsdkcustomdemo.utils.SPUtils;

import androidx.appcompat.widget.Toolbar;
/**
 * junker 分支新增说明
 * 夏雷是我大哥（本来我想写"夏雷我爱你"，但是他说恶心不让我写）
 */
public class SettingActivity extends BaseActivity {

    public static void openActivity(Activity activity) {
        Intent intent = new Intent(activity, SettingActivity.class);
        activity.startActivity(intent);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Toolbar mToolbar = findViewById(R.id.toolbar);
        bindToolbarWithBack(mToolbar);
        RadioGroup mRgCameraType = findViewById(R.id.rg_camera_type);
        RadioGroup mRgRender = findViewById(R.id.rg_render);
        RadioButton mRbCamera1 = findViewById(R.id.rb_camera1);
        RadioButton mRbCamera2 = findViewById(R.id.rb_camera2);
        RadioButton mRbSdk = findViewById(R.id.rb_sdk);
        RadioButton mRbCustom = findViewById(R.id.rb_custom);
        int render = SPUtils.getInstance(this).getInt(Constant.RENDER, Constant.RENDER_SDK);
        int cameraType = SPUtils.getInstance(this).getInt(Constant.CAMERA_TYPE, Constant.CAMERA_1);
        mRbSdk.setChecked(render == Constant.RENDER_SDK);
        mRbCustom.setChecked(render == Constant.RENDER_CUSTOM);
        mRbCamera1.setChecked(cameraType==Constant.CAMERA_1);
        mRbCamera2.setChecked(cameraType==Constant.CAMERA_2);
        mRgCameraType.setOnCheckedChangeListener((radioGroup, id) -> {
            switch (id) {
                case R.id.rb_camera1:
                    SPUtils.getInstance(this).put(Constant.CAMERA_TYPE, Constant.CAMERA_1);
                    break;
                case R.id.rb_camera2:
                    SPUtils.getInstance(this).put(Constant.CAMERA_TYPE, Constant.CAMERA_2);
                    break;
            }
        });
        mRgRender.setOnCheckedChangeListener((radioGroup, id) -> {
            switch (id) {
                case R.id.rb_sdk:
                    SPUtils.getInstance(this).put(Constant.RENDER, Constant.RENDER_SDK);
                    break;
                case R.id.rb_custom:
                    SPUtils.getInstance(this).put(Constant.RENDER, Constant.RENDER_CUSTOM);
                    break;
            }
        });


    }
}
