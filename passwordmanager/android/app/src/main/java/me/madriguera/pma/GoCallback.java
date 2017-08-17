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
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import cerrojoandroid.JavaCallback;
import android.util.Log;



public class GoCallback implements JavaCallback {

    private String TAG = "GoCallback";

    public void setDropboxStatus(boolean ok) {
        Log.d(TAG, "SET SYNC ");
        if(ok) {
            PMModule.setSyncStatus(PMTypes.DROPBOX);
        } else {
            // no es del todo correcto
            PMModule.setSyncStatus(PMTypes.OFFLINE);
        }
        if(ok) {
            WritableMap params = Arguments.createMap();
            PMModule.sendEvent("goConnectDialog", params);
        }
    }

    public void sendEvent(String event, String data) {
        Log.d(TAG, "SENDEVENT [EVENT] " + event);
        Log.d(TAG, "SENDEVENT [DATA] " + data);
        WritableMap params = Arguments.createMap();
        params.putString("data", data);
        PMModule.sendEvent(event, params);
    }

    public void sendToast(String text) {
        PMModule.sendToast(text);
    }



}
