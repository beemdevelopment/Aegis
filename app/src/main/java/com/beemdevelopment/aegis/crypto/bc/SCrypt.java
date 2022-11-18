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

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Integers;
import org.bouncycastle.util.Pack;

/**
 * Implementation of the scrypt a password-based key derivation function.
 * <p>
 * Scrypt was created by Colin Percival and is specified in <a
 * href="https://tools.ietf.org/html/rfc7914">RFC 7914 - The scrypt Password-Based Key Derivation Function</a>
 */
public class SCrypt
{
    private SCrypt()
    {
        // not used.
    }

    /**
     * Generate a key using the scrypt key derivation function.
     *
     * @param P     the bytes of the pass phrase.
     * @param S     the salt to use for this invocation.
     * @param N     CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than
     *              <code>2^(128 * r / 8)</code>.
     * @param r     the block size, must be &gt;= 1.
     * @param p     Parallelization parameter. Must be a positive integer less than or equal to
     *              <code>Integer.MAX_VALUE / (128 * r * 8)</code>.
     * @param dkLen the length of the key to generate.
     * @return the generated key.
     */
    public static byte[] generate(byte[] P, byte[] S, int N, int r, int p, int dkLen)
    {
        if (P == null)
        {
            throw new IllegalArgumentException("Passphrase P must be provided.");
        }
        if (S == null)
        {
            throw new IllegalArgumentException("Salt S must be provided.");
        }
        if (N <= 1 || !isPowerOf2(N))
        {
            throw new IllegalArgumentException("Cost parameter N must be > 1 and a power of 2");
        }
        // Only value of r that cost (as an int) could be exceeded for is 1
        if (r == 1 && N >= 65536)
        {
            throw new IllegalArgumentException("Cost parameter N must be > 1 and < 65536.");
        }
        if (r < 1)
        {
            throw new IllegalArgumentException("Block size r must be >= 1.");
        }
        int maxParallel = Integer.MAX_VALUE / (128 * r * 8);
        if (p < 1 || p > maxParallel)
        {
            throw new IllegalArgumentException("Parallelisation parameter p must be >= 1 and <= " + maxParallel
                    + " (based on block size r of " + r + ")");
        }
        if (dkLen < 1)
        {
            throw new IllegalArgumentException("Generated key length dkLen must be >= 1.");
        }
        return MFcrypt(P, S, N, r, p, dkLen);
    }

    private static byte[] MFcrypt(byte[] P, byte[] S, int N, int r, int p, int dkLen)
    {
        int MFLenBytes = r * 128;
        byte[] bytes = SingleIterationPBKDF2(P, S, p * MFLenBytes);

        int[] B = null;

        try
        {
            int BLen = bytes.length >>> 2;
            B = new int[BLen];

            Pack.littleEndianToInt(bytes, 0, B);

            /*
             * Chunk memory allocations; We choose 'd' so that there will be 2**d chunks, each not
             * larger than 32KiB, except that the minimum chunk size is 2 * r * 32.
             */
            int d = 0, total = N * r;
            while ((N - d) > 2 && total > (1 << 10))
            {
                ++d;
                total >>>= 1;
            }

            int MFLenWords = MFLenBytes >>> 2;
            for (int BOff = 0; BOff < BLen; BOff += MFLenWords)
            {
                // TODO These can be done in parallel threads
                SMix(B, BOff, N, d, r);
            }

            Pack.intToLittleEndian(B, bytes, 0);

            return SingleIterationPBKDF2(P, bytes, dkLen);
        }
        finally
        {
            Clear(bytes);
            Clear(B);
        }
    }

    private static byte[] SingleIterationPBKDF2(byte[] P, byte[] S, int dkLen)
    {
        PBEParametersGenerator pGen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        pGen.init(P, S, 1);
        KeyParameter key = (KeyParameter)pGen.generateDerivedMacParameters(dkLen * 8);
        return key.getKey();
    }

    private static void SMix(int[] B, int BOff, int N, int d, int r)
    {
        int powN = Integers.numberOfTrailingZeros(N);
        int blocksPerChunk = N >>> d;
        int chunkCount = 1 << d, chunkMask = blocksPerChunk - 1, chunkPow = powN - d;

        int BCount = r * 32;

        int[] blockX1 = new int[16];
        int[] blockX2 = new int[16];
        int[] blockY = new int[BCount];

        int[] X = new int[BCount];
        int[][] VV = new int[chunkCount][];

        try
        {
            System.arraycopy(B, BOff, X, 0, BCount);

            for (int c = 0; c < chunkCount; ++c)
            {
                int[] V = new int[blocksPerChunk * BCount];
                VV[c] = V;

                int off = 0;
                for (int i = 0; i < blocksPerChunk; i += 2)
                {
                    System.arraycopy(X, 0, V, off, BCount);
                    off += BCount;
                    BlockMix(X, blockX1, blockX2, blockY, r);
                    System.arraycopy(blockY, 0, V, off, BCount);
                    off += BCount;
                    BlockMix(blockY, blockX1, blockX2, X, r);
                }
            }

            int mask = N - 1;
            for (int i = 0; i < N; ++i)
            {
                int j = X[BCount - 16] & mask;
                int[] V = VV[j >>> chunkPow];
                int VOff = (j & chunkMask) * BCount;
                System.arraycopy(V, VOff, blockY, 0, BCount);
                Xor(blockY, X, 0, blockY);
                BlockMix(blockY, blockX1, blockX2, X, r);
            }

            System.arraycopy(X, 0, B, BOff, BCount);
        }
        finally
        {
            ClearAll(VV);
            ClearAll(new int[][]{X, blockX1, blockX2, blockY});
        }
    }

    private static void BlockMix(int[] B, int[] X1, int[] X2, int[] Y, int r)
    {
        System.arraycopy(B, B.length - 16, X1, 0, 16);

        int BOff = 0, YOff = 0, halfLen = B.length >>> 1;

        for (int i = 2 * r; i > 0; --i)
        {
            Xor(X1, B, BOff, X2);

            Salsa20Engine.salsaCore(8, X2, X1);
            System.arraycopy(X1, 0, Y, YOff, 16);

            YOff = halfLen + BOff - YOff;
            BOff += 16;
        }
    }

    private static void Xor(int[] a, int[] b, int bOff, int[] output)
    {
        for (int i = output.length - 1; i >= 0; --i)
        {
            output[i] = a[i] ^ b[bOff + i];
        }
    }

    private static void Clear(byte[] array)
    {
        if (array != null)
        {
            Arrays.fill(array, (byte)0);
        }
    }

    private static void Clear(int[] array)
    {
        if (array != null)
        {
            Arrays.fill(array, 0);
        }
    }

    private static void ClearAll(int[][] arrays)
    {
        for (int i = 0; i < arrays.length; ++i)
        {
            Clear(arrays[i]);
        }
    }

    // note: we know X is non-zero
    private static boolean isPowerOf2(int x)
    {
        return ((x & (x - 1)) == 0);
    }
}
