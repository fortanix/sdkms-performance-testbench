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
import com.fortanix.sdkms.v1.model.SignRequest;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class SignatureGenerationSampler extends AbstractSignatureSampler {

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                SignAndVerifyApi signAndVerifyApi;
                String keyId;
                SignRequest signRequest;

                RetryableOperation init(SignAndVerifyApi signAndVerifyApi, String keyId, SignRequest signRequest) {
                    this.signAndVerifyApi = signAndVerifyApi;
                    this.keyId = keyId;
                    this.signRequest = signRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.signAndVerifyApi.sign(this.keyId, this.signRequest);
                }
            }.init(this.signAndVerifyApi, this.keyId, this.signRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in generating signature : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
