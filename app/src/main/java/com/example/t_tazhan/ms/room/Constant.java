package com.example.t_tazhan.ms.room;

import java.util.Set;
import java.util.TreeMap;
/*
if couldn't search out a beacon,then make the missing one as 0
 */
public class Constant {
    public static final String A = "0C:61:CF:AB:85:86";
    public static final String B = "0C:61:CF:AB:86:A3";
    public static final String C = "0C:61:CF:AB:85:B2";
    public static final String D = "19:18:FC:06:F7:6E";
    public static final String E = "19:18:FC:06:F7:68";
    public static final String F = "19:18:FC:06:F7:53";
    public static final String G = "19:18:FC:06:F7:51";
    public static final String H = "19:18:FC:06:F7:15";
    public static String getBeacon(String beacon) {
        String value;
        switch (beacon) {
            case A :
                value = "A";
                break;
            case B :
                value = "B";
                break;
            case C :
                value = "C";
                break;
            case D :
                value = "D";
                break;
            case E :
                value = "E";
                break;
            case F :
                value = "F";
                break;
            case G :
                value = "G";
                break;
                //找不到G
            case H :
                value = "H";
                break;
            default:
                value = "mac";
        }
        return value;
    }
    public static TreeMap<String,String> map = new TreeMap<String, String>();
    public static void storageValue(String key,String value) {
        map.put(key,value);
    }
    public static Set<String> set ;
    //未搜索到，信标RSSI置为0
    public static void ifConclude( ) {
        set = map.keySet();
        if (!set.contains("A")) {
            map.put("A","0");
        }
        if (!set.contains("B")) {
            map.put("B","0");
        }
        if (!set.contains("C")) {
            map.put("C","0");
        }
        if (!set.contains("D")) {
            map.put("D","0");
        }
        if (!set.contains("E")) {
            map.put("E","0");
        }
        if (!set.contains("F")) {
            map.put("F","0");
        }
        if (!set.contains("G")) {
            map.put("G","0");
        }
        if (!set.contains("H")) {
            map.put("H","0");
        }
    }
}
