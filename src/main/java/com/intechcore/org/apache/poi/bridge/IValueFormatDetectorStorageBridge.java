/*******************************************************************************
 *  Copyright (C) 2008-2022 Intechcore GmbH - All Rights Reserved
 *
 *  This file is part of SComponents project.
 *
 *  Unauthorized copying of this file, via any medium is strictly
 *  prohibited
 *  Proprietary and confidential
 *
 *  Written by Intechcore GmbH <info@intechcore.com>
 ******************************************************************************/
package com.intechcore.org.apache.poi.bridge;

import java.util.Locale;

public interface IValueFormatDetectorStorageBridge {
    IValueFormatDetectorBridge getDetectorBridge(Locale locale, String format);
}
