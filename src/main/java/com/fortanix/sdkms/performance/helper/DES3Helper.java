/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import com.fortanix.sdkms.v1.model.CryptMode;
import com.fortanix.sdkms.v1.model.EncryptRequestEx;
import com.fortanix.sdkms.v1.model.ObjectType;
import com.fortanix.sdkms.v1.model.SobjectDescriptor;

import java.io.ByteArrayOutputStream;

import static com.fortanix.sdkms.performance.helper.Constants.DES3_BLOCK_SIZE;

public class DES3Helper extends EncryptionDecryptionHelper {

    DES3Helper() {
        super(EncryptionDecryptionType.DES3);
        this.algorithm = ObjectType.DES3;
        this.mode = CryptMode.CBC;
        this.iv = new byte[DES3_BLOCK_SIZE];
        random.nextBytes(this.iv);
    }

    @Override
    public EncryptRequestEx createEncryptRequest(SobjectDescriptor key, String plain) {
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.write(plain.getBytes(), 0, plain.length());
        PKCS5Padding padding = new PKCS5Padding(DES3_BLOCK_SIZE);
        padding.pad(input);
        return new EncryptRequestEx().key(key).alg(this.algorithm).plain(input.toByteArray()).mode(this.mode).iv(this.iv).ad(this.ad).tagLen(this.tagLen);
    }
}
