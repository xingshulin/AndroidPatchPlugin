package com.example.samplewithmultidex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.example.samplelibrary.LibraryActivity;

import static com.example.samplewithmultidex.MessengerService.MESSAGE_TYPE_WELCOME;

public class MainActivity extends AppCompatActivity {
    private Messenger serviceMessenger;
    private boolean isBound;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceMessenger = new Messenger(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceMessenger = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.welcome)).setText("Default Text from app");

        findViewById(R.id.library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LibraryActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.messenger).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isBound) {
                    Toast.makeText(MainActivity.this, "Service is not bound", Toast.LENGTH_SHORT).show();
                    return;
                }
                Message message = Message.obtain();
                message.what = MESSAGE_TYPE_WELCOME;
                try {
                    serviceMessenger.send(message);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent service = new Intent(getApplicationContext(), MessengerService.class);
        bindService(service, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }
}
