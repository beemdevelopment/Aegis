/*
Copyright (c) 2000-2021 The Legion of the Bouncy Castle Inc. (https://www.bouncycastle.org)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
 */

package com.beemdevelopment.aegis.crypto.bc;

/**
 * Implementation of Daniel J. Bernstein's Salsa20 stream cipher, Snuffle 2005
 */
public class Salsa20Engine {
    private Salsa20Engine()
    {

    }

    public static void salsaCore(int rounds, int[] input, int[] x)
    {
        if (input.length != 16)
        {
            throw new IllegalArgumentException();
        }
        if (x.length != 16)
        {
            throw new IllegalArgumentException();
        }
        if (rounds % 2 != 0)
        {
            throw new IllegalArgumentException("Number of rounds must be even");
        }

        int x00 = input[ 0];
        int x01 = input[ 1];
        int x02 = input[ 2];
        int x03 = input[ 3];
        int x04 = input[ 4];
        int x05 = input[ 5];
        int x06 = input[ 6];
        int x07 = input[ 7];
        int x08 = input[ 8];
        int x09 = input[ 9];
        int x10 = input[10];
        int x11 = input[11];
        int x12 = input[12];
        int x13 = input[13];
        int x14 = input[14];
        int x15 = input[15];

        for (int i = rounds; i > 0; i -= 2)
        {
            x04 ^= Integer.rotateLeft(x00 + x12, 7);
            x08 ^= Integer.rotateLeft(x04 + x00, 9);
            x12 ^= Integer.rotateLeft(x08 + x04, 13);
            x00 ^= Integer.rotateLeft(x12 + x08, 18);
            x09 ^= Integer.rotateLeft(x05 + x01, 7);
            x13 ^= Integer.rotateLeft(x09 + x05, 9);
            x01 ^= Integer.rotateLeft(x13 + x09, 13);
            x05 ^= Integer.rotateLeft(x01 + x13, 18);
            x14 ^= Integer.rotateLeft(x10 + x06, 7);
            x02 ^= Integer.rotateLeft(x14 + x10, 9);
            x06 ^= Integer.rotateLeft(x02 + x14, 13);
            x10 ^= Integer.rotateLeft(x06 + x02, 18);
            x03 ^= Integer.rotateLeft(x15 + x11, 7);
            x07 ^= Integer.rotateLeft(x03 + x15, 9);
            x11 ^= Integer.rotateLeft(x07 + x03, 13);
            x15 ^= Integer.rotateLeft(x11 + x07, 18);

            x01 ^= Integer.rotateLeft(x00 + x03, 7);
            x02 ^= Integer.rotateLeft(x01 + x00, 9);
            x03 ^= Integer.rotateLeft(x02 + x01, 13);
            x00 ^= Integer.rotateLeft(x03 + x02, 18);
            x06 ^= Integer.rotateLeft(x05 + x04, 7);
            x07 ^= Integer.rotateLeft(x06 + x05, 9);
            x04 ^= Integer.rotateLeft(x07 + x06, 13);
            x05 ^= Integer.rotateLeft(x04 + x07, 18);
            x11 ^= Integer.rotateLeft(x10 + x09, 7);
            x08 ^= Integer.rotateLeft(x11 + x10, 9);
            x09 ^= Integer.rotateLeft(x08 + x11, 13);
            x10 ^= Integer.rotateLeft(x09 + x08, 18);
            x12 ^= Integer.rotateLeft(x15 + x14, 7);
            x13 ^= Integer.rotateLeft(x12 + x15, 9);
            x14 ^= Integer.rotateLeft(x13 + x12, 13);
            x15 ^= Integer.rotateLeft(x14 + x13, 18);
        }

        x[ 0] = x00 + input[ 0];
        x[ 1] = x01 + input[ 1];
        x[ 2] = x02 + input[ 2];
        x[ 3] = x03 + input[ 3];
        x[ 4] = x04 + input[ 4];
        x[ 5] = x05 + input[ 5];
        x[ 6] = x06 + input[ 6];
        x[ 7] = x07 + input[ 7];
        x[ 8] = x08 + input[ 8];
        x[ 9] = x09 + input[ 9];
        x[10] = x10 + input[10];
        x[11] = x11 + input[11];
        x[12] = x12 + input[12];
        x[13] = x13 + input[13];
        x[14] = x14 + input[14];
        x[15] = x15 + input[15];
    }
}
