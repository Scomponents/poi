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

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import com.intechcore.org.apache.poi.bridge.BridgeContainer;
import com.intechcore.org.apache.poi.util.StringUtil;

public class DateFormatter extends ValueFormatter {
    protected final DateFormat dateFmt;
    protected final DatePartHandler partHandler;

    protected final LocalDateTime EXCEL_START_DATE = BridgeContainer.getStartDate1904();

    protected static /* final */ DateFormatter simpleDate;

    /**
     * Creates a new date formatter with the given specification.
     *
     * @param format The format.
     */
    public DateFormatter(String format) {
        this(Locale.getDefault(), format);
    }

    /**
     * Creates a new date formatter with the given specification.
     *
     * @param locale The locale.
     * @param format The format.
     */
    public DateFormatter(Locale locale, String format) {
        super(format);
        partHandler = new DatePartHandler();
        StringBuffer descBuf = FormatPart.parseFormat(format, FormatType.DATE, partHandler);
        partHandler.finish(descBuf);
        // tweak the format pattern to pass tests on JDK 1.7,
        // See https://issues.apache.org/bugzilla/show_bug.cgi?id=53369
        String ptrn = descBuf.toString().replaceAll("((y)(?!y))(?<!yy)", "yy");
        dateFmt = new SimpleDateFormat(ptrn, locale);
    }

    /** {@inheritDoc} */
    public synchronized void formatValue(StringBuffer toAppendTo, Object value) {
        if (value == null) {
            value = 0.0;
        }
        if (value instanceof Number) {
            Number num = (Number) value;
            long newValue = num.longValue();
            if (newValue == 0L) {
                value = EXCEL_START_DATE;
            } else {
                LocalDateTime dateTime = EXCEL_START_DATE.plusSeconds((newValue / 1000));
                dateTime = dateTime.plusNanos(newValue % 1_000_000);
                value = dateTime;
            }
        }

        AttributedCharacterIterator it = dateFmt.formatToCharacterIterator(toDate((LocalDateTime) value));
        boolean doneAm = false;
        boolean doneMillis = false;

        for (char ch = it.first();
             ch != CharacterIterator.DONE;
             ch = it.next()) {
            if (it.getAttribute(DateFormat.Field.MILLISECOND) != null) {
                if (!doneMillis) {
                    LocalDateTime dateObj = (LocalDateTime) value;
                    int pos = toAppendTo.length();
                    try (Formatter formatter = new Formatter(toAppendTo, Locale.ROOT)) {
                        long msecs = dateObj.getSecond() % 1000;
                        formatter.format(locale, partHandler.getSecondsFormat(), msecs / 1000.0);
                    }
                    toAppendTo.delete(pos, pos + 2);
                    doneMillis = true;
                }
            } else if (it.getAttribute(DateFormat.Field.AM_PM) != null) {
                if (!doneAm) {
                    if (partHandler.isShowAmPm()) {
                        if (partHandler.isAmPmUpper()) {
                            toAppendTo.append(StringUtil.toUpperCase(ch));
                            if (partHandler.isShowM()) {
                                toAppendTo.append('M');
                            }
                        } else {
                            toAppendTo.append(StringUtil.toLowerCase(ch));
                            if (partHandler.isShowM()) {
                                toAppendTo.append('m');
                            }
                        }
                    }
                    doneAm = true;
                }
            } else {
                toAppendTo.append(ch);
            }
        }
    }

    private Date toDate(LocalDateTime dateTime) {
        return new Date(dateTime.getYear() - 1900, dateTime.getMonthValue() - 1, dateTime.getDayOfMonth(),
                dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
    }

    /**
     * {@inheritDoc}
     * <p>
     * For a date, this is <tt>"mm/d/y"</tt>.
     */
    public void simpleValue(StringBuffer toAppendTo, Object value) {
        synchronized (DateFormatter.class) {
            if (simpleDate == null || !simpleDate.EXCEL_START_DATE.equals(EXCEL_START_DATE)) {
                simpleDate = new DateFormatter("mm/d/y");
            }
        }
        simpleDate.formatValue(toAppendTo, value);
    }
}
