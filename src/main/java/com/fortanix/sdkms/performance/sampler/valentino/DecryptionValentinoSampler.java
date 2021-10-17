package com.fortanix.sdkms.performance.sampler.valentino;

import com.fortanix.valentino.*;
import org.apache.commons.lang.StringUtils;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.fortanix.sdkms.performance.sampler.Constants.ALGORITHM;
import static com.fortanix.sdkms.performance.sampler.Constants.MODE;

public class DecryptionValentinoSampler extends AbstractJavaSamplerClient {
    static final Logger LOGGER = Logger.getLogger(DecryptionValentinoSampler.class.getName());

    private static final String FORTANIX_API_ENDPOINT = "FORTANIX_API_ENDPOINT";
    private static final String FORTANIX_API_KEY = "FORTANIX_API_KEY";
    private static final String KEYNAME = "keyName";
    private static final String CIPHER = "cipher";
    private static final String IV = "iv";
    Valentino valentino;

    protected void login() throws ValentinoException {
        String apiKey = System.getenv("FORTANIX_API_KEY");
        LOGGER.info("Authenticating Valentino Client using api key: {}" + apiKey);
        valentino.auth(apiKey);
        LOGGER.info("authenticated successfully");
    }

    public void setupTest(JavaSamplerContext context) {
        String url = System.getenv(FORTANIX_API_ENDPOINT);
        if(url == null) {
            throw new RuntimeException("Environment variable FORTANIX_API_ENDPOINT is not set");
        }
        LOGGER.info("Initializing valentino client with sdkms url: {}" + url);
        try {
            valentino =  new Valentino(url);
        } catch (ValentinoException e) {
            e.printStackTrace();
        }
        String basicAuthString = System.getenv(FORTANIX_API_KEY);
        if (StringUtils.isBlank(basicAuthString)) {
            throw new RuntimeException("Missing Environment Variable: " + FORTANIX_API_KEY);
        }
        try {
            this.login();
        } catch (ValentinoException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        String keyName = context.getParameter(KEYNAME);
        String algorithm = context.getParameter(ALGORITHM);
        String mode = context.getParameter(MODE);
        String cipherBase64 = context.getParameter(CIPHER);
        String ivBase64 = context.getParameter(IV);
        Kid kid = null;
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            System.out.println("Looking up kid with name : " + keyName);
            kid = valentino.lookup(keyName);
        } catch (ValentinoException e) {
            e.printStackTrace();
        }
        byte[] plain = Base64.getDecoder().decode(cipherBase64.getBytes());
        byte[] iv = Base64.getDecoder().decode(ivBase64.getBytes());

        LOGGER.info("Decrypting request: {" +plain+ "} with kid: {"+kid+"}");
        DecryptResponse decryptResp = null;
        try {
            decryptResp = valentino.decrypt(DecryptRequest.builder()
                    .setKid(kid)
                    .setCipher(plain)
                    .setAlg(Algorithm.valueOf(algorithm))
                    .setMode(CipherMode.valueOf(mode))
                    .setIv(iv)
                    .build());
        } catch (ValentinoException e) {
            e.printStackTrace();
        }
        LOGGER.info("Decryption successful with response: {"+decryptResp.toString()+"}");
        result.setSuccessful(true);
        result.sampleEnd();
        return result;
    }

    public void teardownTest(JavaSamplerContext context) {
        try {
            this.valentino.terminate();
        } catch (ValentinoException e) {
            LOGGER.log(Level.INFO, "failure in logging out : " + e.getMessage(), e);
        }
    }
}
