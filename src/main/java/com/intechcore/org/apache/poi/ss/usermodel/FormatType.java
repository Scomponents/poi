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

import java.util.Locale;

/**
 * The different kinds of formats that the formatter understands.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public enum FormatType {

    /** The general (default) format; also used for <tt>"General"</tt>. */
    GENERAL {
        boolean isSpecial(char ch) {
            return false;
        }
        ValueFormatter formatter(String pattern) {
            return new GeneralFormatter();
        }
        ValueFormatter formatter(Locale locale, String pattern) {
            return new GeneralFormatter(locale);
        }
    },
    /** A numeric format. */
    NUMBER {
        boolean isSpecial(char ch) {
            return false;
        }
        ValueFormatter formatter(String pattern) {
            return new NumberFormatter(pattern);
        }
        ValueFormatter formatter(Locale locale, String pattern) {
            return new NumberFormatter(locale, pattern);
        }
    },
    /** A date format. */
    DATE {
        boolean isSpecial(char ch) {
            return ch == '\'' || (ch <= '\u007f' && Character.isLetter(ch));
        }
        ValueFormatter formatter(String pattern) {
            return new DateFormatter(pattern);
        }
        ValueFormatter formatter(Locale locale, String pattern) {
            return new DateFormatter(locale, pattern);
        }
    },
    /** An elapsed time format. */
    ELAPSED {
        boolean isSpecial(char ch) {
            return false;
        }
        ValueFormatter formatter(String pattern) {
            return new ElapsedTimeFormatter(pattern);
        }
        ValueFormatter formatter(Locale locale, String pattern) {
            return new ElapsedTimeFormatter(pattern);
        }
    },
    /** A text format. */
    TEXT {
        boolean isSpecial(char ch) {
            return false;
        }
        ValueFormatter formatter(String pattern) {
            return new TextFormatter(pattern);
        }
        ValueFormatter formatter(Locale locale, String pattern) {
            return new TextFormatter(pattern);
        }
    };

    /**
     * Returns <tt>true</tt> if the format is special and needs to be quoted.
     *
     * @param ch The character to test.
     *
     * @return <tt>true</tt> if the format is special and needs to be quoted.
     */
    abstract boolean isSpecial(char ch);

    /**
     * Returns a new formatter of the appropriate type, for the given pattern.
     * The pattern must be appropriate for the type.
     *
     * @param pattern The pattern to use.
     *
     * @return A new formatter of the appropriate type, for the given pattern.
     */
    abstract ValueFormatter formatter(String pattern);

    /**
     * Returns a new formatter of the appropriate type, for the given pattern.
     * The pattern must be appropriate for the type.
     *
     * @param locale The locale to use.
     * @param pattern The pattern to use.
     *
     * @return A new formatter of the appropriate type, for the given pattern.
     */
    abstract ValueFormatter formatter(Locale locale, String pattern);
}
