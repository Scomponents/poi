package com.intechcore.org.apache.poi.ss.usermodel;

import com.intechcore.scomponents.helper.ARGB;

import java.util.Locale;
import java.util.Optional;

public class NestedFormatPartBuilder {
    private final FormatPart source;

    private ARGB color;
    private Integer decimalsCount;
    private Boolean setThousandsSeparator;
    private Boolean setMinus;
    private Boolean setBraces;

    public NestedFormatPartBuilder(FormatPart source) {
        this.source = source;
    }

    public String build() {
        String resultColor = Optional.ofNullable(this.source.colorString).orElse("");
        if (color != null) {
            if (ARGB.BLACK.equals(color)) {
                resultColor = "";
            } else {
                String newColor = FormatPart.getColorName(color).toLowerCase(Locale.ROOT);
                newColor = String.valueOf(newColor.charAt(0)).toUpperCase(Locale.ROOT) + newColor.substring(1);
                resultColor = "[" + newColor + "]";
            }
        }

        String resultCondition = Optional.ofNullable(this.source.conditionString).orElse("");

        String resultFormat = this.source.format.createNestedFormatWith(decimalsCount, setThousandsSeparator, setMinus);

        resultFormat = handleSetBrackets(resultFormat, setBraces);

        return resultColor + resultCondition + resultFormat;

    }

    public NestedFormatPartBuilder color(ARGB color) {
        this.color = color;
        return this;
    }

    public NestedFormatPartBuilder decimals(Integer decimalsCount) {
        this.decimalsCount = decimalsCount;
        return this;
    }

    public NestedFormatPartBuilder thousandsSeparator(Boolean set) {
        this.setThousandsSeparator = set;
        return this;
    }

    public NestedFormatPartBuilder minus(Boolean set) {
        this.setMinus = set;
        return this;
    }

    public NestedFormatPartBuilder braces(Boolean set) {
        this.setBraces = set;
        return this;
    }

    private static String handleSetBrackets(String resultFormat, Boolean setBraces) {
        if (setBraces == null) {
            return resultFormat;
        }

        boolean bracesAlreadyHere = (resultFormat.contains("\\(") && resultFormat.contains("\\)"))
                || (resultFormat.contains("\"(\"") && resultFormat.contains("\")\""))
                || (resultFormat.contains("(") && resultFormat.contains(")"));

        if (!setBraces && bracesAlreadyHere) {
            resultFormat = resultFormat
                    .replace("\\(", "")
                    .replace("\\)", "")
                    .replace("\")\"", "")
                    .replace("\"(\"", "")
                    .replace(")", "")
                    .replace("(", "");
        }
        if (setBraces && !bracesAlreadyHere) {
            resultFormat = "(" + resultFormat + ")";
        }

        return resultFormat;
    }
}
