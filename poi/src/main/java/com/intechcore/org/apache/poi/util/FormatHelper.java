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

   2021 - Intechcore GmbH.
   This class is modified copy of Apache POI 4.1.2 class.
   It was modified to use Apache POI's data formatting
   in SCell product.
==================================================================== */
package com.intechcore.org.apache.poi.util;

public class FormatHelper {
    public static final String GENERAL_CODE = "General";
    public static final String TEXT_FORMAT = "@";

    /*
     * Cells that cannot be formatted, e.g. cells that have a date or time
     * format and have an invalid date or time value, are displayed as 255
     * pound signs ("#").
     */
    private static final String HASH_TAGS = "###################################################";
    public static final String INVALID_VALUE_FOR_FORMAT = HASH_TAGS + HASH_TAGS + HASH_TAGS + HASH_TAGS + HASH_TAGS;

    public static boolean isGeneralFormatCode(String format) {
        return format.equals(GENERAL_CODE) || TEXT_FORMAT.equals(format);
    }
}
