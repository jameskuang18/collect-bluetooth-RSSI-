// Copyright (c) 2015 YA <ya.androidapp@gmail.com> All rights reserved.
/*
 * Copyright (C) 2014 youten
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

import android.os.Environment;
import java.util.List;
//更新存储名称和路径
public class CsvDumpUtil {
    private static final String HEADER_BS   = "Position Name,BSSID,RSSI,Last Updated,DisplayName,iBeacon flag,Proximity UUID,major,minor,TxPower";
    private static final String DUMP_PATH = "/BSScanner/";
    public static String dumpBs(List<ScannedDevice> deviceList) {
        if ((deviceList == null) || (deviceList.size() == 0)) {
            return null;
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + DUMP_PATH
                + DateUtil.get_nowBsCsvFilename();

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_BS).append("\n");
        for (ScannedDevice device : deviceList) {
            sb.append(device.toCsv()).append("\n");
        }
        return path;
    }
}
