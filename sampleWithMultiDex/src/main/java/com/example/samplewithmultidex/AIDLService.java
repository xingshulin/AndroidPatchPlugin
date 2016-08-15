package com.example.samplewithmultidex;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class AIDLService extends Service {

    private IWelcomeService welcomeService = new IWelcomeService.Stub() {

        @Override
        public String welcome() throws RemoteException {
            return "Hello from AIDLService!";
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return welcomeService.asBinder();
    }
}
