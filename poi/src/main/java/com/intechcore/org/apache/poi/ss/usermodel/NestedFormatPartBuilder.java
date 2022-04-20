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
package com.intechcore.org.apache.poi.ss.usermodel;

import java.util.Locale;
import java.util.Optional;

public class NestedFormatPartBuilder {
    private final FormatPart source;

    private Integer argb;
    private Integer decimalsCount;
    private Boolean setThousandsSeparator;
    private Boolean setMinus;
    private Boolean setBraces;

    public NestedFormatPartBuilder(FormatPart source) {
        this.source = source;
    }

    public String build() {
        String resultColor = Optional.ofNullable(this.source.colorString).orElse("");
        if (this.argb != null) {
            if (PredefinedFormatColors.BLACK.getColor() == this.argb) {
                resultColor = "";
            } else {
                String newColor = FormatPart.getColorName(this.argb).toLowerCase(Locale.ROOT);
                newColor = String.valueOf(newColor.charAt(0)).toUpperCase(Locale.ROOT) + newColor.substring(1);
                resultColor = "[" + newColor + "]";
            }
        }

        String resultCondition = Optional.ofNullable(this.source.conditionString).orElse("");

        String resultFormat = this.source.format.createNestedFormatWith(decimalsCount, setThousandsSeparator, setMinus);

        resultFormat = handleSetBrackets(resultFormat, setBraces);

        return resultColor + resultCondition + resultFormat;

    }

    public NestedFormatPartBuilder color(Integer argb) {
        this.argb = argb;
        return this;
    }

    public NestedFormatPartBuilder decimals(Integer decimalsCount) {
        this.decimalsCount = decimalsCount;
        return this;
    }

    public NestedFormatPartBuilder thousandsSeparator(Boolean set) {
        this.setThousandsSeparator = set;
        return this;
    }

    public NestedFormatPartBuilder minus(Boolean set) {
        this.setMinus = set;
        return this;
    }

    public NestedFormatPartBuilder braces(Boolean set) {
        this.setBraces = set;
        return this;
    }

    private static String handleSetBrackets(String resultFormat, Boolean setBraces) {
        if (setBraces == null) {
            return resultFormat;
        }

        boolean bracesAlreadyHere = (resultFormat.contains("\\(") && resultFormat.contains("\\)"))
                || (resultFormat.contains("\"(\"") && resultFormat.contains("\")\""))
                || (resultFormat.contains("(") && resultFormat.contains(")"));

        if (!setBraces && bracesAlreadyHere) {
            resultFormat = resultFormat
                    .replace("\\(", "")
                    .replace("\\)", "")
                    .replace("\")\"", "")
                    .replace("\"(\"", "")
                    .replace(")", "")
                    .replace("(", "");
        }
        if (setBraces && !bracesAlreadyHere) {
            resultFormat = "(" + resultFormat + ")";
        }

        return resultFormat;
    }
}
