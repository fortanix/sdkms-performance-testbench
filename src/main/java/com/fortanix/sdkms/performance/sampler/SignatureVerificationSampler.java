/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.SignAndVerifyApi;
import com.fortanix.sdkms.v1.model.VerifyRequest;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.ProviderException;
import java.util.logging.Level;

public class SignatureVerificationSampler extends AbstractSignatureSampler {

    private VerifyRequest verifyRequest;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        byte[] signature;
        try {
            signature = this.signAndVerifyApi.sign(this.keyId, this.signRequest).getSignature();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in generating signature : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.verifyRequest = new VerifyRequest().hashAlg(HASH_ALGORITHM).hash(this.hash).signature(signature);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                SignAndVerifyApi signAndVerifyApi;
                String keyId;
                VerifyRequest verifyRequest;

                RetryableOperation init(SignAndVerifyApi signAndVerifyApi, String keyId, VerifyRequest verifyRequest) {
                    this.signAndVerifyApi = signAndVerifyApi;
                    this.keyId = keyId;
                    this.verifyRequest = verifyRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.signAndVerifyApi.verify(this.keyId, this.verifyRequest);
                }
            }.init(this.signAndVerifyApi, this.keyId, this.verifyRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in verifying signature : " + e.getMessage(), e);
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
