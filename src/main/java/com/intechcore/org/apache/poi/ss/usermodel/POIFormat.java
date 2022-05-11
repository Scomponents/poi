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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intechcore.org.apache.poi.bridge.BridgeContainer;
import com.intechcore.org.apache.poi.bridge.IValueFormatDetectorBridge;
import com.intechcore.org.apache.poi.bridge.PoiResult;
import com.intechcore.org.apache.poi.util.FormatHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format a value according to the standard Excel behavior.  This "standard" is
 * not explicitly documented by Microsoft, so the behavior is determined by
 * experimentation; see the tests.
 * <p>
 * An Excel format has up to four parts, separated by semicolons.  Each part
 * specifies what to do with particular kinds of values, depending on the number
 * of parts given:
 * <dl>
 * <dt>One part (example: <tt>[Green]#.##</tt>)</dt>
 * <dd>If the value is a number, display according to this one part (example: green text,
 * with up to two decimal points). If the value is text, display it as is.</dd>
 *
 * <dt>Two parts (example: <tt>[Green]#.##;[Red]#.##</tt>)</dt>
 * <dd>If the value is a positive number or zero, display according to the first part (example: green
 * text, with up to two decimal points); if it is a negative number, display
 * according to the second part (example: red text, with up to two decimal
 * points). If the value is text, display it as is.</dd>
 *
 * <dt>Three parts (example: <tt>[Green]#.##;[Black]#.##;[Red]#.##</tt>)</dt>
 * <dd>If the value is a positive
 * number, display according to the first part (example: green text, with up to
 * two decimal points); if it is zero, display according to the second part
 * (example: black text, with up to two decimal points); if it is a negative
 * number, display according to the third part (example: red text, with up to
 * two decimal points). If the value is text, display it as is.</dd>
 *
 * <dt>Four parts (example: <tt>[Green]#.##;[Black]#.##;[Red]#.##;[@]</tt>)</dt>
 * <dd>If the value is a positive number, display according to the first part (example: green text,
 * with up to two decimal points); if it is zero, display according to the
 * second part (example: black text, with up to two decimal points); if it is a
 * negative number, display according to the third part (example: red text, with
 * up to two decimal points). If the value is text, display according to the
 * fourth part (example: text in the cell's usual color, with the text value
 * surround by brackets).</dd>
 * </dl>
 * In addition to these, there is a general format that is used when no format
 * is specified.
 */
public class POIFormat implements Serializable {

    private final IValueFormatDetectorBridge formatDetector;

    protected final Locale locale;
    protected final FormatPart posNumFmt;
    protected final FormatPart zeroNumFmt;
    protected final FormatPart negNumFmt;
    protected final FormatPart textFmt;
    protected final int formatPartCount;

    private static final Logger LOG = LoggerFactory.getLogger(POIFormat.class);

    protected static final Pattern PARTS_DELIMITER = Pattern.compile(FormatPart.FORMAT_PAT.pattern() + "(;|$)",
            Pattern.CASE_INSENSITIVE);

    private final static String QUOTE = "\"";

    public POIFormat getFormatter() {
        return this;
    }

    public POIFormat getGenerator() {
        return this;
    }

    public POIFormat getExtraDetector() {
        return this;
    }

    public static class GeneralPOIFormat extends POIFormat {

        @JsonCreator
        public GeneralPOIFormat(@JsonProperty("locale") Locale locale) {
            super(locale, FormatHelper.GENERAL_CODE);
        }

        @Override
        public PoiResult apply(Object value) {
            String text = (new GeneralFormatter(this.locale)).format(value);
            return new PoiResult(text, null);
        }
    }

    private static final Map<Locale, Map<String, POIFormat>> formatCache = new WeakHashMap<>();

    /**
     * Returns a {@link POIFormat} that applies the given format.  Two calls
     * with the same format may or may not return the same object.
     *
     * @param format The format.
     *
     * @return A {@link POIFormat} that applies the given format.
     */
    public static POIFormat getInstance(String format) {
        return getInstance(Locale.getDefault(), format);
    }

    /**
     * Returns a {@link POIFormat} that applies the given format.  Two calls
     * with the same format may or may not return the same object.
     *
     * @param locale The locale.
     * @param format The format.
     *
     * @return A {@link POIFormat} that applies the given format.
     */
    @JsonCreator
    public static synchronized POIFormat getInstance(@JsonProperty("locale") Locale locale,
                                                     @JsonProperty("formatCode") String format) {
        Map<String, POIFormat> formatMap = formatCache.computeIfAbsent(locale, k -> new WeakHashMap<>());
        POIFormat result = formatMap.get(format);
        if (result == null) {
            IValueFormatDetectorBridge formatDetector = BridgeContainer.getDetectorStorage()
                    .getDetectorBridge(locale, format);
            if (formatDetector.isGeneral()) {
                result = new GeneralPOIFormat(locale);
            } else {
                result = new POIFormat(locale, format);
            }
            formatMap.put(format, result);
        }
        return result;
    }

    @JsonProperty("locale")
    public Locale getLocale() {
        return this.locale;
    }

    public IValueFormatDetectorBridge withDecimalPlaces(int decimalsCount) {
        return this.updateFormat(
                a -> new NestedFormatPartBuilder(a).decimals(decimalsCount).build(),
                false);
    }

    public IValueFormatDetectorBridge withNegativePart(Integer argbColor, Boolean setBraces, Boolean setMinus) {
        return this.updateFormat(
                a -> new NestedFormatPartBuilder(a).color(argbColor).braces(setBraces).minus(setMinus).build(),
                true);
    }

    public IValueFormatDetectorBridge setThousandsSeparator(boolean value) {
        return this.updateFormat(
                a -> new NestedFormatPartBuilder(a).thousandsSeparator(value).build(),
                false);
    }

    public boolean isNumeric() {
        boolean result = this.posNumFmt.type == FormatType.NUMBER;
        if (this.negNumFmt != null) {
            result = result && this.negNumFmt.type == FormatType.NUMBER;
        }

        if (this.zeroNumFmt != null) {
            result = result && this.zeroNumFmt.type == FormatType.NUMBER;
        }

        return result;
    }

    public String getCurrencySign() {
       if (!this.isNumeric()) {
           return null;
       }

       return this.posNumFmt.getCurrencySign();
    }

    private IValueFormatDetectorBridge updateFormat(Function<FormatPart, String> function, boolean negOnly) {
        Optional<String> posFormatPart = Optional.ofNullable(this.posNumFmt)
                .map(part -> negOnly ? part.toString() : function.apply(part));
        Optional<String> negFormatPart = Optional.ofNullable(this.negNumFmt).map(part -> ';' + function.apply(part));
        Optional<String> zeroFormatPart = Optional.ofNullable(this.zeroNumFmt)
                .map(part -> ';' + (negOnly ? part.toString() : function.apply(part)));
        Optional<String> textFormatPart = Optional.ofNullable(this.textFmt)
                .map(part -> negOnly ? part.toString() : function.apply(part));

        if (textFormatPart.isPresent() && textFormatPart.get().equals(FormatHelper.TEXT_FORMAT)) {
            textFormatPart = Optional.empty();
        }

        String result = posFormatPart.orElse("")
                + negFormatPart.orElse("")
                + zeroFormatPart.orElse("")
                + textFormatPart.map(a -> posFormatPart.isPresent() ? ';' + a : a).orElse("");

        return POIFormat.getInstance(this.locale, result).formatDetector;
    }

    private POIFormat(Locale locale, String formatCode) {
        this.locale = locale;
        this.formatDetector = BridgeContainer.getDetectorStorage().getDetectorBridge(locale, formatCode);

        Matcher formatPartsMatcher = PARTS_DELIMITER.matcher(formatCode);
        List<FormatPart> parts = new ArrayList<>();

        while (formatPartsMatcher.find()) {
            try {
                String valueDesc = formatPartsMatcher.group(0);

                // Strip out the semicolon if it's there
                if (valueDesc.endsWith(";")) {
                    valueDesc = valueDesc.substring(0, valueDesc.length() - 1);
                }

                parts.add(new FormatPart(locale, valueDesc));
            } catch (RuntimeException e) {
                LOG.warn("Invalid format: " + ValueFormatter.quote(formatPartsMatcher.group(0)), e);
                parts.add(null);
            }
        }

        FormatPart defaultTextFormat = new FormatPart(locale, FormatHelper.TEXT_FORMAT);

        this.formatPartCount = parts.size();

        switch (formatPartCount) {
            case 0:
                this.negNumFmt = null;
                this.zeroNumFmt = null;
                this.posNumFmt = null;
                this.textFmt = defaultTextFormat;
                break;
            case 1:
                this.posNumFmt = parts.get(0);
                this.negNumFmt = null;
                this.zeroNumFmt = null;
                this.textFmt = defaultTextFormat;
                break;
            case 2:
                this.posNumFmt = parts.get(0);
                this.negNumFmt = parts.get(1);
                this.zeroNumFmt = null;
                this.textFmt = defaultTextFormat;
                break;
            case 3:
                this.posNumFmt = parts.get(0);
                this.negNumFmt = parts.get(1);
                this.zeroNumFmt = parts.get(2);
                this.textFmt = defaultTextFormat;
                break;
            case 4:
            default:
                this.posNumFmt = parts.get(0);
                this.negNumFmt = parts.get(1);
                this.zeroNumFmt = parts.get(2);
                this.textFmt = parts.get(3);
                break;
        }
    }

    /**
     * Returns the result of applying the format to the given value.  If the
     * value is a number (a type of {@link Number} object), the correct number
     * format type is chosen; otherwise it is considered a text object.
     *
     * @param value The value
     *
     * @return The result, in a {@link PoiResult}.
     */
    public PoiResult apply(Object value) {
        if (value instanceof Number) {
            Number num = (Number) value;
            double val = num.doubleValue();
            if (val < 0 && ((formatPartCount == 2 && !posNumFmt.hasCondition() && !negNumFmt.hasCondition())
                    || (formatPartCount == 3 && !negNumFmt.hasCondition())
                    || (formatPartCount == 4 && !negNumFmt.hasCondition()))) {
                // The negative number format has the negative formatting required,
                // e.g. minus sign or brackets, so pass a positive value so that
                // the default leading minus sign is not also output
                return negNumFmt.apply(-val);
            } else {
                return getApplicableFormatPart(val).apply(val);
            }
        } else if (value instanceof LocalDate) {
            // Don't know (and can't get) the workbook date windowing (1900 or 1904)
            // so assume 1900 date windowing
            int numericValue = BridgeContainer.getDateTimeUtils().getSerialNumberFromDate((LocalDate) value);
            if (isValidExcelDate(numericValue)) {
                return getApplicableFormatPart(numericValue).apply(value);
            } else {
                throw new IllegalArgumentException(
                        "value " + numericValue + " of date " + value + " is not a valid Excel date");
            }
        } else if (value instanceof LocalDateTime) {
            // Don't know (and can't get) the workbook date windowing (1900 or 1904)
            // so assume 1900 date windowing
            double numericValue = BridgeContainer.getDateTimeUtils().getSerialNumberFromDateTime((LocalDateTime) value);
            return getApplicableFormatPart(numericValue).apply(value);
        } else {
            return textFmt.apply(value);
        }
    }

    private static boolean isValidExcelDate(double value) {
        return (value > -Double.MIN_VALUE);
    }

    public boolean isDate(double value) {
        return this.getApplicableFormatPart(value).getCellFormatType() == FormatType.DATE;
    }

    public int getNumberOfDecimalPlaces(Object value) {
        if (this.formatDetector.isGeneral()) {
            return 2;
        }

        FormatPart formatPart = this.getApplicableFormatPart(value);
        return formatPart.getNumberOfDecimalPlaces();
    }

    /**
     * Returns the {@link FormatPart} that applies to the value.  Result
     * depends on how many parts the cell format has, the cell value and any
     * conditions.  The value must be a {@link Number}.
     *
     * @param value The value.
     * @return The {@link FormatPart} that applies to the value.
     */
    private FormatPart getApplicableFormatPart(Object value) {

        if (value instanceof Number) {

            double val = ((Number) value).doubleValue();

            if (formatPartCount == 1) {
                if (posNumFmt != null && (!posNumFmt.hasCondition()
                        || (posNumFmt.hasCondition() && posNumFmt.applies(val)))) {
                    return posNumFmt;
                } else {
                    return new FormatPart(this.locale, "General");
                }
            } else if (formatPartCount == 2) {
                if (posNumFmt != null && ((!posNumFmt.hasCondition() && val >= 0)
                        || (posNumFmt.hasCondition() && posNumFmt.applies(val)))) {
                    return posNumFmt;
                } else if (!negNumFmt.hasCondition()
                        || (negNumFmt.hasCondition() && negNumFmt.applies(val))) {
                    return negNumFmt;
                } else {
                    // Return ###...### (255 #s) to match Excel 2007 behaviour
                    return new FormatPart(this.locale, QUOTE + FormatHelper.INVALID_VALUE_FOR_FORMAT + QUOTE);
                }
            } else {
                if ((!posNumFmt.hasCondition() && val > 0)
                        || (posNumFmt.hasCondition() && posNumFmt.applies(val))) {
                    return posNumFmt;
                } else if ((!negNumFmt.hasCondition() && val < 0)
                        || (negNumFmt.hasCondition() && negNumFmt.applies(val))) {
                    return negNumFmt;
                    // Only the first two format parts can have conditions
                } else {
                    return zeroNumFmt;
                }
            }
        } else {
            throw new IllegalArgumentException("value must be a Number");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof POIFormat) {
            POIFormat that = (POIFormat) obj;
            return this.formatDetector.equals(that.formatDetector);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.formatDetector.hashCode();
    }
}
