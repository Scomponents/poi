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

import java.util.HashMap;
import java.util.Map;

/**
 * This object represents a condition in a cell format.
 *
 * @author Ken Arnold, Industrious Media LLC
 */
public abstract class FormatCondition {
    protected static final int LT = 0;
    protected static final int LE = 1;
    protected static final int GT = 2;
    protected static final int GE = 3;
    protected static final int EQ = 4;
    protected static final int NE = 5;

    protected static final Map<String, Integer> TESTS;

    static {
        TESTS = new HashMap<>();
        TESTS.put("<", LT);
        TESTS.put("<=", LE);
        TESTS.put(">", GT);
        TESTS.put(">=", GE);
        TESTS.put("=", EQ);
        TESTS.put("==", EQ);
        TESTS.put("!=", NE);
        TESTS.put("<>", NE);
    }

    /**
     * Returns an instance of a condition object.
     *
     * @param opString The operator as a string.  One of <tt>"&lt;"</tt>,
     *                 <tt>"&lt;="</tt>, <tt>">"</tt>, <tt>">="</tt>,
     *                 <tt>"="</tt>, <tt>"=="</tt>, <tt>"!="</tt>, or
     *                 <tt>"&lt;>"</tt>.
     * @param constStr The constant (such as <tt>"12"</tt>).
     *
     * @return A condition object for the given condition.
     */
    public static FormatCondition getInstance(String opString,
                                                                           String constStr) {

        if (!TESTS.containsKey(opString)) {
            throw new IllegalArgumentException("Unknown test: " + opString);
        }
        int test = TESTS.get(opString);

        final double c = Double.parseDouble(constStr);

        switch (test) {
            case LT:
                return new FormatCondition() {
                    public boolean pass(double value) {
                        return value < c;
                    }
                };
            case LE:
                return new FormatCondition() {
                    public boolean pass(double value) {
                        return value <= c;
                    }
                };
            case GT:
                return new FormatCondition() {
                    public boolean pass(double value) {
                        return value > c;
                    }
                };
            case GE:
                return new FormatCondition() {
                    public boolean pass(double value) {
                        return value >= c;
                    }
                };
            case EQ:
                return new FormatCondition() {
                    public boolean pass(double value) {
                        return value == c;
                    }
                };
            case NE:
                return new FormatCondition() {
                    public boolean pass(double value) {
                        return !(value != c);
                    }
                };
            default:
                throw new IllegalArgumentException(
                        "Cannot create for test number " + test + "(\"" + opString +
                                "\")");
        }
    }

    /**
     * Returns <tt>true</tt> if the given value passes the constraint's test.
     *
     * @param value The value to compare against.
     *
     * @return <tt>true</tt> if the given value passes the constraint's test.
     */
    public abstract boolean pass(double value);
}
