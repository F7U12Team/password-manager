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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import okhttp3.HttpUrl;


import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.facebook.react.bridge.*;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import cerrojoandroid.Cerrojoandroid;

import com.dropbox.core.android.Auth;

public class GoogleDriveModule extends ReactContextBaseJavaModule {

    public static final String TAG = "GoogleDriveModule";
    private static ReactContext rc;
    private static MainActivity ma;

    private boolean mLoggedIn;
    public static GoogleDriveModule self;
    private static String token;



    private static final String CLIENT_ID = "YOUR GOOGLE DRIVE OAUTH CLIENT ID";
    private static final String REDIRECT_URI = "me.madriguera.pma:/oauth2redirect";
    private static final String REDIRECT_URI_ROOT = "me.madriguera.pma";
    private static final String CODE = "code";
    public static final String API_SCOPE = "https://www.googleapis.com/auth/drive";



    public GoogleDriveModule(ReactApplicationContext reactContext) {
        super(reactContext);
        GoogleDriveModule.rc = reactContext;

        GoogleDriveModule.self = this;
    }

    @Override
    public String getName() {
        return "GoogleDriveModule";
    }

    private void logOut() {
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {

        mLoggedIn = loggedIn;
        if (!loggedIn) {
            GoogleDriveModule.token = "";
            Cerrojoandroid.googleDriveToken("");
        } else {
            Cerrojoandroid.googleDriveToken(GoogleDriveModule.token);
        }
    }

    @ReactMethod
    public void googleDriveButton(Promise promise) {
        try {
            if (mLoggedIn) {
                logOut();
            } else {


                HttpUrl authorizeUrl = HttpUrl.parse("https://accounts.google.com/o/oauth2/v2/auth") //
                        .newBuilder() //
                        .addQueryParameter("client_id", CLIENT_ID)
                        .addQueryParameter("scope", API_SCOPE)
                        .addQueryParameter("redirect_uri", REDIRECT_URI)
                        .addQueryParameter("response_type", CODE)
                        .build();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(String.valueOf(authorizeUrl.url())));
                this.ma.startActivity(i);

                /*
                // Embeded webview is not allowed to oauth request by Google :(
                WritableMap params = Arguments.createMap();
                Log.e(TAG, String.valueOf(authorizeUrl.url()));
                params.putString("data", "{\"uri\":\""+String.valueOf(authorizeUrl.url())+"\"}");
                PMModule.sendEvent("goWebView", params);*/

            }
            WritableMap map = Arguments.createMap();
            map.putString("result", "nothing");
            promise.resolve(map);
        } catch (Exception e) {
            Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    public void returnFromGoogleDrive(Uri data) {
        //Uri data = this.ma.getIntent().getData();
        Log.e(TAG, "RETURN FROM GOOGLE DRIVE");
        if (data != null && !TextUtils.isEmpty(data.getScheme())) {
            if (REDIRECT_URI_ROOT.equals(data.getScheme())) {
                String code = data.getQueryParameter(CODE);
                String error = data.getQueryParameter("error_code");
                Log.e(TAG, "onCreate: handle result of authorization with code :" + code);
                Log.e(TAG, "onCreate: handle result of authorization with code :" + data.getScheme());
                if (!TextUtils.isEmpty(code)) {
                    PMModule.sendToast("Please wait. Syncing with Google Drive.");
                    GoogleDriveModule.token = code;
                    setLoggedIn(true);
                }
                if(!TextUtils.isEmpty(error)) {
                    Log.e(TAG, "onCreate: handle result of authorization with error :" + error);
                }
            }
        }
    }



    public static void setMA(MainActivity ma) {
        GoogleDriveModule.ma = ma;
    }


}
