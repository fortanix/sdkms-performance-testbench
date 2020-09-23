package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.v1.model.CryptMode;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import javax.crypto.Cipher;
import java.security.ProviderException;
import java.util.logging.Level;

public class DecryptionJCESampler extends EncryptDecryptBaseJCESampler {

    private static final String AAD = "SampleAAD";

    protected byte[] cipherBytes;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, this.key, this.params);
            if(mode.equals(CryptMode.GCM)) {
                this.cipher.updateAAD(AAD.getBytes());
            }
            cipherBytes = this.cipher.doFinal(this.input.getBytes());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failure in encrypting : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            this.cipher.init(Cipher.DECRYPT_MODE, this.key, this.params);
            if(mode.equals(CryptMode.GCM)) {
                cipher.updateAAD(AAD.getBytes());
            }
            this.cipher.doFinal(this.cipherBytes);
            result.setSuccessful(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failure in decrypting : " + e, e);
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
