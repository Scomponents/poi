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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.intechcore.org.apache.poi.util.FormatHelper;
import com.zaxxer.sparsebits.SparseBitSet;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This class implements printing out a value using a number format.
 */
public class NumberFormatter extends ValueFormatter {

    private static final Logger LOG = LogManager.getLogger(NumberFormatter.class);

    private static final Pattern fractionStripper = Pattern.compile("(\"[^\"]*\")|([^ ?#\\d/]+)");
    private static final Pattern fractionPattern = Pattern.compile("(?:([#\\d]+)\\s+)?(#+)\\s*/\\s*([#\\d]+)");
    private static final Pattern onlyDigitPattern = Pattern.compile("^[0-9]+$");
    private static final Pattern onlySpacesPattern = Pattern.compile(" +");

    protected final String desc;
    protected final String printfFmt;
    protected final double scale;
    protected final Special decimalPoint;
    protected final Special slash;
    protected final Special exponent;
    protected final Special numerator;
    protected final Special afterInteger;
    protected final Special afterFractional;
    protected final boolean showGroupingSeparator;
    protected final List<Special> specials = new ArrayList<>();
    protected final List<Special> integerSpecials = new ArrayList<>();
    protected final List<Special> fractionalSpecials = new ArrayList<>();
    protected final List<Special> numeratorSpecials = new ArrayList<>();
    protected final List<Special> denominatorSpecials = new ArrayList<>();
    protected final List<Special> exponentSpecials = new ArrayList<>();
    protected final List<Special> exponentDigitSpecials = new ArrayList<>();
    protected final int maxDenominator;
    private boolean concreteDenominator;
    protected final String numeratorFmt;
    protected final String denominatorFmt;
    protected final boolean improperFraction;
    protected final DecimalFormat decimalFmt;
    private FractionFormat fractionFormat;
    private int scalesCount;

    // The NumberFormatter.simpleValue() method uses the SIMPLE_NUMBER
    // ValueFormatter defined here. The CellFormat.GENERAL_FORMAT CellFormat
    // no longer uses the SIMPLE_NUMBER ValueFormatter.
    // Note that the simpleValue()/SIMPLE_NUMBER ValueFormatter format
    // ("#" for integer values, and "#.#" for floating-point values) is
    // different from the 'General' format for numbers ("#" for integer
    // values and "#.#########" for floating-point values).
    protected final ValueFormatter simpleNumber = new GeneralNumberFormatter(locale);

    protected static class GeneralNumberFormatter extends ValueFormatter {
        protected GeneralNumberFormatter(Locale locale) {
            super(locale, FormatHelper.GENERAL_CODE);
        }

        public void formatValue(StringBuffer toAppendTo, Object value) {
            if (value == null) {
                return;
            }

            ValueFormatter cf;
            if (value instanceof Number) {
                Number num = (Number) value;
                cf = (num.doubleValue() % 1.0 == 0) ? new NumberFormatter(locale, "#") :
                        new NumberFormatter(locale, "#.#");
            } else {
                cf = TextFormatter.SIMPLE_TEXT;
            }
            cf.formatValue(toAppendTo, value);
        }

        public void simpleValue(StringBuffer toAppendTo, Object value) {
            formatValue(toAppendTo, value);
        }
    }

    /**
     * This class is used to mark where the special characters in the format
     * are, as opposed to the other characters that are simply printed.
     */
    /* package */
    static class Special {
        final char ch;
        int pos;

        Special(char ch, int pos) {
            this.ch = ch;
            this.pos = pos;
        }

        @Override
        public String toString() {
            return "'" + ch + "' @ " + pos;
        }
    }

    /**
     * Creates a new cell number formatter.
     *
     * @param format The format to parse.
     */
    public NumberFormatter(String format) {
        this(Locale.getDefault(), format);
    }

    /**
     * Creates a new cell number formatter.
     *
     * @param locale The locale to use.
     * @param format The format to parse.
     */
    public NumberFormatter(Locale locale, String format) {
        super(locale, format);

        NumberPartHandler ph = new NumberPartHandler();
        StringBuffer descBuf = FormatPart.parseFormat(format, FormatType.NUMBER, ph);

        exponent = ph.getExponent();
        specials.addAll(ph.getSpecials());
        improperFraction = ph.isImproperFraction();

        // These are inconsistent settings, so ditch 'em
        if ((ph.getDecimalPoint() != null || ph.getExponent() != null) && ph.getSlash() != null) {
            slash = null;
            numerator = null;
        } else {
            slash = ph.getSlash();
            numerator = ph.getNumerator();
        }

        final int precision = interpretPrecision(ph.getDecimalPoint(), specials);
        int fractionPartWidth = 0;
        if (ph.getDecimalPoint() != null) {
            fractionPartWidth = 1 + precision;
            if (precision == 0) {
                // This means the format has a ".", but that output should have no decimals after it.
                // We just stop treating it specially
                specials.remove(ph.getDecimalPoint());
                decimalPoint = null;
            } else {
                decimalPoint = ph.getDecimalPoint();
            }
        } else {
            decimalPoint = null;
        }

        if (decimalPoint != null) {
            afterInteger = decimalPoint;
        } else if (exponent != null) {
            afterInteger = exponent;
        } else if (numerator != null) {
            afterInteger = numerator;
        } else {
            afterInteger = null;
        }

        if (exponent != null) {
            afterFractional = exponent;
        } else if (numerator != null) {
            afterFractional = numerator;
        } else {
            afterFractional = null;
        }

        double[] scaleByRef = { ph.getScale() };
        int[] scalesCountRef = { 0 }; // TODO: we do not need ph.getScale() value ?
        showGroupingSeparator = interpretIntegerCommas(descBuf, specials, decimalPoint, integerEnd(), fractionalEnd(),
                scaleByRef, scalesCountRef);
        if (exponent == null) {
            scale = scaleByRef[0];
            this.scalesCount = scalesCountRef[0];
        } else {
            // in "e" formats,% and trailing commas have no scaling effect
            scale = 1;
        }

        if (precision != 0) {
            // TODO: if decimalPoint is null (-> index == -1), return the whole list?
            fractionalSpecials.addAll(specials.subList(specials.indexOf(decimalPoint) + 1, fractionalEnd()));
        }

        if (exponent != null) {
            int exponentPos = specials.indexOf(exponent);
            exponentSpecials.addAll(specialsFor(exponentPos, 2));
            exponentDigitSpecials.addAll(specialsFor(exponentPos + 2));
        }

        if (slash != null) {
            if (numerator != null) {
                numeratorSpecials.addAll(specialsFor(specials.indexOf(numerator)));
            }

            denominatorSpecials.addAll(specialsFor(specials.indexOf(slash) + 1));
            if (denominatorSpecials.isEmpty()) {
                // no denominator follows the slash, drop the fraction idea
                numeratorSpecials.clear();
                maxDenominator = 1;
                numeratorFmt = null;
                denominatorFmt = null;
            } else {
                String digitsAfterSlash = ph.getDigitsAfterSlashAcc();
                concreteDenominator = !digitsAfterSlash.isEmpty()
                        && onlyDigitPattern.matcher(digitsAfterSlash).matches();
                maxDenominator = concreteDenominator
                        ? Integer.parseInt(digitsAfterSlash)
                        : maxValue(denominatorSpecials);
                numeratorFmt = singleNumberFormat(numeratorSpecials);
                denominatorFmt = singleNumberFormat(denominatorSpecials);

                String wholePart = "#";
                String fractionPart = "#/##";

                String[] chunks = format.split(";");
                for (String currentChunk : chunks) {
                    String chunk = currentChunk.replace("?", "#");
                    Matcher matcher = fractionStripper.matcher(chunk);
                    chunk = matcher.replaceAll(" ");
                    chunk = onlySpacesPattern.matcher(chunk).replaceAll(" ");
                    Matcher fractionMatcher = fractionPattern.matcher(chunk);
                    if (fractionMatcher.find()) {
                        wholePart = (fractionMatcher.group(1) == null) ? "" : wholePart;
                        fractionPart = fractionMatcher.group(3);
                    }
                }

                this.fractionFormat = new FractionFormat(wholePart, fractionPart);
            }
        } else {
            maxDenominator = 1;
            numeratorFmt = null;
            denominatorFmt = null;
        }

        integerSpecials.addAll(specials.subList(0, integerEnd()));

        if (exponent == null) {
            int integerPartWidth = calculateIntegerPartWidth();
            int totalWidth = integerPartWidth + fractionPartWidth;

            // need to handle empty width specially as %00.0f fails during formatting
            // From POI 5.2.2
            if(totalWidth == 0) {
                printfFmt = "";
            } else {
                printfFmt = "%0" + totalWidth + '.' + precision + "f";
            }

            decimalFmt = null;
        } else {
            StringBuffer fmtBuf = new StringBuffer();
            boolean first = true;
            if (integerSpecials.size() == 1) {
                // If we don't do this, we get ".6e5" instead of "6e4"
                fmtBuf.append("0");
                first = false;
            } else {
                for (Special s : integerSpecials) {
                    if (isDigitFmt(s)) {
                        fmtBuf.append(first ? '#' : '0');
                        first = false;
                    }
                }
            }
            if (fractionalSpecials.size() > 0) {
                fmtBuf.append('.');
                for (Special s : fractionalSpecials) {
                    if (isDigitFmt(s)) {
                        if (!first) {
                            fmtBuf.append('0');
                        }
                        first = false;
                    }
                }
            }
            fmtBuf.append('E');
            placeZeros(fmtBuf, exponentSpecials.subList(2, exponentSpecials.size()));
            decimalFmt = new DecimalFormat(fmtBuf.toString(), getDecimalFormatSymbols());
            printfFmt = null;
        }

        desc = descBuf.toString();
    }

    @Override
    public int getNumberOfDecimalPlaces() {
        return fractionalSpecials.size();
    }

    @Override
    public String createNestedFormatWith(Integer decimalsCount, Boolean groupThousands, Boolean setMinus) {
        if (decimalsCount == null) {
            decimalsCount = this.getNumberOfDecimalPlaces();
        }

        if (decimalsCount < 0) {
            throw new IllegalArgumentException("Decimals count cannot be less than 0, actual: " + decimalsCount);
        }

        if (integerSpecials.isEmpty() && fractionalSpecials.isEmpty()) {
            return desc;
        }

        int lastInteger = integerSpecials.get(integerSpecials.size() - 1).pos;

        int decimalStart = decimalPoint == null ? lastInteger : decimalPoint.pos;

        int decimalEnd = fractionalSpecials.isEmpty()
                ? lastInteger
                : fractionalSpecials.get(fractionalSpecials.size() - 1).pos;

        if (decimalPoint == null && decimalStart == 0) {
            decimalStart = 1;
        }

        if (decimalStart == decimalEnd) {
            decimalStart++;
        }

        String partBeforeDecimal = desc.substring(0, decimalStart);
        String partAfterDecimal = desc.substring(decimalEnd + 1);

        int firstDecimalPos = integerSpecials.get(0).pos;

        boolean removeMinus = false;
        boolean addMinus = false;
        if (setMinus != null) {
            boolean minusAlreadySet = partBeforeDecimal.contains("-");
            removeMinus = !setMinus && minusAlreadySet;
            addMinus = !minusAlreadySet && setMinus;
        }

        if (groupThousands == null) {
            groupThousands = this.showGroupingSeparator;
        }

        if (groupThousands) {
            StringBuilder integerPartBuilder = new StringBuilder();
            integerSpecials.forEach(special -> integerPartBuilder.append(special.ch));

            while (integerPartBuilder.length() < 4) {
                integerPartBuilder.insert(0, '#');
            }

            final int THOUSANDS_DIGITS_COUNT = 3;

            int pos = integerPartBuilder.length() - THOUSANDS_DIGITS_COUNT;

            do {
                integerPartBuilder.insert(pos, ',');
                pos -= THOUSANDS_DIGITS_COUNT;
            } while (pos > 0);

            String startSymbols = desc.substring(0, firstDecimalPos);
            if (removeMinus) {
                startSymbols = startSymbols.replace("-", "");
            }
            if (addMinus) {
                integerPartBuilder.insert(0, "-");
            }
            integerPartBuilder.insert(0, startSymbols);
            partBeforeDecimal = integerPartBuilder.toString();
        } else {
            if (removeMinus) {
                partBeforeDecimal = partBeforeDecimal.replace("-", "");
            }
            if (addMinus) {
                String partBeforeDecimalStart = partBeforeDecimal.substring(0, firstDecimalPos);
                String partBeforeDecimalEnd = partBeforeDecimal.substring(firstDecimalPos);
                partBeforeDecimal = partBeforeDecimalStart + "-" + partBeforeDecimalEnd;
            }
        }

        StringBuilder scalesCommasBuilder = new StringBuilder();
        Stream.generate(() -> ',').limit(this.scalesCount).forEach(scalesCommasBuilder::append);

        if (decimalsCount == 0) {
            return partBeforeDecimal + scalesCommasBuilder.toString() + partAfterDecimal;
        }

        StringBuilder builder = new StringBuilder();

        builder.append(partBeforeDecimal);
        builder.append(decimalPoint != null ? decimalPoint.ch : NumberPartHandler.DECIMAL_POINT);

        Stream.generate(() -> '0').limit(decimalsCount).forEach(builder::append);
        builder.append(scalesCommasBuilder);

        if (partAfterDecimal.endsWith(" ") && this.format.endsWith("_)")) {
            partAfterDecimal = partAfterDecimal.substring(0, partAfterDecimal.length() - 1) + "_)";
        }

        builder.append(partAfterDecimal);

        return builder.toString();
    }

    protected DecimalFormatSymbols getDecimalFormatSymbols() {
        return DecimalFormatSymbols.getInstance(locale);
    }

    protected static void placeZeros(StringBuffer sb, List<Special> specials) {
        for (Special s : specials) {
            if (isDigitFmt(s)) {
                sb.append('0');
            }
        }
    }

    protected static NumberStringMod insertMod(Special special, CharSequence toAdd, int where) {
        return new NumberStringMod(special, toAdd, where);
    }

    protected static NumberStringMod deleteMod(Special start, boolean startInclusive, Special end,
                                               boolean endInclusive) {
        return new NumberStringMod(start, startInclusive, end, endInclusive);
    }

    protected static NumberStringMod replaceMod(Special start, boolean startInclusive, Special end,
                                                boolean endInclusive, char withChar) {
        return new NumberStringMod(start, startInclusive, end, endInclusive, withChar);
    }

    protected static String singleNumberFormat(List<Special> numSpecials) {
        return "%0" + numSpecials.size() + "d";
    }

    protected static int maxValue(List<Special> s) {
        return Math.toIntExact(Math.round(Math.pow(10, s.size()) - 1));
    }

    protected List<Special> specialsFor(int pos, int takeFirst) {
        if (pos >= specials.size()) {
            return Collections.emptyList();
        }
        ListIterator<Special> it = specials.listIterator(pos + takeFirst);
        Special last = it.next();
        int end = pos + takeFirst;
        while (it.hasNext()) {
            Special s = it.next();
            if (!isDigitFmt(s) || s.pos - last.pos > 1) {
                break;
            }
            end++;
            last = s;
        }
        return specials.subList(pos, end + 1);
    }

    protected List<Special> specialsFor(int pos) {
        return specialsFor(pos, 0);
    }

    protected static boolean isDigitFmt(Special s) {
        return s.ch == '0' || s.ch == '?' || s.ch == '#';
    }

    private int calculateIntegerPartWidth() {
        int digitCount = 0;
        for (Special s : specials) {
            //!! Handle fractions: The previous set of digits before that is the numerator,
            // so we should stop short of that
            if (s == afterInteger) {
                break;
            }

            if (isDigitFmt(s)) {
                digitCount++;
            }
        }
        return digitCount;
    }

    protected static int interpretPrecision(Special decimalPoint, List<Special> specials) {
        int idx = specials.indexOf(decimalPoint);
        int precision = 0;
        if (idx != -1) {
            // skip over the decimal point itself
            ListIterator<Special> it = specials.listIterator(idx+1);
            while (it.hasNext()) {
                Special s = it.next();
                if (!isDigitFmt(s)) {
                    break;
                }
                precision++;
            }
        }
        return precision;
    }

    protected static boolean interpretIntegerCommas (StringBuffer sb, List<Special> specials, Special decimalPoint,
                                                     int integerEnd, int fractionalEnd, double[] scale, int[] scales) {
        // In the integer part, commas at the end are scaling commas; other commas mean to show thousand-grouping commas
        ListIterator<Special> it = specials.listIterator(integerEnd);

        boolean stillScaling = true;
        boolean integerCommas = false;
        while (it.hasPrevious()) {
            Special s = it.previous();
            if (s.ch != ',') {
                stillScaling = false;
            } else {
                if (stillScaling) {
                    scale[0] /= 1000;
                    scales[0]++;
                } else {
                    integerCommas = true;
                }
            }
        }

        if (decimalPoint != null) {
            it = specials.listIterator(fractionalEnd);
            while (it.hasPrevious()) {
                Special s = it.previous();
                if (s.ch != ',') {
                    break;
                } else {
                    scale[0] /= 1000;
                    scales[0]++;
                }
            }
        }

        // Now strip them out -- we only need their interpretation, not their presence
        it = specials.listIterator();
        int removed = 0;
        while (it.hasNext()) {
            Special s = it.next();
            s.pos -= removed;
            if (s.ch == ',') {
                removed++;
                it.remove();
                sb.deleteCharAt(s.pos);
            }
        }

        return integerCommas;
    }

    protected int integerEnd() {
        return (afterInteger == null) ? specials.size() : specials.indexOf(afterInteger);
    }

    protected int fractionalEnd() {
        return (afterFractional == null) ? specials.size() : specials.indexOf(afterFractional);
    }

    /** {@inheritDoc} */
    public void formatValue(StringBuffer toAppendTo, Object valueObject) {

        if (this.fractionFormat != null) {
            toAppendTo.append(this.fractionFormat.format((Number) valueObject));
            return;
        }

        double value = ((Number) valueObject).doubleValue();
        value *= scale;

        // For negative numbers:
        // - If the cell format has a negative number format, this method
        // is called with a positive value and the number format has
        // the negative formatting required, e.g. minus sign or brackets.
        // - If the cell format does not have a negative number format,
        // this method is called with a negative value and the number is
        // formatted with a minus sign at the start.
        boolean negative = value < 0;
        if (negative) {
            value = -value;
        }

        // Split out the fractional part if we need to print a fraction
        double fractional = 0;
        if (slash != null) {
            if (improperFraction) {
                fractional = value;
                value = 0;
            } else {
                fractional = value % 1.0;
                //noinspection SillyAssignment
                value = (long) value;
            }
        }

        Set<NumberStringMod> mods = new TreeSet<>();
        StringBuffer output = new StringBuffer(localiseFormat(desc));

        if (exponent != null) {
            writeScientific(value, output, mods);
        } else if (improperFraction) {
            writeFraction(value, null, fractional, output, mods);
        } else {
            StringBuffer result = new StringBuffer();
            try (Formatter f = new Formatter(result, locale)) {
                f.format(locale, printfFmt, value);
            }

            if (numerator == null) {
                writeFractional(result, output);
                writeInteger(result, output, integerSpecials, mods, showGroupingSeparator);
            } else {
                writeFraction(value, result, fractional, output, mods);
            }
        }

        DecimalFormatSymbols dfs = getDecimalFormatSymbols();
        String groupingSeparator = Character.toString(dfs.getGroupingSeparator());

        // Now strip out any remaining '#'s and add any pending text ...
        Iterator<NumberStringMod> changes = mods.iterator();
        NumberStringMod nextChange = (changes.hasNext() ? changes.next() : null);
        // records chars already deleted
        SparseBitSet deletedChars = new SparseBitSet();
        int adjust = 0;
        for (Special s : specials) {
            int adjustedPos = s.pos + adjust;
            if (!deletedChars.get(s.pos) && output.charAt(adjustedPos) == '#') {
                output.deleteCharAt(adjustedPos);
                adjust--;
                deletedChars.set(s.pos);
            }
            while (nextChange != null && s == nextChange.getSpecial()) {
                int lenBefore = output.length();
                int modPos = s.pos + adjust;
                switch (nextChange.getOp()) {
                    case NumberStringMod.AFTER:
                        // ignore adding a comma after a deleted char (which was a '#')
                        if (nextChange.getToAdd().equals(groupingSeparator) && deletedChars.get(s.pos)) {
                            break;
                        }
                        output.insert(modPos + 1, nextChange.getToAdd());
                        break;
                    case NumberStringMod.BEFORE:
                        output.insert(modPos, nextChange.getToAdd());
                        break;

                    case NumberStringMod.REPLACE:
                        // delete starting pos in original coordinates
                        int delPos = s.pos;
                        if (!nextChange.isStartInclusive()) {
                            delPos++;
                            modPos++;
                        }

                        // Skip over anything already deleted
                        while (deletedChars.get(delPos)) {
                            delPos++;
                            modPos++;
                        }

                        // delete end point in original
                        int delEndPos = nextChange.getEnd().pos;
                        if (nextChange.isEndInclusive()) {
                            delEndPos++;
                        }

                        // delete end point in current
                        int modEndPos = delEndPos + adjust;

                        if (modPos < modEndPos) {
                            if ("".equals(nextChange.getToAdd())) {
                                output.delete(modPos, modEndPos);
                            }
                            else {
                                char fillCh = nextChange.getToAdd().charAt(0);
                                for (int i = modPos; i < modEndPos; i++) {
                                    output.setCharAt(i, fillCh);
                                }
                            }
                            deletedChars.set(delPos, delEndPos);
                        }
                        break;

                    default:
                        throw new IllegalStateException("Unknown op: " + nextChange.getOp());
                }
                adjust += output.length() - lenBefore;

                nextChange = (changes.hasNext()) ? changes.next() : null;
            }
        }

        // Finally, add it to the string
        if (negative) {
            toAppendTo.append('-');
        }
        toAppendTo.append(output);
    }

    protected void writeScientific(double value, StringBuffer output, Set<NumberStringMod> mods) {

        StringBuffer result = new StringBuffer();
        FieldPosition fractionPos = new FieldPosition(NumberFormat.FRACTION_FIELD);
        decimalFmt.format(value, result, fractionPos);
        writeInteger(result, output, integerSpecials, mods, showGroupingSeparator);
        writeFractional(result, output);

        /*
         * Exponent sign handling is complex.
         *
         * In DecimalFormat, you never put the sign in the format, and the sign only
         * comes out of the format if it is negative.
         *
         * In Excel, you always say whether to always show the sign ("e+") or only
         * show negative signs ("e-").
         *
         * Also in Excel, where you put the sign in the format is NOT where it comes
         * out in the result.  In the format, the sign goes with the "e"; in the
         * output it goes with the exponent value.  That is, if you say "#e-|#" you
         * get "1e|-5", not "1e-|5". This makes sense I suppose, but it complicates
         * things.
         *
         * Finally, everything else in this formatting code assumes that the base of
         * the result is the original format, and that starting from that situation,
         * the indexes of the original special characters can be used to place the new
         * characters.  As just described, this is not true for the exponent's sign.
         * <p>
         * So here is how we handle it:
         *
         * (1) When parsing the format, remove the sign from after the 'e' and put it
         * before the first digit of the exponent (where it will be shown).
         *
         * (2) Determine the result's sign.
         *
         * (3) If it's missing, put the sign into the output to keep the result
         * lined up with the output. (In the result, "after the 'e'" and "before the
         * first digit" are the same because the result has no extra chars to be in
         * the way.)
         *
         * (4) In the output, remove the sign if it should not be shown ("e-" was used
         * and the sign is negative) or set it to the correct value.
         */

        // (2) Determine the result's sign.
        int ePos = fractionPos.getEndIndex();
        int signPos = ePos + 1;
        char expSignRes = result.charAt(signPos);
        if (expSignRes != '-') {
            // not a sign, so it's a digit, and therefore a positive exponent
            expSignRes = '+';
            // (3) If it's missing, put the sign into the output to keep the result
            // lined up with the output.
            result.insert(signPos, '+');
        }

        // Now the result lines up like it is supposed to with the specials' indexes
        ListIterator<Special> it = exponentSpecials.listIterator(1);
        Special expSign = it.next();
        char expSignFmt = expSign.ch;

        // (4) In the output, remove the sign if it should not be shown or set it to
        // the correct value.
        if (expSignRes == '-' || expSignFmt == '+') {
            mods.add(replaceMod(expSign, true, expSign, true, expSignRes));
        } else {
            mods.add(deleteMod(expSign, true, expSign, true));
        }

        StringBuffer exponentNum = new StringBuffer(result.substring(signPos + 1));
        writeInteger(exponentNum, output, exponentDigitSpecials, mods, false);
    }

    @SuppressWarnings("unchecked")
    protected void writeFraction(double value, StringBuffer result,
                               double fractional, StringBuffer output, Set<NumberStringMod> mods) {

        // Figure out if we are to suppress either the integer or fractional part.
        // With # the suppressed part is removed; with ? it is replaced with spaces.
        if (!improperFraction) {
            // If fractional part is zero, and numerator doesn't have '0', write out
            // only the integer part and strip the rest.
            if (fractional == 0 && !hasChar('0', numeratorSpecials)) {
                writeInteger(result, output, integerSpecials, mods, false);

                Special start = lastSpecial(integerSpecials);
                Special end = lastSpecial(denominatorSpecials);
                if (hasChar('?', integerSpecials, numeratorSpecials, denominatorSpecials)) {
                    //if any format has '?', then replace the fraction with spaces
                    mods.add(replaceMod(start, false, end, true, ' '));
                } else {
                    // otherwise, remove the fraction
                    mods.add(deleteMod(start, false, end, true));
                }

                // That's all, just return
                return;
            } else {
                // New we check to see if we should remove the integer part
                boolean numNoZero = !hasChar('0', numeratorSpecials);
                boolean intNoZero = !hasChar('0', integerSpecials);
                boolean intOnlyHash = integerSpecials.isEmpty()
                        || (integerSpecials.size() == 1 && hasChar('#', integerSpecials));

                boolean removeBecauseZero     = fractional == 0 && (intOnlyHash || numNoZero);
                boolean removeBecauseFraction = fractional != 0 && intNoZero;

                if (value == 0 && (removeBecauseZero || removeBecauseFraction)) {
                    Special start = lastSpecial(integerSpecials);
                    boolean hasPlaceHolder = hasChar('?', integerSpecials, numeratorSpecials);
                    NumberStringMod sm = hasPlaceHolder
                            ? replaceMod(start, true, numerator, false, ' ')
                            : deleteMod(start, true, numerator, false);
                    mods.add(sm);
                } else {
                    // Not removing the integer part -- print it out
                    writeInteger(result, output, integerSpecials, mods, false);
                }
            }
        }

        // Calculate and print the actual fraction (improper or otherwise)
        try {
            int n;
            int d;
            // the "fractional % 1" captures integer values in improper fractions
            if (fractional == 0 || (improperFraction && fractional % 1 == 0)) {
                // 0 as a fraction is reported by excel as 0/1
                n = (int) Math.round(fractional);
                d = 1;
            } else {
                SimpleFraction frac;
                if (concreteDenominator) {
                    frac = SimpleFraction.buildFractionExactDenominator(fractional, maxDenominator);
                } else {
                    frac = SimpleFraction.buildFractionMaxDenominator(fractional, maxDenominator);
                }
                n = frac.getNumerator();
                d = frac.getDenominator();
            }
            if (improperFraction) {
                n += Math.round(value * d);
            }
            writeSingleInteger(numeratorFmt, n, output, numeratorSpecials, mods);
            writeSingleInteger(denominatorFmt, d, output, denominatorSpecials, mods);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    protected String localiseFormat(String format) {
        DecimalFormatSymbols dfs = getDecimalFormatSymbols();
        if(format.contains(",") && dfs.getGroupingSeparator() != ',') {
            if(format.contains(".") && dfs.getDecimalSeparator() != '.') {
                format = replaceLast(format, "\\.", "[DECIMAL_SEPARATOR]");
                format = format.replace(',', dfs.getGroupingSeparator())
                        .replace("[DECIMAL_SEPARATOR]", Character.toString(dfs.getDecimalSeparator()));
            } else {
                format = format.replace(',', dfs.getGroupingSeparator());
            }
        } else if(format.contains(".") && dfs.getDecimalSeparator() != '.') {
            format = format.replace('.', dfs.getDecimalSeparator());
        }
        return format;
    }


    protected static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }

    protected static boolean hasChar(char ch, List<Special>... numSpecials) {
        for (List<Special> specials : numSpecials) {
            for (Special s : specials) {
                if (s.ch == ch) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void writeSingleInteger(String fmt, int num, StringBuffer output, List<Special> numSpecials,
                                      Set<NumberStringMod> mods) {

        StringBuffer sb = new StringBuffer();
        try (Formatter formatter = new Formatter(sb, locale)) {
            formatter.format(locale, fmt, num);
        }
        writeInteger(sb, output, numSpecials, mods, false);
    }

    protected void writeInteger(StringBuffer result, StringBuffer output,
                              List<Special> numSpecials, Set<NumberStringMod> mods,
                              boolean showGroupingSeparator) {

        DecimalFormatSymbols dfs = getDecimalFormatSymbols();
        String decimalSeparator = Character.toString(dfs.getDecimalSeparator());
        String groupingSeparator = Character.toString(dfs.getGroupingSeparator());

        int pos = result.indexOf(decimalSeparator) - 1;
        if (pos < 0) {
            if (exponent != null && numSpecials == integerSpecials) {
                pos = result.indexOf("E") - 1;
            } else {
                pos = result.length() - 1;
            }
        }

        int strip;
        for (strip = 0; strip < pos; strip++) {
            char resultCh = result.charAt(strip);
            if (resultCh != '0' && resultCh != dfs.getGroupingSeparator()) {
                break;
            }
        }

        ListIterator<Special> it = numSpecials.listIterator(numSpecials.size());
        boolean followWithGroupingSeparator = false;
        Special lastOutputIntegerDigit = null;
        int digit = 0;
        while (it.hasPrevious()) {
            char resultCh;
            if (pos >= 0) {
                resultCh = result.charAt(pos);
            } else {
                // If result is shorter than field, pretend there are leading zeros
                resultCh = '0';
            }
            Special s = it.previous();
            followWithGroupingSeparator = showGroupingSeparator && digit > 0 && digit % 3 == 0;
            boolean zeroStrip = false;
            if (resultCh != '0' || s.ch == '0' || s.ch == '?' || pos >= strip) {
                zeroStrip = s.ch == '?' && pos < strip;
                output.setCharAt(s.pos, (zeroStrip ? ' ' : resultCh));
                lastOutputIntegerDigit = s;
            }
            if (followWithGroupingSeparator) {
                mods.add(insertMod(s, zeroStrip ? " " : groupingSeparator, NumberStringMod.AFTER));
                followWithGroupingSeparator = false;
            }
            digit++;
            --pos;
        }
        if (pos >= 0) {
            // We ran out of places to put digits before we ran out of digits; put this aside so we can add it later
            // pos was decremented at the end of the loop above when the iterator was at its end
            ++pos;
            StringBuffer extraLeadingDigits = new StringBuffer(result.substring(0, pos));
            if (showGroupingSeparator) {
                while (pos > 0) {
                    if (digit > 0 && digit % 3 == 0) {
                        extraLeadingDigits.insert(pos, groupingSeparator);
                    }
                    digit++;
                    --pos;
                }
            }
            mods.add(insertMod(lastOutputIntegerDigit, extraLeadingDigits, NumberStringMod.BEFORE));
        }
    }

    protected void writeFractional(StringBuffer result, StringBuffer output) {
        int digit;
        int strip;
        if (fractionalSpecials.size() > 0) {
            String decimalSeparator = Character.toString(getDecimalFormatSymbols().getDecimalSeparator());
            digit = result.indexOf(decimalSeparator) + 1;
            if (exponent != null) {
                strip = result.indexOf("e") - 1;
            } else {
                strip = result.length() - 1;
            }

            while (strip > digit && result.charAt(strip) == '0') {
                strip--;
            }

            for (Special s : fractionalSpecials) {
                char resultCh = result.charAt(digit);
                if (resultCh != '0' || s.ch == '0' || digit < strip) {
                    output.setCharAt(s.pos, resultCh);
                } else if (s.ch == '?') {
                    // This is when we're in trailing zeros, and the format is '?'.
                    // We still strip out remaining '#'s later
                    output.setCharAt(s.pos, ' ');
                }
                digit++;
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * For a number, this is <tt>"#"</tt> for integer values, and <tt>"#.#"</tt>
     * for floating-point values.
     */
    public void simpleValue(StringBuffer toAppendTo, Object value) {
        simpleNumber.formatValue(toAppendTo, value);
    }

    protected static Special lastSpecial(List<Special> s)  {
        return s.get(s.size() - 1);
    }
}
