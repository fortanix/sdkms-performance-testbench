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

public class EncryptionValentinoSampler extends AbstractJavaSamplerClient {

    static final Logger LOGGER = Logger.getLogger(EncryptionValentinoSampler.class.getName());

    private static final String FORTANIX_API_ENDPOINT = "FORTANIX_API_ENDPOINT";
    private static final String FORTANIX_API_KEY = "FORTANIX_API_KEY";
    private static final String KEYNAME = "keyName";
    private static final String PLAIN = "plain";
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
            LOGGER.log(Level.SEVERE, "Error initializing valentino client with sdkms url: " + url+ " Exception: " + e);
        }
        String basicAuthString = System.getenv(FORTANIX_API_KEY);
        if (StringUtils.isBlank(basicAuthString)) {
            throw new RuntimeException("Missing Environment Variable: " + FORTANIX_API_KEY);
        }
        try {
            this.login();
        } catch (ValentinoException e) {
            LOGGER.log(Level.SEVERE, "Cannot login to the valentino client: " + e);
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        EncryptRequest encryptRequest = new EncryptRequest();
        String keyName = context.getParameter(KEYNAME);
        String algorithm = context.getParameter(ALGORITHM);
        String mode = context.getParameter(MODE);
        String plainBase64 = context.getParameter(PLAIN);
        Kid kid = null;
        try {
            System.out.println("Looking up kid with name : " + keyName);
            kid = valentino.lookup(keyName);
        } catch (ValentinoException e) {
            LOGGER.log(Level.SEVERE, "Error looking up the kid: " + e);
        }

        byte[] plain = Base64.getEncoder().encode(plainBase64.getBytes());
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            EncryptResponse encryptResp = valentino.encrypt(EncryptRequest.builder()
                    .setKid(kid)
                    .setPlain(plain)
                    .setAlg(Algorithm.valueOf(algorithm))
                    .setMode(CipherMode.valueOf(mode))
                    .build());
        } catch (ValentinoException e) {
            LOGGER.log(Level.SEVERE, "Error while encryption: " + e);
        }
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
