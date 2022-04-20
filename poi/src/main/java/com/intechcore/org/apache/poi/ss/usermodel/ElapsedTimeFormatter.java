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

import java.util.Formatter;
import java.util.ListIterator;
import java.util.Locale;

/**
 * This class implements printing out an elapsed time format.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public class ElapsedTimeFormatter extends ValueFormatter {
    protected final String printfFmt;
    protected final ElapsedTimePartHandler partHandler;

    /**
     * Creates a elapsed time formatter.
     *
     * @param pattern The pattern to parse.
     */
    public ElapsedTimeFormatter(String pattern) {
        super(pattern);

        partHandler = new ElapsedTimePartHandler();
        StringBuffer desc = FormatPart.parseFormat(pattern, FormatType.ELAPSED, partHandler);

        ListIterator<ElapsedTimeSpec> it = partHandler.getSpecs().listIterator(partHandler.getSpecs().size());
        while (it.hasPrevious()) {
            ElapsedTimeSpec spec = it.previous();
            desc.replace(spec.getPos(), spec.getPos() + spec.getLen(), "%0" + spec.getLen() + "d");
            if (spec.getType() != partHandler.getTopmost().getType()) {
                spec.updateMod();
            }
        }

        printfFmt = desc.toString();
    }



    /** {@inheritDoc} */
    public void formatValue(StringBuffer toAppendTo, Object value) {
        double elapsed = ((Number) value).doubleValue();

        if (elapsed < 0) {
            toAppendTo.append('-');
            elapsed = -elapsed;
        }

        Object[] parts = new Long[partHandler.getSpecs().size()];
        for (int i = 0; i < partHandler.getSpecs().size(); i++) {
            parts[i] = partHandler.getSpecs().get(i).valueFor(elapsed);
        }

        try (Formatter formatter = new Formatter(toAppendTo, Locale.ROOT)) {
            formatter.format(printfFmt, parts);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * For a date, this is <tt>"mm/d/y"</tt>.
     */
    public void simpleValue(StringBuffer toAppendTo, Object value) {
        formatValue(toAppendTo, value);
    }
}
