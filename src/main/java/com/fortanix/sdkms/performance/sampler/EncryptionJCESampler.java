package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;

public class EncryptionJCESampler extends EncryptDecryptBaseJCESampler {
    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, this.key);
            cipher.doFinal(this.input.getBytes());
            result.setSuccessful(true);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            LOGGER.info("failure in encrypting : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
