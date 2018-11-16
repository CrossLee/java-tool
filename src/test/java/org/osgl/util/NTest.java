package org.osgl.util;

/*-
 * #%L
 * Java Tool
 * %%
 * Copyright (C) 2014 - 2017 OSGL (Open Source General Library)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgl.TestBase;
import org.osgl.$;

/**
 * Created by luog on 8/06/2014.
 */
public class NTest extends TestBase {

    @Test
    public void testRandIntWithRandSymbol() {
        boolean hasNegative = false;
        boolean hasPositive = false;
        for (int i = 0; i < 10; ++i) {
            int n = N.randIntWithSymbol();
            if (n > 0) hasPositive = true;
            if (n < 0) hasNegative = true;
        }
        yes(hasNegative, "it shall has at least one negative number");
        yes(hasPositive, "it shall has at least one positive number");
    }

    @Ignore
    public static class FuncTestBase extends TestBase {

        protected int int1, int2;
        protected long long1, long2;
        protected float float1, float2;
        protected double double1, double2;

        @Before
        public void prepare() {
            int1 = N.randInt();
            int2 = N.randInt();
            long1 = N.randLong();
            long2 = N.randLong();
            float1 = N.randFloat();
            float2 = N.randFloat();
            double1 = N.randDouble();
            double2 = N.randDouble();
        }

        @Test
        public void testMultiplyTwoInt() {
            eq(int1 * int2, N.F.MULTIPLY.apply(int1, int2).intValue());
            eq(long1 * long2, N.F.MULTIPLY.apply(long1, long2).longValue());
            eq(float1 * float2, N.F.MULTIPLY.apply(float1, float2).floatValue());
            eq(double1 * double2, N.F.MULTIPLY.apply(double1, double2).doubleValue());
        }
    }

    public static class DivideTest extends FuncTestBase {
        private $.F2<Number, Number, Number> func = N.F.DIVIDE;

        @Test
        public void testIntByInt() {
            eq(int1 / int2, func.apply(int1, int2).intValue());
        }

        @Test
        public void testIntByLong() {
            eq(int1 / long1, func.apply(int1, long1).longValue());
        }

        @Test
        public void testIntByFloat() {
            eq(int1 / float1, func.apply(int1, float1).floatValue(), Math.abs(int1 / 10000 / float1));
        }

        @Test
        public void testIntByDouble() {
            eq(int1 / double1, func.apply(int1, double1).doubleValue());
        }

        @Test
        public void testLongByLong() {
            eq(long1 / long2, func.apply(long1, long2).longValue());
        }

        @Test
        public void testLongByInt() {
            eq(long1 / int1, func.apply(long1, int1).longValue());
        }

        @Test
        public void testLongByFloat() {
            eq(long1 / float1, func.apply(long1, float1).floatValue(), Math.abs(long1 / 100000 / float1));
        }

        @Test
        public void testLongByDouble() {
            eq(long1 / double1, func.apply(long1, double1).doubleValue());
        }

        @Test
        public void testFloatByFloat() {
            eq(float1 / float2, func.apply(float1, float2).floatValue());
        }

        @Test
        public void testFloatByInt() {
            yes((float1 / int1 - func.apply(float1, int1).floatValue()) < 0.0001);
        }

        @Test
        public void testFloatByLong() {
            yes((float1 / long1 - func.apply(float1, long1).floatValue()) < 0.0001);
        }

        @Test
        public void testFloatByDouble() {
            eq(float1 / double1, func.apply(float1, double1).doubleValue());
        }

        @Test
        public void testDoubleByDouble() {
            eq(double1 / double2, func.apply(double1, double2).doubleValue());
        }

        @Test
        public void testDoubleByInt() {
            eq(double1 / int1, func.apply(double1, int1).doubleValue());
        }

        @Test
        public void testDoubleByLong() {
            eq(double1 / long1, func.apply(double1, long1).doubleValue());
        }

        @Test
        public void testDoubleByFloat() {
            eq(double1 / float1, func.apply(double1, float1).doubleValue());
        }

        @Test
        public void testPerf() {
            long ts = System.currentTimeMillis();
            for (int i = 0; i < 1000 * 1000; ++i) {
                double n = double1 * double2;
                if (n < 0) {
                    System.out.println(n);
                }
            }
            long t1 = System.currentTimeMillis() - ts;
            ts = System.currentTimeMillis();
            for (int i = 0; i < 1000 * 1000; ++i) {
                int n = N.F.MULTIPLY.apply(double1, double2).intValue();
                if (n < 0) {
                    System.out.println(n);
                }
            }
            long t2 = System.currentTimeMillis() - ts;
            System.out.printf("t1: %s, t2: %s\n", t1, t2);
        }
    }

    public static class MultiplyTest extends FuncTestBase {
        private $.F2<Number, Number, Number> func = N.F.MULTIPLY;

        @Test
        public void testIntByInt() {
            eq(int1 * int2, func.apply(int1, int2).intValue());
        }

        @Test
        public void testIntByLong() {
            eq(int1 * long1, func.apply(int1, long1).longValue());
        }

        @Test
        public void testIntByFloat() {
            eq(int1 * float1, func.apply(int1, float1).floatValue());
        }

        @Test
        public void testIntByDouble() {
            eq(int1 * double1, func.apply(int1, double1).doubleValue());
        }

        @Test
        public void testLongByLong() {
            eq(long1 * long2, func.apply(long1, long2).longValue());
        }

        @Test
        public void testLongByInt() {
            eq(long1 * int1, func.apply(long1, int1).longValue());
        }

        @Test
        public void testLongByFloat() {
            eq(long1 * float1, func.apply(long1, float1).floatValue());
        }

        @Test
        public void testLongByDouble() {
            eq(long1 * double1, func.apply(long1, double1).doubleValue());
        }

        @Test
        public void testFloatByFloat() {
            eq(float1 * float2, func.apply(float1, float2).floatValue());
        }

        @Test
        public void testFloatByInt() {
            eq(float1 * int1, func.apply(float1, int1).floatValue());
        }

        @Test
        public void testFloatByLong() {
            eq(float1 * long1, func.apply(float1, long1).floatValue());
        }

        @Test
        public void testFloatByDouble() {
            eq(float1 * double1, func.apply(float1, double1).doubleValue());
        }

        @Test
        public void testDoubleByDouble() {
            eq(double1 * double2, func.apply(double1, double2).doubleValue());
        }

        @Test
        public void testDoubleByInt() {
            eq(double1 * int1, func.apply(double1, int1).doubleValue());
        }

        @Test
        public void testDoubleByLong() {
            eq(double1 * long1, func.apply(double1, long1).doubleValue());
        }

        @Test
        public void testDoubleByFloat() {
            eq(double1 * float1, func.apply(double1, float1).doubleValue());
        }

        @Test
        public void testPerf() {
            long ts = System.currentTimeMillis();
            for (int i = 0; i < 1000 * 1000; ++i) {
                double n = double1 * double2;
                if (n < 0) {
                    System.out.println(n);
                }
            }
            long t1 = System.currentTimeMillis() - ts;
            ts = System.currentTimeMillis();
            for (int i = 0; i < 1000 * 1000; ++i) {
                int n = N.F.MULTIPLY.apply(double1, double2).intValue();
                if (n < 0) {
                    System.out.println(n);
                }
            }
            long t2 = System.currentTimeMillis() - ts;
            System.out.printf("t1: %s, t2: %s\n", t1, t2);
        }
    }

    @Test
    public void testPerfectSquare() {
        for (int i = 0; i < 100; ++i) {
            long l = N.randInt(5000) + 13;
            long ll = l * l;
            yes(N.isPerfectSquare(ll));
            no(N.isPerfectSquare(ll - 1));
        }
    }

    @Test
    public void testIsInt() {
        yes(N.isInt("23423"));
        no(N.isInt("2Addf"));
        yes(N.isInt("2AddF", 16));
        no(N.isInt("2AddFG", 16));
        yes(N.isInt(String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void testPowOfTen() {
        for (int i = 0; i < 9; ++i) {
            eq((int)N.pow(10, i), N.powOfTen(i));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPowOfTenError1() {
        N.powOfTen(10);
    }

    @Test
    public void testPowerOfTenLong() {
        for (int i = 0; i < 19; ++i) {
            eq((long) N.pow(10, i), N.powOfTenLong(i));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPowerOfTenLongError1() {
        N.powOfTenLong(19);
    }

}
