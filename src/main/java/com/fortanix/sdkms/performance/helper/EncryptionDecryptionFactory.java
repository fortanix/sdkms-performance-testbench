/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import com.fortanix.sdkms.v1.model.CryptMode;

public class EncryptionDecryptionFactory {

    public static EncryptionDecryptionHelper getHelper(EncryptionDecryptionType encryptionDecryptionType, CryptMode mode) {
        EncryptionDecryptionHelper encryptionDecryptionHelper = null;
        switch (encryptionDecryptionType) {
            case AES:
                encryptionDecryptionHelper = new AESHelper(mode);
                break;
            case DES:
                encryptionDecryptionHelper = new DESHelper();
                break;
            case DES3:
                encryptionDecryptionHelper = new DES3Helper();
                break;
            case RSA:
                encryptionDecryptionHelper = new RSAHelper();
                break;
        }
        return encryptionDecryptionHelper;
    }
}
