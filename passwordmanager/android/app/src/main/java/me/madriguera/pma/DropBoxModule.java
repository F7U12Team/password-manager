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
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

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
    private static final String APP_SECRET = "YOUR DROP BOX SECRET";

    ///////////////////////////////////////////////////////////////////////////
    //                      End app-specific settings.                       //
    ///////////////////////////////////////////////////////////////////////////

    // You don't need to change these, leave them alone.
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private static final boolean USE_OAUTH1 = false;
    DropboxAPI<AndroidAuthSession> mApi;
    private boolean mLoggedIn;
    public static DropBoxModule self;

    public DropBoxModule(ReactApplicationContext reactContext) {
        super(reactContext);
        DropBoxModule.rc = reactContext;

        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mApi = new DropboxAPI<AndroidAuthSession>(session);
        DropBoxModule.self = this;
    }

    @Override
    public String getName() {
        return "DropBoxModule";
    }

    private void logOut() {
        mApi.getSession().unlink();
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {

        mLoggedIn = loggedIn;
        if (!loggedIn) {
            Cerrojoandroid.dropboxToken("");
        } else {
            String oauth2AccessToken = mApi.getSession().getOAuth2AccessToken();
            Cerrojoandroid.dropboxToken(oauth2AccessToken);
        }
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        return session;
    }

    @ReactMethod
    public void dropboxButton(Promise promise) {
        try {
            if (mLoggedIn) {
                logOut();
            } else {
                // Start the remote authentication
                if (USE_OAUTH1) {
                    mApi.getSession().startAuthentication(rc);
                } else {
                    mApi.getSession().startOAuth2Authentication(rc);
                }
            }

            setLoggedIn(mApi.getSession().isLinked());


            WritableMap map = Arguments.createMap();
            map.putString("result", "nothing");
            promise.resolve(map);
        } catch (Exception e) {
            Log.e(TAG, "Solve error: " + e.toString());
            promise.reject(e);
        }
    }

    public void returnFromDropbox() {
        Log.d(TAG, "ON RESUME DROPBOX");
        AndroidAuthSession session = mApi.getSession();
        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();
                setLoggedIn(true);
            } catch (IllegalStateException e) {
                Log.i(TAG, "Error authenticating", e);
            }
        }
    }

}
