/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import com.fortanix.sdkms.v1.model.*;

import java.util.Random;

public class EncryptionDecryptionHelper {
    static final Random random = new Random();
    ObjectType algorithm;
    CryptMode mode;
    byte[] iv;
    byte[] ad;
    Integer tagLen;
    private EncryptionDecryptionType encryptionDecryptionType;

    EncryptionDecryptionHelper(EncryptionDecryptionType encryptionDecryptionType) {
        this.encryptionDecryptionType = encryptionDecryptionType;
    }

    public EncryptRequestEx createEncryptRequest(SobjectDescriptor key, String plainText) {
        return new EncryptRequestEx().key(key).alg(this.algorithm).plain(plainText.getBytes()).mode(this.mode).iv(this.iv).ad(this.ad).tagLen(this.tagLen);
    }

    public DecryptRequestEx createDecryptRequest(SobjectDescriptor key, byte[] cipher, byte[] tag) {
        return new DecryptRequestEx().key(key).alg(this.algorithm).cipher(cipher).mode(this.mode).iv(this.iv).ad(this.ad).tag(tag);
    }
}
