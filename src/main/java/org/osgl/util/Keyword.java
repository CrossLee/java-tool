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

import org.osgl.$;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A `Keyword` can be presented in the different ways:
 * * CamelCaseStyle
 * * underscore_style
 * * CONSTANT_STYLE
 * * dash-style
 * * "readable style"
 * * "Http-Header-Style"
 *
 * When reading a string into a keyword, the following separator chars
 * will be ignored and used as separator to construct the keyword
 *
 * * space `' '`
 * * underscore: `_`
 * * dash: `-`
 * * comma: `,`
 * * dot: `.`
 * * colon: `:`
 * * semi-colon: `;`
 * * slash: `\`
 * * forward slash: `/`
 *
 */
public final class Keyword implements Comparable<Keyword> {

    public static final char SEP_SPACE = ' ';
    public static final char SEP_UNDERSCORE = '_';
    public static final char SEP_DASH = '-';
    public static final char SEP_COMMA = ',';
    public static final char SEP_COLON = ':';
    public static final char SEP_DOT = '.';
    public static final char SEP_SEMI_COLON = ';';
    public static final char SEP_SLASH = '\\';
    public static final char SEP_FORWARD_SLASH = '/';

    private static final char[] SEPS = {
            SEP_SPACE, SEP_UNDERSCORE, SEP_DASH, SEP_COMMA,
            SEP_COLON, SEP_DOT, SEP_SEMI_COLON, SEP_FORWARD_SLASH,
            SEP_SLASH
    };

    static {
        Arrays.sort(SEPS);
    }

    public enum Style {
        /**
         * `CamelCaseStyle`
         */
        CAMEL_CASE () {
            @Override
            protected CharSequence processToken(FastStr token, int seq) {
                return token.capFirst();
            }
        },

        JAVA_VARIABLE() {
            @Override
            protected CharSequence processToken(FastStr token, int seq) {
                return seq > 0 ? token.capFirst() : token;
            }
        },

        /**
         * `underscore_style`
         */
        UNDERSCORE(SEP_UNDERSCORE),

        /**
         * `CONSTANT_NAME_STYLE`
         */
        CONSTANT_NAME(SEP_UNDERSCORE) {
            @Override
            protected CharSequence processToken(FastStr token, int seq) {
                return token.toUpperCase();
            }
        },

        /**
         * `dashed-style`
         */
        DASHED(SEP_DASH),

        /**
         * `dotted.style`
         */
        DOTTED(SEP_DOT),

        /**
         * `Http-Header-Style`
         */
        HTTP_HEADER(SEP_DASH) {
            @Override
            protected CharSequence processToken(FastStr token, int seq) {
                return token.capFirst();
            }
        },

        /**
         * `Readable style`
         */
        READABLE(SEP_SPACE) {
            @Override
            protected CharSequence processToken(FastStr token, int seq) {
                if (seq == 0) {
                    return token.capFirst();
                }
                return token;
            }
        };

        private String separator;

        Style() {
            separator = null;
        }

        Style(char sep) {
            separator = String.valueOf(sep);
        }

        public String toString(Keyword keyword) {
            S.Buffer sb = S.buffer();
            int sz = keyword.list.size();
            for (int i = 0; i < sz; i++) {
                FastStr fs = keyword.list.get(i);
                sb.append(processToken(fs, i));
                if (i < sz - 1 && null != separator) {
                    sb.append(separator);
                }
            }
            return sb.toString();
        }

        public $.Transformer<String, String> asTransformer() {
            final Style me = this;
            return new $.Transformer<String, String>() {
                @Override
                public String transform(String s) {
                    return me.toString(Keyword.of(s));
                }
            };
        }

        protected CharSequence processToken(FastStr token, int seq) {
            return token;
        }
    }

    private C.List<FastStr> list = C.newList();

    public Keyword(CharSequence chars) {
        init(chars);
    }

    public String camelCase() {
        return Style.CAMEL_CASE.toString(this);
    }

    public String javaVariable() {
        return Style.JAVA_VARIABLE.toString(this);
    }

    public String constantName() {
        return Style.CONSTANT_NAME.toString(this);
    }

    public String underscore() {
        return Style.UNDERSCORE.toString(this);
    }

    /**
     * Alias of {@link #hyphenated()}
     * @return hyphen separated string
     */
    public String dashed() {
        return Style.DASHED.toString(this);
    }

    /**
     * Alias of {@link #dashed()}
     * @return hyphen separated string
     */
    public String hyphenated() {
        return dashed();
    }

    public String dotted() {
        return Style.DOTTED.toString(this);
    }

    public String httpHeader() {
        return Style.HTTP_HEADER.toString(this);
    }

    public String readable() {
        return Style.READABLE.toString(this);
    }

    public List<String> tokens() {
        List<String> list = new ArrayList<String>();
        for (FastStr fs : this.list) {
            list.add(fs.toString());
        }
        return list;
    }

    @Override
    public int hashCode() {
        return $.hc(list);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Keyword) {
            return ((Keyword) obj).list.equals(list);
        }
        return false;
    }

    /**
     * Returns string representation of this keyword using
     * {@link Style#UNDERSCORE underscore style}
     * @return the underscore style representation of this keyword
     */
    @Override
    public String toString() {
        return underscore();
    }

    /**
     * Return string representation of this keyword using style specified
     * @param style the style used to print this keyword
     * @return the printed string of this keyword by style specified
     */
    public String toString(Style style) {
        return style.toString(this);
    }

    @Override
    public int compareTo(Keyword o) {
        return camelCase().compareTo(o.camelCase());
    }

    /**
     * Create a `Keyword` for the given `chars`
     *
     * @param chars
     *      A `CharSequence`
     * @return a `Keyword` of the `chars`
     */
    public static Keyword of(CharSequence chars) {
        return new Keyword(chars);
    }

    /**
     * Check if two {@link CharSequence}s are keyword identical.
     *
     * This method is an alias of {@link #equals(CharSequence, CharSequence)}.
     *
     * @param a
     *      the first char sequence
     * @param b
     *      the second char sequence
     * @return `true` if `a` and `b` are keyword identical
     */
    public static boolean eq(CharSequence a, CharSequence b) {
        return of(a).equals(of(b));
    }

    /**
     * Check if two {@link CharSequence}s are not keyword identical.
     * @param a
     *      the first char sequence
     * @param b
     *      the second char sequence
     * @return `true` if `a` and `b` are not keyword identical
     */
    public static boolean neq(CharSequence a, CharSequence b) {
        return !eq(a, b);
    }

    /**
     * Check if two {@link CharSequence}s are keyword identical.
     *
     * This method is an alias of {@link #notEquals(CharSequence, CharSequence)}.
     *
     * @param a
     *      the first char sequence
     * @param b
     *      the second char sequence
     * @return `true` if `a` and `b` are keyword identical
     */
    public static boolean equals(CharSequence a, CharSequence b) {
        return eq(a, b);
    }

    /**
     * Check if two {@link CharSequence}s are not keyword identical.
     * @param a
     *      the first char sequence
     * @param b
     *      the second char sequence
     * @return `true` if `a` and `b` are not keyword identical
     */
    public static boolean notEquals(CharSequence a, CharSequence b) {
        return !eq(a, b);
    }

    private void init(CharSequence chars) {
        final FastStr fs = FastStr.of(chars);
        final int sz = fs.length();
        int last = nextNonSeparator(fs, 0);
        int pos;
        while (true) {
            pos = locateNextStop(fs, last);
            if (pos < 0 || pos == sz) {
                FastStr sub = fs.substr(last);
                if (!sub.isEmpty()) {
                    list.add(sub.toLowerCase());
                }
                break;
            }
            FastStr sub = fs.subSequence(last, pos);
            if (sub.length() == 1 && isUpperCase(sub.charAt(0))) {
                pos = nextNonUpperCase(fs, pos);
                sub = fs.subSequence(last, pos);
            }
            if (!sub.isEmpty()) {
                list.add(sub.toLowerCase());
            }
            last = nextNonSeparator(fs, pos);
        }
    }

    /*
     * next stop is at:
     * 1. Uppercase character that followed a non-uppercase character
     * 2. separator
     */
    private static int locateNextStop(FastStr str, int start) {
        int sz = str.length();
        if (start >= sz - 1) {
            return -1;
        }
        int pos = start + 1;
        while (pos < sz) {
            char ch = str.charAt(pos);
            if (isSeparator(ch) || isUpperCase(ch)) {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static int nextNonSeparator(FastStr str, int start) {
        int sz = str.length();
        int pos = start;
        while (pos < sz) {
            char ch = str.charAt(pos);
            if (isSeparator(ch)) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    private static int nextNonUpperCase(FastStr str, int start) {
        final int sz = str.size();
        int pos = start;
        while (pos < sz) {
            char ch = str.charAt(pos);
            if (isUpperCase(ch)) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    private static boolean isSeparator(char ch) {
        return Arrays.binarySearch(SEPS, ch) >= 0;
    }

    private static boolean isUpperCase(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }


}
