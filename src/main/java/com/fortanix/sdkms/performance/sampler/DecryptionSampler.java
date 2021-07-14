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
import com.fortanix.sdkms.v1.model.BatchDecryptRequest;
import com.fortanix.sdkms.v1.model.BatchDecryptRequestInner;
import com.fortanix.sdkms.v1.model.DecryptRequest;
import com.fortanix.sdkms.v1.model.DecryptRequestEx;
import com.fortanix.sdkms.v1.model.SobjectDescriptor;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.ProviderException;
import java.util.logging.Level;

public class DecryptionSampler extends AbstractEncryptionAndDecryptionSampler {

    DecryptRequest decryptRequest;
    DecryptRequestEx decryptRequestEx;
    BatchDecryptRequest batchDecryptRequest;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        byte[] cipher;
        byte[] tag;
        byte[] iv;
        try {
            cipher = this.encryptionAndDecryptionApi.encryptEx(this.encryptRequestEx).getCipher();
            tag = this.encryptionAndDecryptionApi.encryptEx(this.encryptRequestEx).getTag();
            iv = this.encryptionAndDecryptionApi.encryptEx(this.encryptRequestEx).getIv();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in encrypting : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.decryptRequestEx = this.encryptionDecryptionHelper.createDecryptRequest(new SobjectDescriptor().kid(this.keyId), cipher, tag, iv);
        this.decryptRequest = this.encryptionDecryptionHelper.createDecryptRequest(cipher, tag);
        BatchDecryptRequestInner batchDecryptRequestInner = new BatchDecryptRequestInner().kid(this.keyId).request(decryptRequest);

        if(this.batchDecryptRequest != null) {
            if (this.batchEncryptRequest.size() != 0) {
                this.batchDecryptRequest = new BatchDecryptRequest();
                for (int i = 0; i < this.batchEncryptRequest.size(); i++) {
                    this.batchDecryptRequest.add(batchDecryptRequestInner);
                }
            }
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                EncryptionAndDecryptionApi encryptionAndDecryptionApi;
                DecryptRequestEx decryptRequestEx;
                BatchDecryptRequest batchDecryptRequest;

                RetryableOperation init(EncryptionAndDecryptionApi encryptionAndDecryptionApi, DecryptRequestEx decryptRequestEx, BatchDecryptRequest batchDecryptRequest) {
                    this.encryptionAndDecryptionApi = encryptionAndDecryptionApi;
                    this.decryptRequestEx = decryptRequestEx;
                    this.batchDecryptRequest = batchDecryptRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    if (this.batchDecryptRequest == null) {
                        return this.encryptionAndDecryptionApi.decryptEx(this.decryptRequestEx);
                    } else{
                        return this.encryptionAndDecryptionApi.batchDecrypt(this.batchDecryptRequest);
                    }
                }
            }.init(this.encryptionAndDecryptionApi, this.decryptRequestEx, this.batchDecryptRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in decrypting : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
