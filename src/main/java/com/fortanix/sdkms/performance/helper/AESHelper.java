/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import com.fortanix.sdkms.v1.model.*;

import java.io.ByteArrayOutputStream;

import static com.fortanix.sdkms.performance.helper.Constants.AES_BLOCK_SIZE;

public class AESHelper extends EncryptionDecryptionHelper {

    AESHelper(CryptMode mode) {
        super(EncryptionDecryptionType.AES);
        this.algorithm = ObjectType.AES;
        this.mode = mode;
        // iv depends on mode
        int blockSize = AES_BLOCK_SIZE;
        if (mode == CryptMode.GCM || mode == CryptMode.CCM) {
            blockSize = 12;
        }
        this.iv = new byte[blockSize];
        random.nextBytes(iv);
        // generate ad
        if (mode == CryptMode.GCM || mode == CryptMode.CCM) {
            this.ad = "TestAD".getBytes();
        }
        // generate tagLen
        if (mode == CryptMode.GCM || mode == CryptMode.CCM) {
            this.tagLen = AES_BLOCK_SIZE * 8;
        }
    }

    @Override
    public EncryptRequestEx createEncryptRequest(SobjectDescriptor key, String plain) {
        EncryptRequestEx encryptRequest = new EncryptRequestEx().key(key).alg(this.algorithm).mode(this.mode).ad(this.ad).tagLen(this.tagLen);
        if(mode == CryptMode.FPE) {
            encryptRequest.plain(plain.getBytes());
        } else {
            ByteArrayOutputStream input = new ByteArrayOutputStream();
            input.write(plain.getBytes(), 0, plain.length());
            PKCS5Padding padding = new PKCS5Padding(AES_BLOCK_SIZE);
            padding.pad(input);
            encryptRequest.iv(this.iv);
            encryptRequest.plain(input.toByteArray());
        }
        return encryptRequest;
    }

    @Override
    public DecryptRequestEx createDecryptRequest(SobjectDescriptor key, byte[] cipher, byte[] tag, byte[] iv) {
        return new DecryptRequestEx().key(key).alg(this.algorithm).cipher(cipher).mode(this.mode).ad(this.ad).tag(tag).iv(iv);
    }
}
