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
import com.fortanix.sdkms.v1.model.DecryptRequestEx;
import com.fortanix.sdkms.v1.model.SobjectDescriptor;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.ProviderException;
import java.util.logging.Level;

public class DecryptionSampler extends AbstractEncryptionAndDecryptionSampler {

    DecryptRequestEx decryptRequest;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        byte[] cipher;
        byte[] tag;
        byte[] iv;
        try {
            cipher = this.encryptionAndDecryptionApi.encryptEx(this.encryptRequest).getCipher();
            tag = this.encryptionAndDecryptionApi.encryptEx(this.encryptRequest).getTag();
            iv = this.encryptionAndDecryptionApi.encryptEx(this.encryptRequest).getIv();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in encrypting : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.decryptRequest = this.encryptionDecryptionHelper.createDecryptRequest(new SobjectDescriptor().kid(this.keyId), cipher, tag, iv);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                EncryptionAndDecryptionApi encryptionAndDecryptionApi;
                DecryptRequestEx decryptRequest;

                RetryableOperation init(EncryptionAndDecryptionApi encryptionAndDecryptionApi, DecryptRequestEx decryptRequest) {
                    this.encryptionAndDecryptionApi = encryptionAndDecryptionApi;
                    this.decryptRequest = decryptRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.encryptionAndDecryptionApi.decryptEx(this.decryptRequest);
                }
            }.init(this.encryptionAndDecryptionApi, this.decryptRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in decrypting : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
