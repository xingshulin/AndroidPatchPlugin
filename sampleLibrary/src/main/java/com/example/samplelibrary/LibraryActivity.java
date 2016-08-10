package com.example.samplelibrary;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class LibraryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        view.setText("Default Text from library");
        setContentView(view);
    }
}
