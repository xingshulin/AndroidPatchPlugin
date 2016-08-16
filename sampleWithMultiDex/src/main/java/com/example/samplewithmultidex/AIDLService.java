package com.example.samplewithmultidex;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

public class AIDLService extends Service {

    private IWelcomeService welcomeService = new IWelcomeService.Stub() {

        @Override
        public String welcome(Person person) throws RemoteException {
            return String.format("Welcome %s!", person.display());
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return welcomeService.asBinder();
    }
}
