/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.DigestApi;
import com.fortanix.sdkms.v1.model.MacGenerateRequest;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class MACGenerationSampler extends AbstractMACSampler {

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                DigestApi digestApi;
                String keyId;
                MacGenerateRequest macGenerateRequest;

                RetryableOperation init(DigestApi digestApi, String keyId, MacGenerateRequest macGenerateRequest) {
                    this.digestApi = digestApi;
                    this.keyId = keyId;
                    this.macGenerateRequest = macGenerateRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.digestApi.computeMac(this.keyId, this.macGenerateRequest);
                }
            }.init(this.digestApi, this.keyId, this.macGenerateRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in generating MAC : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}