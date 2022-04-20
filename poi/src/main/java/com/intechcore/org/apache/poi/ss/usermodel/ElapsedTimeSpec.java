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

public class ElapsedTimeSpec {

    protected static final double HOUR_FACTOR = 1.0 / 24.0;
    protected static final double MIN_FACTOR = HOUR_FACTOR / 60.0;
    protected static final double SEC_FACTOR = MIN_FACTOR / 60.0;

    protected final char type;
    protected final int pos;
    protected final int len;
    protected final double factor;
    protected double modBy;

    public ElapsedTimeSpec(char type, int pos, int len) {
        this.type = type;
        this.pos = pos;
        this.len = len;
        this.factor = factorFor(type, len);
        modBy = 0;
    }

    public double factorFor(char type, int len) {
        switch (type) {
            case 'h':
                return HOUR_FACTOR;
            case 'm':
                return MIN_FACTOR;
            case 's':
                return SEC_FACTOR;
            case '0':
                return SEC_FACTOR / Math.pow(10, len);
            default:
                throw new IllegalArgumentException("Uknown elapsed time spec: " + type);
        }
    }

    public long valueFor(double elapsed) {
        double val;
        if (modBy == 0) {
            val = elapsed / factor;
        } else {
            val = elapsed / factor % modBy;
        }
        if (type == '0') {
            return Math.round(val);
        } else {
            return (long) val;
        }
    }

    public double modFor(char type, int len) {
        switch (type) {
            case 'h':
                return 24;
            case 'm':
            case 's':
                return 60;
            case '0':
                return Math.pow(10, len);
            default:
                throw new IllegalArgumentException(
                        "Uknown elapsed time spec: " + type);
        }
    }

    public char getType() {
        return type;
    }

    public int getLen() {
        return len;
    }


    public int getPos() {
        return pos;
    }

    public void updateMod() {
        this.modBy = modFor(type, len);
    }
}
