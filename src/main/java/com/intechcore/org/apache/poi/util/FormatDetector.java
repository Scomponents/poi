package com.intechcore.org.apache.poi.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intechcore.scomponents.services.ServiceContainer;
import com.intechcore.scomponents.services.format.DateUtil;
import com.intechcore.scomponents.services.format.IFormatService;
import com.intechcore.scomponents.services.format.IFormatServiceFactory;
import com.intechcore.scomponents.services.format.IValueFormatDetector;
import com.intechcore.scomponents.services.model.SerializableObject;
import com.intechcore.scomponents.services.regexwrapper.RegexAdapter;
import com.intechcore.scomponents.services.regexwrapper.RegexFactory;

import java.util.Locale;
import java.util.Optional;

public class FormatDetector implements IValueFormatDetector, SerializableObject {
    private static final ServiceContainer serviceContainer = ServiceContainer.getInstance();
    private static final IFormatServiceFactory formatServiceFactory = serviceContainer
            .resolve(IFormatServiceFactory.class);
    private static final RegexFactory regexFactory = serviceContainer.resolve(RegexFactory.class);

    private static final RegexAdapter percentagePattern = regexFactory.createFromPattern("0(\\.?0*)%");

    private final Locale locale;

    @JsonIgnore
    private final String formatCode;

    @JsonCreator
    public FormatDetector(@JsonProperty("formatCode") String formatCode, @JsonProperty("locale") Locale locale) {
        this.formatCode = formatCode;
        this.locale = locale;
    }

    @JsonProperty("formatCode")
    public String getFormatCode() {
        return this.formatCode;
    }

    @Override
    public boolean isTime() {
        return DateUtil.isTime(this.formatCode);
    }

    @Override
    public boolean isDateAndTime() {
        return DateUtil.isDateAndTime(this.formatCode);
    }

    @Override
    public boolean isDate() {
        return DateUtil.isDate(this.formatCode);
    }

    @Override
    public boolean isPercentage() {
        return percentagePattern.matcher(this.formatCode).matches();
    }

    @Override
    public boolean isGeneral() {
        return FormatHelper.isGeneralFormatCode(this.formatCode);
    }

    @Override
    public boolean isString() {
        return this.formatCode.equals(FormatHelper.TEXT_FORMAT);
    }

    @Override
    public Optional<IFormatService> getPoiServices() {
        return Optional.ofNullable(formatServiceFactory.create(this.locale, this.formatCode));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FormatDetector) {
            FormatDetector that = (FormatDetector) obj;
            return this.formatCode.equals(that.formatCode);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.formatCode.hashCode();
    }
}
