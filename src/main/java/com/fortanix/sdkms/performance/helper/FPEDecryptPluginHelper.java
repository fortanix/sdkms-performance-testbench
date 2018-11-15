/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import com.fortanix.sdkms.performance.payload.FPEDecryptRequestPayload;
import com.fortanix.sdkms.performance.payload.RequestPayload;

import java.util.List;

class FPEDecryptPluginHelper extends PluginHelper {

    FPEDecryptPluginHelper(String pluginId) {
        super(PluginType.FPEDecrypt, pluginId);
    }

    @Override
    public RequestPayload createRequestPayload(String keyId, int radix, String tweak, String data) {
        FPEDecryptRequestPayload requestPayload = new FPEDecryptRequestPayload();
        requestPayload.keyId = keyId;
        requestPayload.radix = radix;
        requestPayload.tweak = tweak;
        requestPayload.encryptedData = data;
        return requestPayload;
    }

    @Override
    public RequestPayload createRequestPayload(String keyId, int radix, String tweak, List<String> data) {
        FPEDecryptRequestPayload requestPayload = new FPEDecryptRequestPayload();
        requestPayload.keyId = keyId;
        requestPayload.radix = radix;
        requestPayload.tweak = tweak;
        requestPayload.encryptedDataList = data;
        return requestPayload;
    }
}
