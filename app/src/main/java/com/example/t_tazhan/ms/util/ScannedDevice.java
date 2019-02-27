// Copyright (c) 2015 YA <ya.androidapp@gmail.com> All rights reserved.
/*
 * Copyright (C) 2013 youten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.t_tazhan.ms.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

//将获取到的蓝牙设备转码，并验证正确
public class ScannedDevice {
    private static final String UNKNOWN = "Unknown";

    private BluetoothDevice mDevice;

    private int mRssi;

    private int mPointNum;//?

    private String mDisplayName;

    private byte[] mScanRecord;

    private IBeacon mIBeacon;

    private long mLastUpdatedMs;

    public ScannedDevice(int pointNum, BluetoothDevice device, int rssi, byte[] scanRecord, long now) {
        if (device == null)
            throw new IllegalArgumentException("BluetoothDevice is null");

        mPointNum = pointNum;
        mDevice = device;
        mDisplayName = device.getName();
        if ((mDisplayName == null) || (mDisplayName.length() == 0))
            mDisplayName = UNKNOWN;
        mRssi = rssi;
        mScanRecord = scanRecord;
        mLastUpdatedMs = now;
        checkIBeacon();
    }

    @SuppressLint("DefaultLocale")
    public static String asHex(byte bytes[]) {
        if ((bytes == null) || (bytes.length == 0)) {
            return "";
        }

        StringBuffer sb = new StringBuffer(bytes.length * 2);

        for (int index = 0; index < bytes.length; index++) {
            int bt = bytes[index] & 0xff;

            if (bt < 0x10) {
                sb.append("0");
            }

            sb.append(Integer.toHexString(bt).toUpperCase());
        }

        return sb.toString();
    }

    private void checkIBeacon() {
        if (mScanRecord != null) {
            mIBeacon = IBeacon.fromScanData(mScanRecord, mRssi);
        }
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public int getRssi() {
        return mRssi;
    }

    public long getLastUpdatedMs() {
        return mLastUpdatedMs;
    }

    public String getScanRecordHexString() {
        return ScannedDevice.asHex(mScanRecord);
    }

    public IBeacon getIBeacon() {
        return mIBeacon;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public int getPointNum() {
        return mPointNum;
    }

//将文件格式输出为Csv
    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(String.format("%1$05d", mPointNum)).append(",");
        sb.append(mDevice.getAddress()).append(",");
        sb.append(mRssi).append(",");
        sb.append(DateUtil.get_yyyyMMddHHmmssSSS(mLastUpdatedMs)).append(",");
        sb.append(mDisplayName).append(",");
        if (mIBeacon == null) {
            sb.append("false,,0,0,0");
        } else {
            sb.append("true").append(",");
            sb.append(mIBeacon.toCsv());
        }
        return sb.toString();
    }
}
