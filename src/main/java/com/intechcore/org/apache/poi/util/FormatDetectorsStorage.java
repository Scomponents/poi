package com.intechcore.org.apache.poi.util;

import com.intechcore.scomponents.services.ServiceContainer;
import com.intechcore.scomponents.services.collections.CollectionFactory;
import com.intechcore.scomponents.services.format.IValueFormatDetector;

import java.util.Locale;
import java.util.Map;

public final class FormatDetectorsStorage {
    private static final CollectionFactory collectionFactory = ServiceContainer.getInstance()
            .resolve(CollectionFactory.class);
    private static final Map<Locale, Map<String, IValueFormatDetector>>
            detectorsCache = collectionFactory.createWeakMap();

    public static final FormatDetectorsStorage Instance = new FormatDetectorsStorage();

    private FormatDetectorsStorage() {
    }

    public IValueFormatDetector getDetector(Locale locale, String format) {
        Map<String, IValueFormatDetector> detectorMap = detectorsCache
                .computeIfAbsent(locale, loc -> collectionFactory.createWeakMap());

        IValueFormatDetector result = detectorMap.get(format);
        if (result == null) {
            result = new FormatDetector(format, locale);
            detectorMap.put(format, result);
        }

        return result;
    }
}
