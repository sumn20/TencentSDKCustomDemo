package com.project.tencentsdkcustomdemo.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

public abstract class BaseActivity extends AppCompatActivity {


    protected void bindToolbarWithBack(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> this.finish());
    }

    protected void bindToolbarWithNoBack(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

}
