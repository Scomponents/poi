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

import java.util.Locale;

/**
 * Collection of string handling utilities
 */
public class StringUtil {

    private StringUtil() {
        // no instances of this class
    }

    public static String toLowerCase(char character) {
        return Character.toString(character).toLowerCase(Locale.getDefault());
    }

    public static String toUpperCase(char character) {
        return Character.toString(character).toUpperCase(Locale.getDefault());
    }

    public static boolean isUpperCase(char character) {
        String str = Character.toString(character);
        return str.toUpperCase(Locale.getDefault()).equals(str);
    }
}
