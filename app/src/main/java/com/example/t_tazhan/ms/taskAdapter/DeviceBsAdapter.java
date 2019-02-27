package com.example.t_tazhan.ms.taskAdapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.t_tazhan.ms.R;
import com.example.t_tazhan.ms.util.DateUtil;
import com.example.t_tazhan.ms.util.ScannedDevice;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeviceBsAdapter extends ArrayAdapter<ScannedDevice> {
    private static final String PREFIX_RSSI = "RSSI:";
    private static final String PREFIX_LASTUPDATED = "Last Updated:";
    private static int mPointNum = -1;

    private int mResId;
    private LayoutInflater mInflater;
    private List<ScannedDevice> mList;
    private String mWhiteList;

    public DeviceBsAdapter(Context context, int resId, List<ScannedDevice> objects) {
        super(context, resId, objects);

        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public List<ScannedDevice> getList() {
        return mList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ScannedDevice item = getItem(position);
        if (convertView == null)
            convertView = mInflater.inflate(mResId, null);

        final TextView address = convertView.findViewById(R.id.device_address);
        final TextView beaconInfo = convertView.findViewById(R.id.device_ibeacon_info);
        final TextView lastUpdated = convertView.findViewById(R.id.device_lastupdated);
        final TextView name = convertView.findViewById(R.id.device_name);
        final TextView point = convertView.findViewById(R.id.point_num);
        final TextView rssi = convertView.findViewById(R.id.device_rssi);
        final TextView scanRecord = convertView.findViewById(R.id.device_scanrecord);

        final Resources res = convertView.getContext().getResources();

        address.setText(item.getDevice().getAddress());
        lastUpdated.setText(PREFIX_LASTUPDATED + DateUtil.get_yyyyMMddHHmmssSSS(item.getLastUpdatedMs()));
        name.setText(item.getDisplayName());
        point.setText("#" + item.getPointNum());
        rssi.setText(PREFIX_RSSI + Integer.toString(item.getRssi()));

        if (mWhiteList.equals("")) {
            address.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            lastUpdated.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            name.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            point.setTextColor(res.getColor(android.R.color.holo_blue_bright));
            rssi.setTextColor(res.getColor(android.R.color.holo_blue_bright));
        } else if (item.getIBeacon() != null) {
            if (("," + mWhiteList + ",").contains(item.getDevice().getAddress())) {
                if (item.getPointNum() == mPointNum) {
                    address.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                    lastUpdated.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                    name.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                    point.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                    rssi.setTextColor(res.getColor(android.R.color.holo_blue_dark));
                } else {
                    address.setTextColor(res.getColor(android.R.color.holo_green_light));
                    lastUpdated.setTextColor(res.getColor(android.R.color.holo_green_light));
                    name.setTextColor(res.getColor(android.R.color.holo_green_light));
                    point.setTextColor(res.getColor(android.R.color.holo_green_light));
                    rssi.setTextColor(res.getColor(android.R.color.holo_green_light));
                }
            } else {
                address.setTextColor(Color.GRAY);
                lastUpdated.setTextColor(Color.GRAY);
                name.setTextColor(Color.GRAY);
                point.setTextColor(Color.GRAY);
                rssi.setTextColor(Color.GRAY);
            }

            beaconInfo.setText(res.getString(R.string.label_ibeacon) + "\n" + item.getIBeacon().toString());
            scanRecord.setText(item.getScanRecordHexString());

            beaconInfo.setVisibility(View.VISIBLE);
            scanRecord.setVisibility(View.VISIBLE);
        } else {
            address.setTextColor(Color.GRAY);
            lastUpdated.setTextColor(Color.GRAY);
            name.setTextColor(Color.GRAY);
            point.setTextColor(Color.GRAY);
            rssi.setTextColor(Color.GRAY);

            beaconInfo.setText("");
            scanRecord.setText("");

            beaconInfo.setVisibility(View.GONE);
            scanRecord.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void setPointNum(int pointNum) {
        mPointNum = pointNum;
    }

    public void setWhiteList(String whiteList) {
        mWhiteList = whiteList;
    }

    public int update(int pointNum, BluetoothDevice newDevice, int rssi, byte[] scanRecord) {
        if ((newDevice == null) || (newDevice.getAddress() == null))
            return -1;

        final long now = System.currentTimeMillis();
        mList.add(new ScannedDevice(pointNum, newDevice, rssi, scanRecord, now));

        notifyDataSetChanged();

        Set<String> whiteListAddresses = new HashSet<>();
        if (mList != null) {
            for (final ScannedDevice device : mList) {
                if (device.getIBeacon() != null) {
                    if (pointNum == device.getPointNum()) {
                        if (("," + mWhiteList + ",").contains(device.getDevice().getAddress())) {
                            whiteListAddresses.add(device.getDevice().getAddress());
                        }
                    }
                }
            }
        }

        final int result = whiteListAddresses.size();
        return result;
    }
}
