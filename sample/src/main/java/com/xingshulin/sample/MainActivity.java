package com.xingshulin.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import com.xingshulin.singularity.hotfix.PatchManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println(123);
        Log.d(MainActivity.class.getSimpleName(), "aaaaaaaaaaaaa");
        setContentView(R.layout.activity_main);
    }
}
