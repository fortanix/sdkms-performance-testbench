/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import com.fortanix.sdkms.v1.model.ObjectType;

import static com.fortanix.sdkms.performance.helper.Constants.RSA_BLOCK_SIZE;

class RSAHelper extends EncryptionDecryptionHelper {

    RSAHelper() {
        super(EncryptionDecryptionType.RSA);
        this.algorithm = ObjectType.RSA;
        this.iv = new byte[RSA_BLOCK_SIZE];
        random.nextBytes(iv);
    }
}
