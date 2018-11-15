/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import java.io.ByteArrayOutputStream;

/**
 * This class implements padding as specified in the PKCS#5 standard.
 *
 * @see IPadding
 */
final class PKCS5Padding {

    private int blockSize;

    PKCS5Padding(int blockSize) {
        this.blockSize = blockSize;
    }

    /**
     * Adds the given number of padding bytes to the data input. The value of the
     * padding bytes is determined by the specific padding mechanism that implements
     * this interface.
     *
     * @param in
     *            the input buffer with the data to pad
     * @param off
     *            the offset in <code>in</code> where the padding bytes are appended
     * @param len
     *            the number of padding bytes to add
     *
     * @exception ShortBufferException
     *                if <code>in</code> is too small to hold the padding bytes
     */
    public void pad(ByteArrayOutputStream in) {

        int inputLength = in.toByteArray().length;
        int padLength = blockSize - (inputLength % blockSize);

        byte paddingOctet = (byte) (padLength & 0xff);
        for (int i = 0; i < padLength; i++) {
            in.write(paddingOctet);
        }
    }

    /**
     * Returns the index where the padding starts.
     *
     * <p>
     * Given a buffer with padded data, this method returns the index where the
     * padding starts.
     *
     * @param in
     *            the buffer with the padded data
     * @param off
     *            the offset in <code>in</code> where the padded data starts
     * @param len
     *            the length of the padded data
     *
     * @return the index where the padding starts, or -1 if the input is not
     *         properly padded
     */
    public byte[] unpad(byte[] in) {

        int start = 0;

        byte lastByte = in[in.length - 1];
        int padValue = (int) lastByte & 0x0ff;

        start = in.length - padValue;
        if (start < 0) {
            start = -1;
        }

        for (int i = 0; i < padValue; i++) {
            if (in[start + i] != lastByte) {
                start = -1;
            }
        }

        if (start == -1)
            return null;

        byte[] output = new byte[start];
        System.arraycopy(in, 0, output, 0, start);
        return output;
    }
}
