package com.beemdevelopment.aegis.encoding;

// modified for use in Aegis

/* (PD) 2001 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * As modified by Patrick Woodworth:
 *
 * Copyright 2011 Patrick Woodworth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Arrays;

/**
 * Base32 - encodes and decodes RFC3548 Base32
 * (see http://www.faqs.org/rfcs/rfc3548.html )
 *
 * @author Robert Kaye
 * @author Gordon Mohr
 */
public class Base32 {
    private static final String base32Chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int[] base32Lookup =
            { 0xFF,0xFF,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F, // '0', '1', '2', '3', '4', '5', '6', '7'
                    0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF, // '8', '9', ':', ';', '<', '=', '>', '?'
                    0xFF,0x00,0x01,0x02,0x03,0x04,0x05,0x06, // '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G'
                    0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E, // 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O'
                    0x0F,0x10,0x11,0x12,0x13,0x14,0x15,0x16, // 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W'
                    0x17,0x18,0x19,0xFF,0xFF,0xFF,0xFF,0xFF, // 'X', 'Y', 'Z', '[', '\', ']', '^', '_'
                    0xFF,0x00,0x01,0x02,0x03,0x04,0x05,0x06, // '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g'
                    0x07,0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E, // 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o'
                    0x0F,0x10,0x11,0x12,0x13,0x14,0x15,0x16, // 'p', 'q', 'r', 's', 't', 'u', 'v', 'w'
                    0x17,0x18,0x19,0xFF,0xFF,0xFF,0xFF,0xFF  // 'x', 'y', 'z', '{', '|', '}', '~', 'DEL'
            };

    /**
     * Encodes byte array to Base32 String.
     *
     * @param bytes Bytes to encode.
     * @return Encoded byte array <code>bytes</code> as a String.
     *
     */
    public static char[] encode(final byte[] bytes) {
        int i = 0, index = 0, digit = 0, j = 0;
        int currByte, nextByte;
        char[] base32 = new char[(bytes.length + 7) * 8 / 5];

        while (i < bytes.length) {
            currByte = (bytes[i] >= 0) ? bytes[i] : (bytes[i] + 256); // unsign

            /* Is the current digit going to span a byte boundary? */
            if (index > 3) {
                if ((i + 1) < bytes.length) {
                    nextByte =
                            (bytes[i + 1] >= 0) ? bytes[i + 1] : (bytes[i + 1] + 256);
                } else {
                    nextByte = 0;
                }

                digit = currByte & (0xFF >> index);
                index = (index + 5) % 8;
                digit <<= index;
                digit |= nextByte >> (8 - index);
                i++;
            } else {
                digit = (currByte >> (8 - (index + 5))) & 0x1F;
                index = (index + 5) % 8;
                if (index == 0)
                    i++;
            }
            base32[j++] = base32Chars.charAt(digit);
        }

        return Arrays.copyOf(base32, j);
    }

    /**
     * Decodes the given Base32 String to a raw byte array.
     *
     * @param base32
     * @return Decoded <code>base32</code> String as a raw byte array.
     */
    public static byte[] decode(final char[] base32) throws Base32Exception {
        int i, index, lookup, offset, digit;
        byte[] bytes = new byte[base32.length * 5 / 8];

        for (i = 0, index = 0, offset = 0; i < base32.length; i++) {
            // stop decoding when a padding char is encountered
            if (base32[i] == '=') {
                // make sure the rest is also padding, but don't bother verifying the length
                for (int j = i + 1; j < base32.length; j++) {
                    if (base32[j] != '=') {
                        throw new Base32Exception("bad padding");
                    }
                }
                break;
            }

            lookup = base32[i] - '0';
            digit = decodeDigit(lookup);

            if (index <= 3) {
                index = (index + 5) % 8;
                if (index == 0) {
                    bytes[offset] |= digit;
                    offset++;
                    if (offset >= bytes.length)
                        break;
                } else {
                    bytes[offset] |= digit << (8 - index);
                }
            } else {
                index = (index + 5) % 8;
                bytes[offset] |= (digit >>> index);
                offset++;

                if (offset >= bytes.length) {
                    break;
                }
                bytes[offset] |= digit << (8 - index);
            }
        }
        return bytes;
    }

    private static int decodeDigit(int c) throws Base32Exception {
        /* Skip chars outside the lookup table */
        if (c < 0 || c >= base32Lookup.length) {
            throw new Base32Exception("char not found in base32 lookup table");
        }

        int digit = base32Lookup[c];

        /* If this digit is not in the table, ignore it */
        if (digit == 0xFF) {
            throw new Base32Exception("char not found in base32 lookup table");
        }

        return digit;
    }
}
