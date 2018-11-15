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
import com.fortanix.sdkms.v1.model.MacVerifyRequest;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.ProviderException;
import java.util.logging.Level;

public class MACVerificationSampler extends AbstractMACSampler {

    private MacVerifyRequest macVerifyRequest;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        byte[] mac;
        try {
            mac = this.digestApi.computeMac(this.keyId, this.macGenerateRequest).getDigest();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in generating MAC : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.macVerifyRequest = new MacVerifyRequest().alg(this.hashAlgorithm).data(this.text.getBytes()).digest(mac);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                DigestApi digestApi;
                String keyId;
                MacVerifyRequest macVerifyRequest;

                RetryableOperation init(DigestApi digestApi, String keyId, MacVerifyRequest macVerifyRequest) {
                    this.digestApi = digestApi;
                    this.keyId = keyId;
                    this.macVerifyRequest = macVerifyRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.digestApi.verifyMac(this.keyId, this.macVerifyRequest);
                }
            }.init(this.digestApi, this.keyId, this.macVerifyRequest));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in verifying signature : " + e.getMessage(), e);
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}