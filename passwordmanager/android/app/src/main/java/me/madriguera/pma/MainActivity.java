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

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import java.util.HashMap;
import java.util.Iterator;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.*;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import cerrojoandroid.Cerrojoandroid;


public class MainActivity extends ReactActivity {

    private String TAG = "MainActivity";
    private int VID = 21324;
    private int PID = 1;
    private UsbAccessory accessory;
    private static final String ACTION_USB_PERMISSION = "me.madriguera.pma.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private UsbManager manager;
    private UsbDeviceConnection connection;
    private HashMap<Integer, Integer> connectedDevices;
    private int fdint;
    private int isConnected;
    private UsbDevice device;
    private static boolean registered;

    /**
     * Returns the name of the main component registered from JavaScript.
     * This is used to schedule rendering of the component.
     */
    @Override
    protected String getMainComponentName() {
        return "PasswordManager";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "REGISTERED " + MainActivity.registered);
        if(!MainActivity.registered) {
            MainActivity.registered = true;
            manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            isConnected = -1;

            registerReceiver(usbManagerBroadcastReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
            registerReceiver(usbManagerBroadcastReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
            registerReceiver(usbManagerBroadcastReceiver, new IntentFilter(ACTION_USB_PERMISSION));

            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

            final Handler handler = new Handler();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkForDevices();
                }
            }, 1000);
            PMModule.setMA(this);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ON RESUME MAIN ACTIVITY");
        if(DropBoxModule.self!=null) {
            DropBoxModule.self.returnFromDropbox();
        }
    }

    private final BroadcastReceiver usbManagerBroadcastReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            try
            {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action))
                {
                    synchronized (this)
                    {
                        device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                        {
                            if(device != null)
                            {
                                if(PMModule.getSyncStatus()!=PMTypes.NONE) {
                                    fdint = connectToDevice(device);
                                    Log.d(TAG,"device file descriptor: " + fdint);
                                } else {
                                    Log.d(TAG,"SYNC STATUS NONE");
                                }
                            }
                        }
                        else
                        {
                            Log.d(TAG, "permission denied for device " + device);
                        }
                    }
                }

                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
                {
                    Log.d(TAG, "onDeviceConnected");

                    synchronized(this)
                    {
                        device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (device != null && device.getDeviceId() != isConnected)
                        {
                            manager.requestPermission(device, mPermissionIntent);
                        }
                    }
                }

                if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
                {
                    Log.d(TAG, "onDeviceDisconnected");
                    isConnected = -1;
                    Cerrojoandroid.cleanData();

                    WritableMap params = Arguments.createMap();
                    params.putString("data", "");
                    if(PMModule.getSyncStatus()!=PMTypes.NONE) {
                        PMModule.sendEvent("goConnectDialog", params);
                    }

                    //PMModule.setSyncStatus(PMTypes.NONE);

                    synchronized(this)
                    {
                        device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        int fd = connectedDevices.get(device.getDeviceId());

                        Log.d(TAG, "device: " + device.getDeviceId() + " disconnected. fd: " + fd);
                        connectedDevices.remove(device.getDeviceId());
                    }

                }
            }
            catch(Exception e)
            {
                Log.d(TAG, "Exception: " + e);
            }
        }
    };

    public int connectToDevice(UsbDevice device)
    {
        connection = manager.openDevice(device);
        connection.claimInterface(device.getInterface(0), true);
        Log.d(TAG, "inserting device with id: " + device.getDeviceId() + " and file descriptor: " + connection.getFileDescriptor());
        String i = "\n" + "DeviceID: " + device.getDeviceId() + "\n"
                + "DeviceName: " + device.getDeviceName() + "\n"
                + "DeviceClass: " + device.getDeviceClass() + " - "
                + "DeviceSubClass: " + device.getDeviceSubclass() + "\n"
                + "VendorID: " + device.getVendorId() + "\n"
                + "ProductID: " + device.getProductId() + "\n";
        Log.d(TAG, i);
        try {
            Cerrojoandroid.connect(connection.getFileDescriptor(), device.getDeviceName(), "trezor");
            fdint = connection.getFileDescriptor();
            Log.d(TAG, "CONNECTED? " + fdint);
        }catch(Exception e)
        {
            Log.d(TAG, "Exception: " + e);
        }
        isConnected = device.getDeviceId();
        // TODO FIX THIS
        //connectedDevices.put(device.getDeviceId(), connection.getFileDescriptor());
        return connection.getFileDescriptor();
    }

    public void checkForDevices()
    {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while(deviceIterator.hasNext())
        {
            UsbDevice device = deviceIterator.next();

            if(device.getDeviceId()!=isConnected) {
                if (device.getVendorId()==VID && device.getProductId()==PID) {
                    Log.d(TAG, "Found a device: " + device + "   ID " + isConnected + "   ======  " + device.getDeviceId());
                    manager.requestPermission(device, mPermissionIntent);
                }
            }
        }
    }

    public int getIsConnected() {
        return this.isConnected;
    }

    public UsbDevice getDevice() {
        return this.device;
    }

}
