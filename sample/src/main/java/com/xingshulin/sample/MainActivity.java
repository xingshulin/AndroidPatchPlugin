package com.xingshulin.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import com.xingshulin.singularity.hotfix.PatchDownloaderKt;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PatchDownloaderKt.download("hello world");
        setContentView(R.layout.activity_main);
    }
}
