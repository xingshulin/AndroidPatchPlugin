package com.xingshulin.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.xingshulin.singularity.hotfix.PatchManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PatchManager.install(getApplicationContext());
        setContentView(R.layout.activity_main);
    }
}
