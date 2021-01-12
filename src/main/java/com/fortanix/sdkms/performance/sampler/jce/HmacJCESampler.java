package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.jce.provider.AlgorithmParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public class HmacJCESampler extends JCEBaseSampler {
    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        String algorithm = context.getParameter(HASH_ALGORITHM, "SHA1");
        String filePath = context.getParameter(FILE_PATH);
        String input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }

        String algorithmParam = getAlgorithmParam(algorithm);
        try {
            Mac mac = Mac.getInstance(algorithmParam, "sdkms-jce");
            mac.init(getSecretKey());
            mac.update(input.getBytes());
            mac.doFinal();
            result.setSuccessful(true);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            LOGGER.log(Level.SEVERE, "failure in mac generation : " + e.getMessage(), e);
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }

    private String getAlgorithmParam(String algorithm) {
        if (algorithm.equalsIgnoreCase("SHA1")) {
            return AlgorithmParameters.HmacSHA1;
        } else if (algorithm.equalsIgnoreCase("SHA256")) {
            return AlgorithmParameters.HmacSHA256;
        } else if (algorithm.equalsIgnoreCase("SHA384")) {
            return AlgorithmParameters.HmacSHA384;
        } else if (algorithm.equalsIgnoreCase("SHA512")) {
            return AlgorithmParameters.HmacSHA512;
        }
        return null;
    }

    private SecretKey getSecretKey() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyGenerator keygenerator = KeyGenerator.getInstance(AlgorithmParameters.HmacSHA1, "sdkms-jce");
        return keygenerator.generateKey();
    }
}
