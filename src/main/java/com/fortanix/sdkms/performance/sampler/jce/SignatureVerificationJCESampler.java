package com.fortanix.sdkms.performance.sampler.jce;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.PSSParameterSpec;
import java.util.logging.Level;

public class SignatureVerificationJCESampler extends SignatureVerificationBaseSampler{

    private Signature sig =null;
    private byte[] signatureBytes;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        try {
            sig = Signature.getInstance(this.algorithm, "sdkms-jce");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.log(Level.SEVERE, "failure in signature verification : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        if(this.algorithm.contains("RSA")) {
        if (this.mgf1ParameterSpec != null || this.pssParameterDigest != null) {
            assert sig != null;
            try {
                sig.setParameter(new PSSParameterSpec(pssParameterDigest, "MGF1", mgf1ParameterSpec, 32, 1));
            } catch (InvalidAlgorithmParameterException e) {
                LOGGER.log(Level.SEVERE, "failure in signature verification : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }
        }
        assert sig != null;
        try {
            sig.initSign(this.key.getPrivate());
        } catch (InvalidKeyException e) {
            LOGGER.log(Level.SEVERE, "failure in signature verification : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        try {
            signatureBytes = sig.sign();
        }
        catch ( SignatureException e ) {
            LOGGER.log(Level.SEVERE, "failure in signature verification : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            byte[] data = this.input.getBytes("UTF8");
            sig.initVerify(key.getPublic());
            sig.update(ByteBuffer.wrap(data));
            sig.verify(signatureBytes);
            result.setSuccessful(true);
        } catch (SignatureException | UnsupportedEncodingException | InvalidKeyException e) {
            LOGGER.log(Level.SEVERE, "failure in signature verification : " + e.getMessage(), e);
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }

}
