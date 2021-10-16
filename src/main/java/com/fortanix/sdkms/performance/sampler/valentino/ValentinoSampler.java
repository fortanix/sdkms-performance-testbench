package com.fortanix.sdkms.performance.sampler.valentino;

import com.fortanix.sdkms.performance.sampler.AbstractSDKMSSamplerClient;
import com.fortanix.sdkms.v1.ApiClient;
import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.AuthenticationApi;
import com.fortanix.valentino.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.ProviderException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.fortanix.sdkms.performance.sampler.Constants.*;
import static com.fortanix.sdkms.performance.sampler.Constants.INVALID_SESSION_MESSAGES;

public class ValentinoSampler extends AbstractJavaSamplerClient {

    static final Logger LOGGER = Logger.getLogger(AbstractSDKMSSamplerClient.class.getName());

    private static final String FORTANIX_API_ENDPOINT = "FORTANIX_API_ENDPOINT";
    private static final String FORTANIX_API_KEY = "FORTANIX_API_KEY";
    private static final String KEYNAME = "keyName";


    Valentino valentino;

    ApiClient apiClient;
    private AuthenticationApi authenticationApi;

    protected void login() throws ValentinoException {
        String apiKey = System.getenv("FORTANIX_API_KEY");
        LOGGER.info("Authenticating Valentino Client using api key: {}" + apiKey);
        valentino.auth(apiKey);
        LOGGER.info("authenticated successfully");
    }

//    public void setupTest(JavaSamplerContext context) {
//        String url = System.getenv(FORTANIX_API_ENDPOINT);
//        if(url == null) {
//            throw new RuntimeException("Environment variable FORTANIX_API_ENDPOINT is not set");
//        }
//        LOGGER.info("Initializing valentino client with sdkms url: {}" + url);
//        try {
//            valentino =  new Valentino(url);
//        } catch (ValentinoException e) {
//            e.printStackTrace();
//        }
//
//
//        this.apiClient = new ApiClient();
//        Map<String, String> envVars = System.getenv();
//        String basePath = envVars.get(ENV_SDKMS_SERVER_URL);
//        if (StringUtils.isBlank(basePath)) {
//            throw new ProviderException("Missing Environment Variable: " + ENV_SDKMS_SERVER_URL);
//        }
//        String basicAuthString = envVars.get(ENV_SDKMS_API_KEY);
//        if (StringUtils.isBlank(basicAuthString)) {
//            throw new ProviderException("Missing Environment Variable: " + ENV_SDKMS_API_KEY);
//        }
//        // configure trust store for server certificates (optional)
//        if (envVars.containsKey(TRUST_STORE_ENV_VAR)) {
//            System.setProperty("javax.net.ssl.trustStore", envVars.get(TRUST_STORE_ENV_VAR));
//        }
////        try {
////            this.login();
////        } catch (ValentinoException e) {
////            e.printStackTrace();
////        }
//    }

    public void setupTest(JavaSamplerContext context) {

    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
//        EncryptRequest encryptRequest = new EncryptRequest();
//        String keyName = context.getParameter(KEYNAME);
//        String algorithm = context.getParameter(ALGORITHM);
//        String mode = context.getParameter(MODE);
//        Kid kid = null;
//        try {
//            System.out.println("Looking up kid with name : " + keyName);
//            kid = valentino.lookup("Valentino");
//        } catch (ValentinoException e) {
//            e.printStackTrace();
//        }
//
//        byte[] plain = Base64.getEncoder().encode("YWJj".getBytes());
        SampleResult result = new SampleResult();
        result.sampleStart();
//        try {
//            EncryptResponse encryptResp = valentino.encrypt(EncryptRequest.builder()
//                    .setKid(kid)
//                    .setPlain(plain)
//                    .setAlg(Algorithm.AES)
//                    .setMode(CipherMode.CBC)
//                    .build());
//        } catch (ValentinoException e) {
//            e.printStackTrace();
//        }
        result.setSuccessful(true);
        result.sampleEnd();
        return result;
    }

    private boolean isValidSession(ApiException e) {
        return !(INVALID_SESSION_CODES.contains(e.getCode()) || INVALID_SESSION_MESSAGES.contains(e.getMessage()));
    }

//    Object retryOperationIfSessionExpires(RetryableOperation command) throws ApiException {
//        try {
//            return command.execute();
//        } catch (ApiException e) {
//            if (this.isValidSession(e)) {
//                throw e;
//            } else {
//                LOGGER.info("Session has expired, trying to re-login");
//                this.login();
//                return command.execute();
//            }
//        }
//    }

    public void teardownTest(JavaSamplerContext context) {
//        try {
//            this.authenticationApi.terminate();
//        } catch (ApiException e) {
//            LOGGER.log(Level.INFO, "failure in logging out : " + e.getMessage(), e);
//        }
    }

}
