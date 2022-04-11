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
   This class is modified copy of Apache POI 4.1.2 class.
   It was modified to use Apache POI's data formatting
   in SCell product.
==================================================================== */
package com.intechcore.org.apache.poi.ss.usermodel;

import com.intechcore.org.apache.poi.util.StringUtil;
import java.util.Locale;
import java.util.regex.Matcher;

public class DatePartHandler implements PartHandler {
    protected boolean amPmUpper;
    protected boolean showM;
    protected boolean showAmPm;
    protected String sFmt;

    protected int mStart = -1;
    protected int mLen;
    protected int hStart = -1;
    protected int hLen;

    @Override
    public String handlePart(Matcher matcher, String part, FormatType type, StringBuffer desc) {

        int pos = desc.length();
        char firstCh = part.charAt(0);
        switch (firstCh) {
            case 's':
            case 'S':
                if (mStart >= 0) {
                    for (int i = 0; i < mLen; i++) {
                        desc.setCharAt(mStart + i, 'm');
                    }
                    mStart = -1;
                }
                return part.toLowerCase(Locale.ROOT);

            case 'h':
            case 'H':
                mStart = -1;
                hStart = pos;
                hLen = part.length();
                return part.toLowerCase(Locale.ROOT);

            case 'd':
            case 'D':
                mStart = -1;
                if (part.length() <= 2) {
                    return part.toLowerCase(Locale.ROOT);
                } else {
                    return part.toLowerCase(Locale.ROOT).replace('d', 'E');
                }

            case 'm':
            case 'M':
                mStart = pos;
                mLen = part.length();
                // For 'm' after 'h', output minutes ('m') not month ('M')
                if (hStart >= 0) {
                    return part.toLowerCase(Locale.ROOT);
                } else {
                    return part.toUpperCase(Locale.ROOT);
                }

            case 'y':
            case 'Y':
                mStart = -1;
                if (part.length() == 3) {
                    part = "yyyy";
                }
                return part.toLowerCase(Locale.ROOT);

            case '0':
                mStart = -1;
                int sLen = part.length();
                sFmt = "%0" + (sLen + 2) + "." + sLen + "f";
                return part.replace('0', 'S');

            case 'a':
            case 'A':
            case 'p':
            case 'P':
                if (part.length() > 1) {
                    // am/pm marker
                    mStart = -1;
                    showAmPm = true;
                    showM = "m".equals(StringUtil.toLowerCase(part.charAt(1)));
                    // For some reason "am/pm" becomes AM or PM, but "a/p" becomes a or p
                    amPmUpper = showM || StringUtil.isUpperCase(part.charAt(0));

                    return "a";
                }
                //noinspection fallthrough

            default:
                return null;
        }
    }

    public void finish(StringBuffer toAppendTo) {
        if (hStart >= 0 && !showAmPm) {
            for (int i = 0; i < hLen; i++) {
                toAppendTo.setCharAt(hStart + i, 'H');
            }
        }
    }

    public boolean isAmPmUpper() {
        return amPmUpper;
    }

    public boolean isShowM() {
        return showM;
    }

    public boolean isShowAmPm() {
        return showAmPm;
    }

    public String getSecondsFormat() {
        return sFmt;
    }
}
