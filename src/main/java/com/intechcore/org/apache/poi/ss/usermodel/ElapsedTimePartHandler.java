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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElapsedTimePartHandler implements PartHandler {
    protected static final Pattern PERCENTS =  Pattern.compile("%");

    protected final List<ElapsedTimeSpec> specs = new ArrayList<>();

    protected ElapsedTimeSpec topmost;

    public List<ElapsedTimeSpec> getSpecs() {
        return specs;
    }

    public ElapsedTimeSpec getTopmost() {
        return topmost;
    }

    // This is the one class that's directly using printf, so it can't use
    // the default handling for quoted strings and special characters.  The
    // only special character for this is '%', so we have to handle all the
    // quoting in this method ourselves.

    public String handlePart(Matcher matcher, String part, FormatType type, StringBuffer desc) {
        int pos = desc.length();
        char firstCh = part.charAt(0);
        switch (firstCh) {
            case '[':
                if (part.length() < 3) {
                    break;
                }
                if (topmost != null) {
                    throw new IllegalArgumentException("Duplicate '[' times in format");
                }
                part = part.toLowerCase(Locale.ROOT);
                int specLen = part.length() - 2;
                topmost = assignSpec(part.charAt(1), pos, specLen);
                return part.substring(1, 1 + specLen);

            case 'h':
            case 'm':
            case 's':
            case '0':
                part = part.toLowerCase(Locale.ROOT);
                assignSpec(part.charAt(0), pos, part.length());
                return part;

            case '\n':
                return "%n";

            case '\"':
                part = part.substring(1, part.length() - 1);
                break;

            case '\\':
                part = part.substring(1);
                break;

            case '*':
                if (part.length() > 1) {
                    part = FormatPart.expandChar(part);
                }
                break;

            // An escape we can let it handle because it can't have a '%'
            case '_':
                return null;
            default:
                break;
        }
        // Replace ever "%" with a "%%" so we can use printf
        return PERCENTS.matcher(part).replaceAll("%%");
    }

    protected ElapsedTimeSpec assignSpec(char type, int pos, int len) {
        ElapsedTimeSpec spec = new ElapsedTimeSpec(type, pos, len);
        specs.add(spec);
        return spec;
    }
}
