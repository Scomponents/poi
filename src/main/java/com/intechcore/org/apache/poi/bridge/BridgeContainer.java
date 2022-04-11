/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   2022 - Intechcore GmbH.
==================================================================== */

package com.intechcore.org.apache.poi.bridge;

import java.time.LocalDateTime;

public final class BridgeContainer {
    private static LocalDateTime START_DATE_1904;
    private static IDateTimeUtilsBridge dateTimeUtils;
    private static IValueFormatDetectorStorageBridge detectorStorage;

    public static void Init(LocalDateTime startDate1904, IDateTimeUtilsBridge dateTimeUtils,
                            IValueFormatDetectorStorageBridge detectorStorage) {
        START_DATE_1904 = startDate1904;
        BridgeContainer.dateTimeUtils = dateTimeUtils;
        BridgeContainer.detectorStorage = detectorStorage;
    }

    public static LocalDateTime getStartDate1904() {
        return START_DATE_1904;
    }

    public static IDateTimeUtilsBridge getDateTimeUtils() {
        return dateTimeUtils;
    }

    public static IValueFormatDetectorStorageBridge getDetectorStorage() {
        return detectorStorage;
    }
}
