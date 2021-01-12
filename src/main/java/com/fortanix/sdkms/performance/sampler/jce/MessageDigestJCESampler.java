package com.fortanix.sdkms.performance.sampler.jce;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.FILE_PATH;
import static com.fortanix.sdkms.performance.sampler.Constants.HASH_ALGORITHM;

public class MessageDigestJCESampler extends JCEBaseSampler {

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

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm, "sdkms-jce");
            md.update(input.getBytes());
            md.digest();
            result.setSuccessful(true);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.log(Level.SEVERE, "failure in message digest generation : " + e.getMessage(), e);
            result.setSuccessful(false);
        }
        result.sampleEnd();
        return result;
    }
}
