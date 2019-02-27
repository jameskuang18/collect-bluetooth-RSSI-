package com.example.t_tazhan.ms.util;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.util.Log;

public class IBeacon {

    public static final int PROXIMITY_IMMEDIATE = 1;//PROXIMITY距离

    public static final int PROXIMITY_NEAR = 2;

    public static final int PROXIMITY_FAR = 3;

    public static final int PROXIMITY_UNKNOWN = 0;

    final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static final String TAG = "IBeacon";    

    protected String proximityUuid;

    protected int major;

    protected int minor;

    protected Integer proximity;

    protected Double accuracy;

    protected int rssi;

    protected int txPower;

    protected Double runningAverageRssi = null;

    public double getAccuracy(){
        if (accuracy == null) {
            accuracy = calculateAccuracy(txPower, runningAverageRssi != null ? runningAverageRssi : rssi );
        }
        return accuracy;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    /*public int getProximity() {
        if (proximity == null) {
            proximity = calculateProximity(getAccuracy());
        }
        return proximity;
    }该函数未调用*/

    public int getRssi() {
        return rssi;
    }

    public int getTxPower() {
        return txPower;
    }
//获取厂商识别号
    public String getProximityUuid() {
        return proximityUuid;
    }
    
    @Override
    public int hashCode() {
        return minor;
    }

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof IBeacon)) {
            return false;
        }
        IBeacon thatIBeacon = (IBeacon) that;       
        return (thatIBeacon.getMajor() == this.getMajor() && thatIBeacon.getMinor() == this.getMinor() && thatIBeacon.getProximityUuid().equals(this.getProximityUuid()));
    }

    public static IBeacon fromScanData(byte[] scanData, int rssi) {
        int startByte = 0;//=2？
        boolean patternFound = false;
        while (startByte <= 5) {
            if (((int)scanData[startByte] & 0xff) == 0x4c &&
                ((int)scanData[startByte+1] & 0xff) == 0x00 &&
                ((int)scanData[startByte+2] & 0xff) == 0x02 &&
                ((int)scanData[startByte+3] & 0xff) == 0x15) {          
                patternFound = true;
                break;
            } else if (((int)scanData[startByte] & 0xff) == 0x2d &&
                    ((int)scanData[startByte+1] & 0xff) == 0x24 &&
                    ((int)scanData[startByte+2] & 0xff) == 0xbf &&
                    ((int)scanData[startByte+3] & 0xff) == 0x16) {  
                IBeacon iBeacon = new IBeacon();
                iBeacon.major = 0;
                iBeacon.minor = 0;
                iBeacon.proximityUuid = "00000000-0000-0000-0000-000000000000";
                iBeacon.txPower = -55;
                return iBeacon;
            }                   
            startByte++;
        }

        if (patternFound == false) {
            return null;
        }
                                
        IBeacon iBeacon = new IBeacon();
        
        iBeacon.major = (scanData[startByte+20] & 0xff) * 0x100 + (scanData[startByte+21] & 0xff);
        iBeacon.minor = (scanData[startByte+22] & 0xff) * 0x100 + (scanData[startByte+23] & 0xff);
        iBeacon.txPower = (int)scanData[startByte+24]; // this one is signed
        iBeacon.rssi = rssi;

        byte[] proximityUuidBytes = new byte[16];
        System.arraycopy(scanData, startByte+4, proximityUuidBytes, 0, 16); 
        String hexString = bytesToHex(proximityUuidBytes);
        StringBuilder sb = new StringBuilder();
        sb.append(hexString.substring(0,8));
        sb.append("-");
        sb.append(hexString.substring(8,12));
        sb.append("-");
        sb.append(hexString.substring(12,16));
        sb.append("-");
        sb.append(hexString.substring(16,20));
        sb.append("-");
        sb.append(hexString.substring(20,32));
        iBeacon.proximityUuid = sb.toString();

        return iBeacon;
    }
    
    protected IBeacon(IBeacon otherIBeacon) {
        this.major = otherIBeacon.major;
        this.minor = otherIBeacon.minor;
        this.accuracy = otherIBeacon.accuracy;
        this.proximity = otherIBeacon.proximity;
        this.rssi = otherIBeacon.rssi;
        this.proximityUuid = otherIBeacon.proximityUuid;
        this.txPower = otherIBeacon.txPower;
    }
    
    protected IBeacon() {
        
    }

    protected IBeacon(String proximityUuid, int major, int minor, int txPower, int rssi) {
        this.proximityUuid = proximityUuid;
        this.major = major;
        this.minor = minor;
        this.rssi = rssi;
        this.txPower = txPower;
    }
    
    @SuppressLint("DefaultLocale")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("UUID=").append(this.proximityUuid.toUpperCase());
        sb.append(" Major=").append(this.major);
        sb.append(" Minor=").append(this.minor);
        sb.append(" TxPower=").append(this.txPower);
        
        return sb.toString();
    }    
    
    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.proximityUuid.toUpperCase(Locale.ENGLISH)).append(",");
        sb.append(this.major).append(",");
        sb.append(this.minor).append(",");
        sb.append(this.txPower);
        
        return sb.toString();
    }

//判断当前rssi值准确度
    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0;
        }

        Log.d(TAG, "calculating accuracy based on rssi of "+rssi);


        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            Log.d(TAG, " avg rssi: "+rssi+" accuracy: "+accuracy);
            return accuracy;
        }
    }
    
    protected static int calculateProximity(double accuracy) {
        if (accuracy < 0) {
            return PROXIMITY_UNKNOWN;
        }
        if (accuracy < 0.5 ) {
            return IBeacon.PROXIMITY_IMMEDIATE;
        }
        if (accuracy <= 4.0) {
            return IBeacon.PROXIMITY_NEAR;
        }
        return IBeacon.PROXIMITY_FAR;

    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    } 
}
