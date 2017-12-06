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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.facebook.react.bridge.*;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import cerrojoandroid.Cerrojoandroid;

import com.dropbox.core.android.Auth;

public class DropBoxModule extends ReactContextBaseJavaModule {

    public static final String TAG = "DropBoxModule";
    private static ReactContext rc;

    ///////////////////////////////////////////////////////////////////////////
    //                      Your app-specific settings.                      //
    ///////////////////////////////////////////////////////////////////////////

    // Replace this with your app key and secret assigned by Dropbox.
    // Note that this is a really insecure way to do this, and you shouldn't
    // ship code which contains your key & secret in such an obvious way.
    // Obfuscation is good.
    //private static final String APP_KEY = "YOUR DROP BOX KEY";
    //private static final String APP_SECRET = "YOUR DROP BOX SECRET";
    private static final String APP_KEY = "YOUR DROP BOX KEY";

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    // You don't need to change these, leave them alone.
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    //DropboxAPI<AndroidAuthSession> mApi;
    private boolean mLoggedIn;
    public static DropBoxModule self;
    private static String token;

    public DropBoxModule(ReactApplicationContext reactContext) {
        super(reactContext);
        DropBoxModule.rc = reactContext;

        DropBoxModule.self = this;
    }

    @Override
    public String getName() {
        return "DropBoxModule";
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
            DropBoxModule.token = "";
            Cerrojoandroid.dropboxToken("");
        } else {
            Cerrojoandroid.dropboxToken(DropBoxModule.token);
        }
    }

    @ReactMethod
    public void dropboxButton(Promise promise) {
        try {
            if (mLoggedIn) {
                logOut();
            } else {
                Auth.startOAuth2Authentication(rc, APP_KEY);
            }
            WritableMap map = Arguments.createMap();
            map.putString("result", "nothing");
            promise.resolve(map);
        } catch (Exception e) {
            //Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    public void returnFromDropbox() {
        String accessToken = Auth.getOAuth2Token();
        //Log.d(TAG, "ON RESUME DROPBOX " + accessToken);
        //assume it's logged?
        if (accessToken!="") {
            try {
                DropBoxModule.token = accessToken;
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                //Log.i(TAG, "Error authenticating", e);
            }
        }
    }

}
