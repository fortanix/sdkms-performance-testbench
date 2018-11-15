/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.EncryptionAndDecryptionApi;
import com.fortanix.sdkms.v1.model.EncryptRequestEx;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class EncryptionSampler extends AbstractEncryptionAndDecryptionSampler {

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                EncryptionAndDecryptionApi encryptionAndDecryptionApi;
                EncryptRequestEx encryptRequest;

                RetryableOperation init(EncryptionAndDecryptionApi encryptionAndDecryptionApi, EncryptRequestEx encryptRequest) {
                    this.encryptionAndDecryptionApi = encryptionAndDecryptionApi;
                    this.encryptRequest = encryptRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.encryptionAndDecryptionApi.encryptEx(this.encryptRequest);
                }
            }.init(this.encryptionAndDecryptionApi, this.encryptRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in encrypting : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}