package com.xingshulin.singularity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println(1234);
        System.out.println(1234);
        System.out.println(1234);
        TextView textView = new TextView(this);
        textView.setText("1234");
        System.out.println("textView = " + textView);
        setContentView(R.layout.activity_main);
    }
}
