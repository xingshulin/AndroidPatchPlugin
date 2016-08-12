package com.example.samplewithmultidex;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.widget.Toast;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class MessengerService extends Service {
    public static final int MESSAGE_TYPE_WELCOME = 1;

    private class IncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TYPE_WELCOME:
                    makeText(getApplicationContext(), "Hello from MessengerService!", LENGTH_SHORT).show();
                    return;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Messenger messenger = new Messenger(new IncomingMessageHandler());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        makeText(getApplicationContext(), "Binding MessengerService...", LENGTH_SHORT).show();
        return messenger.getBinder();
    }
}
