package com.intechcore.org.apache.poi.util;

public class FormatHelper {
    public static final String GENERAL_CODE = "General";
    public static final String TEXT_FORMAT = "@";

    /*
     * Cells that cannot be formatted, e.g. cells that have a date or time
     * format and have an invalid date or time value, are displayed as 255
     * pound signs ("#").
     */
    private static final String HASH_TAGS = "###################################################";
    public static final String INVALID_VALUE_FOR_FORMAT = HASH_TAGS + HASH_TAGS + HASH_TAGS + HASH_TAGS + HASH_TAGS;

    public static boolean isGeneralFormatCode(String format) {
        return format.equals(GENERAL_CODE) || TEXT_FORMAT.equals(format);
    }
}
