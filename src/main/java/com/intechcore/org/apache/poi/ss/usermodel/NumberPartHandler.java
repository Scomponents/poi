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

import com.intechcore.scomponents.services.regexwrapper.MatcherAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Internal helper class for NumberFormatter
 */
public class NumberPartHandler implements PartHandler {
    public static final char DECIMAL_POINT = '.';

    protected char insertSignForExponent;
    protected double scale = 1;
    protected NumberFormatter.Special decimalPoint;
    protected NumberFormatter.Special slash;
    protected NumberFormatter.Special exponent;
    protected NumberFormatter.Special numerator;
    protected final List<NumberFormatter.Special> specials = new LinkedList<>();
    protected boolean improperFraction;

    private boolean startDigitsAfterSlash;
    private final StringBuilder digitsAfterSlashAcc = new StringBuilder();

    public String handlePart(MatcherAdapter m, String part, FormatType type, StringBuffer descBuf) {
        int pos = descBuf.length();
        char firstCh = part.charAt(0);
        switch (firstCh) {
            case 'e':
            case 'E':
                // See comment in writeScientific -- exponent handling is complex.
                // (1) When parsing the format, remove the sign from after the 'e' and
                // put it before the first digit of the exponent.
                if (exponent == null && !specials.isEmpty() && part.length() > 1) {
                    exponent = new NumberFormatter.Special('.', pos);
                    specials.add(exponent);
                    insertSignForExponent = part.charAt(1);
                    return part.substring(0, 1);
                }
                startDigitsAfterSlash = false;
                digitsAfterSlashAcc.setLength(0);
                break;

            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                if (slash != null) {
                    startDigitsAfterSlash = true;
                }
                if (startDigitsAfterSlash) {
                    digitsAfterSlashAcc.append(firstCh);
                    specials.add(new NumberFormatter.Special('0', pos));
                }
                break;

            case '0':
            case '?':
            case '#':
                if (insertSignForExponent != '\0') {
                    specials.add(new NumberFormatter.Special(insertSignForExponent, pos));
                    descBuf.append(insertSignForExponent);
                    insertSignForExponent = '\0';
                    pos++;
                }
                boolean addToAcc = firstCh == '0' && startDigitsAfterSlash;
                for (int i = 0; i < part.length(); i++) {
                    char ch = part.charAt(i);
                    specials.add(new NumberFormatter.Special(ch, pos + i));
                    if (addToAcc) {
                        digitsAfterSlashAcc.append(ch);
                    }
                }
                startDigitsAfterSlash = false;
                break;

            case DECIMAL_POINT:
                if (decimalPoint == null && !specials.isEmpty()) {
                    decimalPoint = new NumberFormatter.Special(DECIMAL_POINT, pos);
                    specials.add(decimalPoint);
                }
                startDigitsAfterSlash = false;
                digitsAfterSlashAcc.setLength(0);
                break;

            case '/':
                //!! This assumes there is a numerator and a denominator, but these are actually optional
                if (slash == null && !specials.isEmpty()) {
                    numerator = previousNumber();
                    // If the first number in the whole format is the numerator, the
                    // entire number should be printed as an improper fraction
                    improperFraction |= (numerator == firstDigit(specials));
                    slash = new NumberFormatter.Special('.', pos);
                    specials.add(slash);
                }
                startDigitsAfterSlash = false;
                digitsAfterSlashAcc.setLength(0);
                break;

            case '%':
                // don't need to remember because we don't need to do anything with these
                scale *= 100;
                startDigitsAfterSlash = false;
                digitsAfterSlashAcc.setLength(0);
                break;

            default:
                startDigitsAfterSlash = false;
                digitsAfterSlashAcc.setLength(0);
                return null;
        }
        return part;
    }

    public String getDigitsAfterSlashAcc() {
        return digitsAfterSlashAcc.toString();
    }

    public double getScale() {
        return scale;
    }

    public NumberFormatter.Special getDecimalPoint() {
        return decimalPoint;
    }

    public NumberFormatter.Special getSlash() {
        return slash;
    }

    public NumberFormatter.Special getExponent() {
        return exponent;
    }

    public NumberFormatter.Special getNumerator() {
        return numerator;
    }

    public List<NumberFormatter.Special> getSpecials() {
        return specials;
    }

    public boolean isImproperFraction() {
        return improperFraction;
    }

    protected NumberFormatter.Special previousNumber() {
        ListIterator<NumberFormatter.Special> it = specials.listIterator(specials.size());
        while (it.hasPrevious()) {
            NumberFormatter.Special s = it.previous();
            if (isDigitFmt(s)) {
                NumberFormatter.Special last = s;
                while (it.hasPrevious()) {
                    s = it.previous();
                    // it has to be continuous digits
                    if (last.pos - s.pos > 1 || !isDigitFmt(s)) {
                        break;
                    }
                    last = s;
                }
                return last;
            }
        }
        return null;
    }

    protected static boolean isDigitFmt(NumberFormatter.Special s) {
        return s.ch == '0' || s.ch == '?' || s.ch == '#';
    }

    protected static NumberFormatter.Special firstDigit(List<NumberFormatter.Special> specials) {
        for (NumberFormatter.Special s : specials) {
            if (isDigitFmt(s)) {
                return s;
            }
        }
        return null;
    }
}

