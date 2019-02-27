package com.example.t_tazhan.ms.util;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

public class BleUtil {

    private BleUtil() {

    }

    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static BluetoothManager getManager(Context context) {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }
}
