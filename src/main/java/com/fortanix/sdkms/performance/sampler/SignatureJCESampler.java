package com.fortanix.sdkms.performance.sampler;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.SignatureException;

public class SignatureJCESampler extends SignatureVerificationBaseJCESampler {

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            this.signature.update(this.input.getBytes());
            this.signature.sign();
            result.setSuccessful(true);
        } catch (SignatureException e) {
            LOGGER.info("failure in encrypting : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
