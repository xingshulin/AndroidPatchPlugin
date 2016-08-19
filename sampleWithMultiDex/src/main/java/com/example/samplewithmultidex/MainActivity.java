package com.example.samplewithmultidex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import com.example.samplelibrary.LibraryActivity;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.example.samplewithmultidex.IWelcomeService.Stub.asInterface;
import static com.example.samplewithmultidex.MessengerService.MESSAGE_TYPE_WELCOME;

public class MainActivity extends AppCompatActivity {
    private Messenger serviceMessenger;
    private IWelcomeService welcomeService;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessengerService.MESSAGE_TYPE_WELCOME:
                    makeText(MainActivity.this, msg.getData().getString(MessengerService.KEY_WELCOME), LENGTH_SHORT).show();
                    return;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    private Messenger messageReceiver = new Messenger(handler);

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceMessenger = null;
        }
    };

    private ServiceConnection aidlConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            welcomeService = asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            welcomeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.welcome)).setText("Default Text from app");
        initLibraryButton();
        initMessengerButton();
        initAIDLServiceButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent messengerService = new Intent(getApplicationContext(), MessengerService.class);
        bindService(messengerService, connection, Context.BIND_AUTO_CREATE);
        Intent aidlService = new Intent(getApplicationContext(), AIDLService.class);
        bindService(aidlService, aidlConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        unbindService(aidlConnection);
    }

    private void initMessengerButton() {
        findViewById(R.id.messenger).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceMessenger == null) {
                    makeText(MainActivity.this, "Service is not bound", LENGTH_SHORT).show();
                    return;
                }
                Message message = Message.obtain();
                message.what = MESSAGE_TYPE_WELCOME;
                message.replyTo = messageReceiver;
                try {
                    serviceMessenger.send(message);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void initLibraryButton() {
        findViewById(R.id.library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LibraryActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initAIDLServiceButton() {
        findViewById(R.id.aidl_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (welcomeService == null) {
                    makeText(MainActivity.this, "Service is not bound", LENGTH_SHORT).show();
                    return;
                }
                try {
                    Person person = new Person("Xiaofan", "Zhang", 16);
                    String welcome = welcomeService.welcome(person);
                    makeText(MainActivity.this, welcome, LENGTH_SHORT).show();
                } catch (RemoteException ignored) {
                }
            }
        });
    }
}
