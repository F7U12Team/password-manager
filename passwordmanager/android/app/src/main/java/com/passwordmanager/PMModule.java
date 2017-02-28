/*This is distributed under the Apache License v2.0

Copyright 2017 F7U12 Team - pma@madriguera.me

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package me.madriguera.pma;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.*;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import go.tesoroandroid.Tesoroandroid;


public class PMModule extends ReactContextBaseJavaModule {

    public static final String TAG = "PMModule";
    private static ReactContext rc;
    private static MainActivity ma;
    private static int syncStatus = PMTypes.NONE;


    public PMModule(ReactApplicationContext reactContext) {
        super(reactContext);
        PMModule.rc = reactContext;

        // REGISTER CALLBACK
        GoCallback gocb = new GoCallback();
        Tesoroandroid.registerJavaCallback(gocb);
    }

    @Override
    public String getName() {
        return "PMModule";
    }

    @ReactMethod
    public void inputPin(String pin, Promise promise) {
        try {
            Log.d(TAG,"TESORO PIN:  " + pin);
            Tesoroandroid.inputPin(pin);
            WritableMap map = Arguments.createMap();
            promise.resolve(map);
        } catch (Exception e) {
            Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    @ReactMethod
    public void getlogins(Promise promise) {
        try {
            Tesoroandroid.getLogins();
            WritableMap map = Arguments.createMap();
            promise.resolve(map);
        } catch (Exception e) {
            Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    @ReactMethod
    public void getLogin(String id, Boolean edit, Promise promise) {
        try {
            Tesoroandroid.getLogin(id, edit);
            WritableMap map = Arguments.createMap();
            promise.resolve(map);
        } catch (Exception e) {
            Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    @ReactMethod
    public void deleteLogin(String id, Promise promise) {
        try {
            Tesoroandroid.deleteLogin(id);
            WritableMap map = Arguments.createMap();
            promise.resolve(map);
        } catch (Exception e) {
            Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    @ReactMethod
    public void saveLogin(String title, String username, String note, String password, String secretNote, String id, Promise promise) {
        try {
            Tesoroandroid.saveLogin(title, username, note, password, secretNote, id);
            WritableMap map = Arguments.createMap();
            promise.resolve(map);
        } catch (Exception e) {
            Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    @ReactMethod
    public static void setSyncStatus(int status) {
        PMModule.syncStatus = status;
        Log.d(TAG, PMModule.syncStatus + "  SET SYNC STATUS  " + status);
    }

    public static int getSyncStatus() {
        Log.d(TAG, "GET SYNC STATUS" + PMModule.syncStatus);
        return PMModule.syncStatus;
    }

    @ReactMethod
    public static void getSyncStatusPromise(Promise promise) {
        Log.d(TAG, "GET SYNC STATUS" + PMModule.syncStatus);
        WritableMap map = Arguments.createMap();
        map.putInt("data", PMModule.syncStatus);
        promise.resolve(map);
    }

    @ReactMethod
    public static void connectTo() {
        if(PMModule.ma.getIsConnected()!=-1) {
            Log.d(TAG, "GOING TO CONNECT");
            PMModule.ma.connectToDevice(PMModule.ma.getDevice());
        } else {
            Log.d(TAG, "NOT CONNECTED");
            PMModule.ma.checkForDevices();
        }
    }

    public static void setMA(MainActivity ma) {
        PMModule.ma = ma;
    }

    public static void sendEvent(String eventName, @Nullable WritableMap params) {
        PMModule.rc.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}
