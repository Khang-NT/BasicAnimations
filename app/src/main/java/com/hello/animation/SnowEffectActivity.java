package com.hello.animation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Khang on 25/12/2015.
 */
public class SnowEffectActivity extends AppCompatActivity{
    private static final String TAG = "SnowEffectActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        ((SnowEffect) findViewById(R.id.snowEffect)).show();
    }
}
