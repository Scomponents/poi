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

import com.intechcore.org.apache.poi.bridge.PoiResult;
import com.intechcore.org.apache.poi.util.StringCodepointsIterable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Objects of this class represent a single part of a cell format expression.
 * Each cell can have up to four of these for positive, zero, negative, and text
 * values.
 * <p>
 * Each format part can contain a color, a condition, and will always contain a
 * format specification.  For example <tt>"[Red][>=10]#"</tt> has a color
 * (<tt>[Red]</tt>), a condition (<tt>>=10</tt>) and a format specification
 * (<tt>#</tt>).
 * <p>
 * This class also contains patterns for matching the subparts of format
 * specification.  These are used internally, but are made public in case other
 * code has use for them.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public class FormatPart {
    private String currencySign;

    protected final Integer color;
    protected final ValueFormatter format;
    protected final FormatType type;

    static final Map<String, Integer> NAMED_COLORS;

    protected String colorString;
    protected FormatCondition condition;
    protected String conditionString;

    static {
        NAMED_COLORS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (PredefinedFormatColors color : PredefinedFormatColors.values()) {
            String name = color.name();
            int argbColor = color.getColor();
            NAMED_COLORS.put(name, argbColor);
            if (name.indexOf('_') > 0) {
                NAMED_COLORS.put(name.replace('_', ' '), argbColor);
            }
            if (name.indexOf("_PERCENT") > 0) {
                NAMED_COLORS.put(name.replace("_PERCENT", "%").replace('_',
                        ' '), argbColor);
            }
        }
    }

//    private static final RegexFactory regexFactory = ServiceContainer.getInstance().resolve(RegexFactory.class);

    /** Pattern for the color part of a cell format part. */
    public static final Pattern COLOR_PAT;
    /** Pattern for the condition part of a cell format part. */
    public static final Pattern CONDITION_PAT;
    /** Pattern for the format specification part of a cell format part. */
    public static final Pattern SPECIFICATION_PAT;
    /** Pattern for the currency symbol part of a cell format part */
    public static final Pattern CURRENCY_PAT;
    /** Pattern for an entire cell single part. */
    public static final Pattern FORMAT_PAT;

    /** Within {@link #FORMAT_PAT}, the group number for the matched color. */
    public static final int COLOR_GROUP;
    /**
     * Within {@link #FORMAT_PAT}, the group number for the operator in the
     * condition.
     */
    public static final int CONDITION_OPERATOR_GROUP;
    /**
     * Within {@link #FORMAT_PAT}, the group number for the value in the
     * condition.
     */
    public static final int CONDITION_VALUE_GROUP;
    /**
     * Within {@link #FORMAT_PAT}, the group number for the format
     * specification.
     */
    public static final int SPECIFICATION_GROUP;

    static {
        // A condition specification
        String condition = "([<>=]=?|!=|<>)"           + //   # The operator\n" +
//                "  \\s*([0-9]+(?:\\.[0-9]*)?)\\s*  # The constant to test against\n";
                            "\\s*([0-9]+(?:\\.[0-9]*)?)\\s*"; //  # The constant to test against

        // A currency symbol / string, in a specific locale
        String currency = "(\\[\\$.{0,3}(-[0-9a-f]{3,4})?])"; // FROM POI 5.2.2

        String color = "\\[(black|blue|cyan|green|magenta|red|white|yellow|color [0-9]+)\\]";

        // A number specification
        // Note: careful that in something like ##, that the trailing comma is not caught up in the integer part

        // A part of a specification
        String part = "\\\\."                 + // # Quoted single character\n" +
                "|\"([^\\\\\"]|\\\\.)*\""     + // # Quoted string of characters (handles escaped quotes like \\\")\n" +
                "|" + currency + ""           + // # Currency symbol in a given locale\n" +
                "|_."                         + // # Space as wide as a given character\n" +
                "|\\*."                       + // # Repeating fill character\n" +
                "|@"                          + // # Text: cell text\n" +
                "|([0?\\#](?:[0?\\#,]*))"     + // # Number: digit + other digits and commas\n" +
                "|e[-+]"                      + // # Number: Scientific: Exponent\n" +
                "|m{1,5}"                     + // # Date: month or minute spec\n" +
                "|d{1,4}"                     + // # Date: day/date spec\n" +
                "|y{2,4}"                     + // # Date: year spec\n" +
                "|h{1,2}"                     + // # Date: hour spec\n" +
                "|s{1,2}"                     + // # Date: second spec\n" +
                "|am?/pm?"                    + // # Date: am/pm spec\n" +
                "|\\[h{1,2}\\]"               + // # Elapsed time: hour spec\n" +
                "|\\[m{1,2}\\]"               + // # Elapsed time: minute spec\n" +
                "|\\[s{1,2}\\]"               + // # Elapsed time: second spec\n" +
                "|[^;]";                        // # A character\n" + "";

        String format = "(?:" + color + ")?"         + //   # Text color\n" +
                "(?:\\[" + condition + "\\])?"       + //   # Condition\n" +
                // see https://msdn.microsoft.com/en-ca/goglobal/bb964664.aspx
                // and https://bz.apache.org/ooo/show_bug.cgi?id=70003
                // we ignore these for now though
                "(?:\\[\\$-[0-9a-fA-F]+\\])?"        + //   # Optional locale id, ignored currently\n" +
                "((?:" + part + ")+)";                 //   # Format spec\n";

        int flags = Pattern.CASE_INSENSITIVE; // GWT has not the Pattern.COMMENTS flag
        COLOR_PAT = Pattern.compile(color, flags);
        CONDITION_PAT = Pattern.compile(condition, flags);
        SPECIFICATION_PAT = Pattern.compile(part, flags);
        CURRENCY_PAT = Pattern.compile(currency, flags);
        FORMAT_PAT = Pattern.compile(format, flags);

        // Calculate the group numbers of important groups.  (They shift around
        // when the pattern is changed; this way we figure out the numbers by
        // experimentation.)

        COLOR_GROUP = findGroup("[Blue]@", "Blue");
        CONDITION_OPERATOR_GROUP = findGroup("[>=1]@", ">=");
        CONDITION_VALUE_GROUP = findGroup("[>=1]@", "1");
        SPECIFICATION_GROUP = findGroup("[Blue][>1]\\a ?", "\\a ?");
    }

    /**
     * Create an object to represent a format part.
     *
     * @param locale The locale to use.
     * @param desc The string to parse.
     */
    public FormatPart(Locale locale, String desc) {
        Matcher matcher = FORMAT_PAT.matcher(desc);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unrecognized format: " + ValueFormatter.quote(desc));
        }
        Pair<Integer, String> colorData = getColorData(matcher);
        if (colorData != null) {
            this.color = colorData.getKey();
            this.colorString = colorData.getValue();
        } else {
            this.color = null;
        }

        this.condition = this.getCondition(matcher);
        this.type = this.getCellFormatType(matcher);
        this.format = this.getFormatter(locale, matcher);
    }

    /**
     * Returns <tt>true</tt> if this format part applies to the given value. If
     * the value is a number and this is part has a condition, returns
     * <tt>true</tt> only if the number passes the condition.  Otherwise, this
     * always return <tt>true</tt>.
     *
     * @param valueObject The value to evaluate.
     *
     * @return <tt>true</tt> if this format part applies to the given value.
     */
    public boolean applies(Object valueObject) {
        if (condition == null || !(valueObject instanceof Number)) {
            if (valueObject == null) {
                throw new NullPointerException("valueObject");
            }
            return true;
        } else {
            Number num = (Number) valueObject;
            return condition.pass(num.doubleValue());
        }
    }

    /**
     * Returns the number of the first group that is the same as the marker
     * string. Starts from group 1.
     *
     * @param str    The string to match against the pattern.
     * @param marker The marker value to find the group of.
     *
     * @return The matching group number.
     *
     * @throws IllegalArgumentException No group matches the marker.
     */
    private static int findGroup(String str, String marker) {
        Matcher matcher = FORMAT_PAT.matcher(str);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Pattern \"" + FORMAT_PAT.pattern() + "\" doesn't match \"" + str + "\"");
        }

        for (int i = 1; i <= matcher.groupCount(); i++) {
            String grp = matcher.group(i);
            if (grp != null && grp.equals(marker)) {
                return i;
            }
        }

        throw new IllegalArgumentException("\"" + marker + "\" not found in \"" + FORMAT_PAT.pattern() + "\"");
    }

    /**
     * Returns the color specification from the matcher, or <tt>null</tt> if
     * there is none.
     *
     * @param matcher The matcher for the format part.
     *
     * @return The color specification or <tt>null</tt>.
     */
    private static Pair<Integer, String> getColorData(Matcher matcher) {
        String cdesc = matcher.group(COLOR_GROUP);
        if (cdesc == null || cdesc.length() == 0) {
            return null;
        }
        Integer newColor = NAMED_COLORS.get(cdesc);
        if (newColor == null) {
            ValueFormatter.logger.warning("Unknown color: " + ValueFormatter.quote(cdesc));
        }

        return new ImmutablePair<>(newColor, '[' + cdesc + ']');
    }

    public static String getColorName(Integer color) {
        return NAMED_COLORS.entrySet().stream().filter(entry -> entry.getValue().equals(color)).findFirst()
                .map(Map.Entry::getKey).orElse("");
    }

    /**
     * Returns the condition specification from the matcher, or <tt>null</tt> if
     * there is none.
     *
     * @param matcher The matcher for the format part.
     *
     * @return The condition specification or <tt>null</tt>.
     */
    private FormatCondition getCondition(Matcher matcher) {
        String operator = matcher.group(CONDITION_OPERATOR_GROUP);
        if (operator == null || operator.length() == 0) {
            return null;
        }

        String value = matcher.group(CONDITION_VALUE_GROUP);

        conditionString = '[' + operator + value + ']';
        return FormatCondition.getInstance(operator, value);
    }

    /**
     * Returns the FormatType object implied by the format specification for
     * the format part.
     *
     * @param matcher The matcher for the format part.
     *
     * @return The FormatType.
     */
    private FormatType getCellFormatType(Matcher matcher) {
        String fdesc = matcher.group(SPECIFICATION_GROUP);
        return this.formatType(fdesc);
    }

    /**
     * Returns the formatter object implied by the format specification for the
     * format part.
     *
     * @param matcher The matcher for the format part.
     *
     * @return The formatter.
     */
    private ValueFormatter getFormatter(Locale locale, Matcher matcher) {
        String fdesc = matcher.group(SPECIFICATION_GROUP);

        // For now, we don't support localised currencies, so simplify if there
        Matcher currencyM = CURRENCY_PAT.matcher(fdesc);
        if (currencyM.find()) {
            String currencyPart = currencyM.group(1);
            String currencyRepl = null;
            if (currencyPart.startsWith("[$-")) {
                // Default $ in a different locale
                currencyRepl = "$";
            } else if (!currencyPart.contains("-")) { // From POI 5.2.2
                // Accounting formats such as USD [$USD]
                currencyRepl = currencyPart.substring(2, currencyPart.indexOf("]"));
            } else {
                currencyRepl = currencyPart.substring(2, currencyPart.lastIndexOf('-'));
            }

            this.currencySign = currencyRepl;
            fdesc = fdesc.replace(currencyPart, currencyRepl);
        }

        // Build a formatter for this simplified string
        return this.type.formatter(locale, fdesc);
    }

    public String getCurrencySign() {
        return this.currencySign;
    }

    /**
     * Returns the type of format.
     *
     * @param fdesc The format specification
     *
     * @return The type of format.
     */
    protected FormatType formatType(String fdesc) {
        fdesc = fdesc.trim();
        if (fdesc.isEmpty() || "General".equalsIgnoreCase(fdesc)) {
            return FormatType.GENERAL;
        }

        Matcher matcher = SPECIFICATION_PAT.matcher(fdesc);
        boolean couldBeDate = false;
        boolean seenZero = false;
        while (matcher.find()) {
            String repl = matcher.group(0);
            Iterator<String> codePoints = new StringCodepointsIterable(repl).iterator();
            if (codePoints.hasNext()) {
                String c1 = codePoints.next();
                String c2 = null;
                if (codePoints.hasNext()) {
                    c2 = codePoints.next().toLowerCase(Locale.ROOT);
                }

                switch (c1) {
                    case "@":
                        return FormatType.TEXT;
                    case "d":
                    case "D":
                    case "y":
                    case "Y":
                        return FormatType.DATE;
                    case "h":
                    case "H":
                    case "m":
                    case "M":
                    case "s":
                    case "S":
                        // These can be part of date, or elapsed
                        couldBeDate = true;
                        break;
                    case "0":
                        // This can be part of date, elapsed, or number
                        seenZero = true;
                        break;
                    case "[":
                        if ("h".equals(c2) || "m".equals(c2) || "s".equals(c2)) {
                            return FormatType.ELAPSED;
                        }
                        if ("$".equals(c2)) {
                            // Localised currency
                            return FormatType.NUMBER;
                        } else {
                            return FormatType.GENERAL;
                        }
                    case "#":
                    case "?":
                        return FormatType.NUMBER;
                    default:
                        break;
                }
            }
        }

        // Nothing definitive was found, so we figure out it deductively
        if (couldBeDate) {
            return FormatType.DATE;
        }
        if (seenZero) {
            return FormatType.NUMBER;
        }
        return FormatType.TEXT;
    }

    /**
     * Returns a version of the original string that has any special characters
     * quoted (or escaped) as appropriate for the cell format type.  The format
     * type object is queried to see what is special.
     *
     * @param repl The original string.
     * @param type The format type representation object.
     *
     * @return A version of the string with any special characters replaced.
     *
     * @see FormatType#isSpecial(char)
     */
    static String quoteSpecial(String repl, FormatType type) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> codePoints = new StringCodepointsIterable(repl).iterator();
        while (codePoints.hasNext()) {
            String ch = codePoints.next();
            if ("\'".equals(ch) && type.isSpecial('\'')) {
                sb.append('\u0000');
                continue;
            }

            boolean special = type.isSpecial(ch.charAt(0));
            if (special) {
                sb.append("\'");
            }
            sb.append(ch);
            if (special) {
                sb.append("\'");
            }
        }
        return sb.toString();
    }

    /**
     * Apply this format part to the given value.  This returns a {@link
     * PoiResult} object with the results.
     *
     * @param value The value to apply this format part to.
     *
     * @return A {@link PoiResult} object containing the results of
     *         applying the format to the value.
     */
    public PoiResult apply(Object value) {
        boolean applies = applies(value);
        String text;
        Integer textColor;
        if (applies) {
            text = format.format(value);
            textColor = color;
        } else {
            text = format.simpleFormat(value);
            textColor = null;
        }
        return new PoiResult(text, textColor);
    }

    public ValueFormatter getFormat() {
        return format;
    }

    public int getNumberOfDecimalPlaces() {
        return format.getNumberOfDecimalPlaces();
    }

    /**
     * Returns the FormatType object implied by the format specification for
     * the format part.
     *
     * @return The FormatType.
     */
    public FormatType getCellFormatType() {
        return type;
    }

    /**
     * Returns <tt>true</tt> if this format part has a condition.
     *
     * @return <tt>true</tt> if this format part has a condition.
     */
    boolean hasCondition() {
        return condition != null;
    }

    public static StringBuffer parseFormat(String fdesc, FormatType type, PartHandler partHandler) {

        // Quoting is very awkward.  In the Java classes, quoting is done
        // between ' chars, with '' meaning a single ' char. The problem is that
        // in Excel, it is legal to have two adjacent escaped strings.  For
        // example, consider the Excel format "\a\b#".  The naive (and easy)
        // translation into Java DecimalFormat is "'a''b'#".  For the number 17,
        // in Excel you would get "ab17", but in Java it would be "a'b17" -- the
        // '' is in the middle of the quoted string in Java.  So the trick we
        // use is this: When we encounter a ' char in the Excel format, we
        // output a \u0000 char into the string.  Now we know that any '' in the
        // output is the result of two adjacent escaped strings.  So after the
        // main loop, we have to do two passes: One to eliminate any ''
        // sequences, to make "'a''b'" become "'ab'", and another to replace any
        // \u0000 with '' to mean a quote char.  Oy.
        //
        // For formats that don't use "'" we don't do any of this
        Matcher matcher = SPECIFICATION_PAT.matcher(fdesc);
        StringBuffer fmt = new StringBuffer();
        while (matcher.find()) {
            String part = group(matcher, 0);

            // we need it for EUR accounting format
            if ("E".equalsIgnoreCase(part)) {
                int curCharPos = matcher.start();
                boolean cont = false;

                if (curCharPos - 1 > 0) {
                    char prev = fdesc.charAt(curCharPos - 1);
                    cont = prev == ' ';
                }

                //TODO(butthurter): continue if letters after is not numeric

                if (cont) {
                    matcher.appendReplacement(fmt, Matcher.quoteReplacement(part));
                    continue;
                }
            }

            if (part.length() > 0) {
                String repl = partHandler.handlePart(matcher, part, type, fmt);
                if (repl == null) {
                    switch (part.charAt(0)) {
                        case '\"':
                            repl = quoteSpecial(part.substring(1, part.length() - 1), type);
                            break;
                        case '\\':
                            repl = quoteSpecial(part.substring(1), type);
                            break;
                        case '_':
                            repl = " ";
                            break;
                        case '*': //!! We don't do this for real, we just put in 3 of them
                            repl = expandChar(part);
                            break;
                        default:
                            repl = part;
                            break;
                    }
                }
                matcher.appendReplacement(fmt, Matcher.quoteReplacement(repl));
            }
        }
        matcher.appendTail(fmt);

        if (type.isSpecial('\'')) {
            // Now the next pass for quoted characters: Remove '' chars, making "'a''b'" into "'ab'"
            int pos = 0;
            while ((pos = fmt.indexOf("''", pos)) >= 0) {
                fmt.delete(pos, pos + 2);
            }

            // Now the final pass for quoted chars: Replace any \u0000 with ''
            pos = 0;
            while ((pos = fmt.indexOf("\u0000", pos)) >= 0) {
                fmt.replace(pos, pos + 1, "''");
            }
        }

        return fmt;
    }

    /**
     * Expands a character. This is only partly done, because we don't have the
     * correct info.  In Excel, this would be expanded to fill the rest of the
     * cell, but we don't know, in general, what the "rest of the cell" is.
     *
     * @param part The character to be repeated is the second character in this
     *             string.
     *
     * @return The character repeated three times.
     */
    static String expandChar(String part) {
        List<String> codePoints = new ArrayList<>();
        new StringCodepointsIterable(part).iterator().forEachRemaining(codePoints::add);
        if (codePoints.size() < 2){
            throw new IllegalArgumentException("Expected part string to have at least 2 chars");
        }
        String ch = codePoints.get(1);
        return ch + ch + ch;
    }

    /**
     * Returns the string from the group, or <tt>""</tt> if the group is
     * <tt>null</tt>.
     *
     * @param matcher The matcher.
     * @param group The group number.
     *
     * @return The group or <tt>""</tt>.
     */
    public static String group(Matcher matcher, int group) {
        String str = matcher.group(group);
        return (str == null ? "" : str);
    }

    public String toString() {
        return format.format;
    }
}
