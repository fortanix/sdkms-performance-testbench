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
import com.fortanix.sdkms.v1.model.*;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.ProviderException;
import java.util.logging.Level;

public class DecryptionSampler extends AbstractEncryptionAndDecryptionSampler {

    DecryptRequest decryptRequest;
    DecryptRequestEx decryptRequestEx;
    BatchDecryptRequest batchDecryptRequest;
    BatchDecryptRequestInner batchDecryptRequestInners;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        byte[] cipher;
        try {
            cipher = this.encryptionAndDecryptionApi.encryptEx(this.encryptRequestEx).getCipher();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in encrypting : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.decryptRequestEx = this.encryptionDecryptionHelper.createDecryptRequest(new SobjectDescriptor().kid(this.keyId), cipher);
        if (this.batchEncryptRequest.size() != 0){
            this.batchDecryptRequest = new BatchDecryptRequest();
            for ( int i = 0; i < this.batchEncryptRequest.size() ; i++ ) {
                this.batchDecryptRequest.add(this.batchDecryptRequestInners);
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
                DecryptRequestEx decryptRequest;
                BatchDecryptRequest batchDecryptRequest;

                RetryableOperation init(EncryptionAndDecryptionApi encryptionAndDecryptionApi, DecryptRequestEx decryptRequest, BatchDecryptRequest batchDecryptRequest) {
                    this.encryptionAndDecryptionApi = encryptionAndDecryptionApi;
                    this.decryptRequest = decryptRequest;
                    this.batchDecryptRequest = batchDecryptRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    if (this.batchDecryptRequest == null) {
                        return this.encryptionAndDecryptionApi.decryptEx(this.decryptRequest);
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
