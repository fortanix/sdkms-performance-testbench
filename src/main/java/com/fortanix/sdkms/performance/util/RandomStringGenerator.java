/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RandomStringGenerator {

    public static final String CHARACTER_SET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final Map<Integer, Boolean> RADICES = new HashMap<Integer, Boolean>() {{
        put(10, true);
        put(36, true);
    }};
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public static String generateRandomString(String charSet, int length) {
        int charSetLength = charSet.length();
        if (charSetLength == 0) {
            throw new IllegalArgumentException("invalid character set");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(charSetLength);
            char character = charSet.charAt(index);
            result.append(character);
        }
        return result.toString();
    }

    public static String generateTweak(int radix, int length) {
        if (!RADICES.containsKey(radix)) {
            throw new IllegalArgumentException("invalid radix");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        return generateRandomString(CHARACTER_SET.substring(0, radix), length);
    }

    public static String generateData(int radix, int length) {
        if (!RADICES.containsKey(radix)) {
            throw new IllegalArgumentException("invalid radix");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        return generateRandomString(CHARACTER_SET.substring(0, radix), length);
    }
}
