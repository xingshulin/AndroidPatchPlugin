package com.example.samplewithmultidex;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.support.annotation.Nullable;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class MessengerService extends Service {
    public static final int MESSAGE_TYPE_WELCOME = 1;
    public static final String KEY_WELCOME = "welcome";

    private class IncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TYPE_WELCOME:
                    handleWelcomeMessage(msg);
                    return;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void handleWelcomeMessage(Message msg) {
        if (msg.replyTo == null) {
            return;
        }
        Message message = Message.obtain(msg);
        try {
            Bundle data = new Bundle();
            data.putString(KEY_WELCOME, "Hello from MessengerService!");
            message.setData(data);
            msg.replyTo.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private final Messenger messenger = new Messenger(new IncomingMessageHandler());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }
}
