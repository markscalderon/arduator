package com.e_regular_games.arduator.arduino;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author S. Ryan Edgar
 * Class used to discover and connect to Bluetooth 2.0 devices. If your intend to support
 * Bluetooth 2 and 4 it is suggested to use ArduinoCommManagerAny.
 */
public class ArduinoCommManagerBt extends ArduinoCommManager {

    public ArduinoCommManagerBt(Activity app) {
        super(app);
    }

    @Override
    public void find() {
        AfterEnable afterEnableFind = new AfterEnable() {
            @Override
            public void after() {
                find();
            }
        };

        if (!enable(afterEnableFind)) {
            return;
        }
        if (findInProgress) {
            return;
        }

        onStatusChange(BluetoothStatus.Searching);
        stopFindTask = new TimerTask() {
            @Override
            public void run() {
                cancelFind();
                stopFindTask = null;
            }
        };

        // stop find after 2 minutes.
        stopFindTimer = new Timer();
        stopFindTimer.schedule(stopFindTask, 120 * 1000);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        app.registerReceiver(mReceiver, filter);

        findInProgress = true;
        mBt.startDiscovery();
    }

    @Override
    public void cancelFind() {
        if (findInProgress) {
            app.unregisterReceiver(mReceiver);
            mBt.cancelDiscovery();
            findInProgress = false;
            onStatusChange(BluetoothStatus.Enabled);
        }
    }

    @Override
    public void createArduinoComm(final BluetoothDevice device) {
        AfterEnable afterEnableCreate = new AfterEnable() {
            @Override
            public void after() {
                ArduinoComm station = new ArduinoCommBt(app, device);
                onCreateStation(station);
            }
        };

        if (!enable(afterEnableCreate)) {
            return;
        }

        if (findInProgress) {
            onStatusChange(BluetoothStatus.Enabled);
            cancelFind();
        }

        ArduinoComm station = new ArduinoCommBt(app, device);
        onCreateStation(station);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

    }

    private boolean findInProgress = false;
    private TimerTask stopFindTask;
    private Timer stopFindTimer;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                onFind(device);
            }
        }
    };
}
