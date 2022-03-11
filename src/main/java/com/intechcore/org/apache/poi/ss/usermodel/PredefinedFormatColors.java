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
package com.intechcore.org.apache.poi.ss.usermodel;

import com.intechcore.scomponents.helper.ARGB;

public enum PredefinedFormatColors {
    BLACK                (0xFF000000),
    BROWN                (0xFF993300),
    OLIVE_GREEN          (0xFF333300),
    DARK_GREEN           (0xFF003300),
    DARK_TEAL            (0xFF003366),
    DARK_BLUE            (0xFF000080),
    INDIGO               (0xFF333399),
    GREY_80_PERCENT      (0xFF333333),
    ORANGE               (0xFFFF6600),
    DARK_YELLOW          (0xFF808000),
    GREEN                (0xFF008000),
    TEAL                 (0xFF008080),
    BLUE                 (0xFF0000FF),
    BLUE_GREY            (0xFF666699),
    GREY_50_PERCENT      (0xFF808080),
    RED                  (0xFFFF0000),
    LIGHT_ORANGE         (0xFFFF9900),
    LIME                 (0xFF99CC00),
    SEA_GREEN            (0xFF339966),
    AQUA                 (0xFF33CCCC),
    LIGHT_BLUE           (0xFF3366FF),
    VIOLET               (0xFF800080),
    GREY_40_PERCENT      (0xFF969696),
    PINK                 (0xFFFF00FF),
    GOLD                 (0xFFFFCC00),
    YELLOW               (0xFFFFFF00),
    BRIGHT_GREEN         (0xFF00FF00),
    TURQUOISE            (0xFF00FFFF),
    DARK_RED             (0xFF800000),
    SKY_BLUE             (0xFF00CCFF),
    PLUM                 (0xFF993366),
    GREY_25_PERCENT      (0xFFC0C0C0),
    ROSE                 (0xFFFF99CC),
    LIGHT_YELLOW         (0xFFFFFF99),
    LIGHT_GREEN          (0xFFCCFFCC),
    LIGHT_TURQUOISE      (0xFFCCFFFF),
    PALE_BLUE            (0xFF99CCFF),
    LAVENDER             (0xFFCC99FF),
    WHITE                (0xFFFFFFFF),
    CORNFLOWER_BLUE      (0xFF9999FF),
    LEMON_CHIFFON        (0xFFFFFFCC),
    MAROON               (0xFF7F0000),
    ORCHID               (0xFF660066),
    CORAL                (0xFFFF8080),
    ROYAL_BLUE           (0xFF0066CC),
    LIGHT_CORNFLOWER_BLUE(0xFFCCCCFF),
    TAN                  (0xFFFFCC99),
    AUTOMATIC            (0xFF000000);

    private final ARGB color;

    PredefinedFormatColors(int argb) {
        this.color = ARGB.create(argb);
    }

    /**
     * @return (a copy of) the {@link ARGB} assigned to the enum.
     */
    public ARGB getColor() {
        return color;
    }
}
