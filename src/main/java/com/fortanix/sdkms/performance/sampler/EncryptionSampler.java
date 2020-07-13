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
import com.fortanix.sdkms.v1.model.BatchEncryptRequest;
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
                EncryptRequestEx encryptRequestEx;
                BatchEncryptRequest batchEncryptRequest;

                RetryableOperation init(EncryptionAndDecryptionApi encryptionAndDecryptionApi, EncryptRequestEx encryptRequestEx, BatchEncryptRequest batchEncryptRequest) {
                    this.encryptionAndDecryptionApi = encryptionAndDecryptionApi;
                    this.encryptRequestEx = encryptRequestEx;
                    this.batchEncryptRequest = batchEncryptRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    if (this.batchEncryptRequest == null) {
                        return this.encryptionAndDecryptionApi.encryptEx(this.encryptRequestEx);
                    } else{
                        return this.encryptionAndDecryptionApi.batchEncrypt(this.batchEncryptRequest);
                    }
                }
            }.init(this.encryptionAndDecryptionApi, this.encryptRequestEx, this.batchEncryptRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in encrypting : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
