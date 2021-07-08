package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.jce.provider.SecurityObjectParameterSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;
import static com.fortanix.sdkms.performance.sampler.Constants.HASH_ALGORITHM;

public abstract class SignatureVerificationBaseSampler extends JCEBaseSampler {

    protected KeyPair key;
    protected String input;
    String algorithm;
    MGF1ParameterSpec mgf1ParameterSpec;
    String pssParameterDigest;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        String algorithm = context.getParameter(ALGORITHM, "RSA");
        String keySize = context.getParameter(KEY_SIZE, "1024");
        String filePath = context.getParameter(FILE_PATH);
        String hashAlgorithm = context.getParameter(HASH_ALGORITHM, "SHA256");
        this.pssParameterDigest = hashAlgorithm;
        this.input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }
        this.key = getKeyPair(algorithm, keySize);
        this.algorithm = jceAlgo(algorithm, hashAlgorithm);
    }

    private static KeyPair getKeyPair(String algorithm, String keySize) {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance(algorithm, "sdkms-jce");
        } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "failure in key generation : " + e.getMessage(), e);
        }
        SecurityObjectParameterSpec parameterSpec = new SecurityObjectParameterSpec(false);
        kpg.initialize(Integer.parseInt(keySize));
        try {
            kpg.initialize(parameterSpec, null);
        } catch (InvalidAlgorithmParameterException e) {
            LOGGER.log(Level.SEVERE, "failure in key generation : " + e.getMessage(), e);
        }
        return kpg.genKeyPair();
    }

    private String jceAlgo(String algorithm, String hashAlgorithm) {
        String alg = "";
        if (algorithm.equalsIgnoreCase("RSA")) {
            if (hashAlgorithm.equalsIgnoreCase("SHA1")) {
                alg = "SHA1withRSA";
            } else if (hashAlgorithm.equalsIgnoreCase("SHA256")) {
                alg = "SHA256withRSA";
            } else if (hashAlgorithm.equalsIgnoreCase("SHA384")) {
                alg = "SHA384withRSA";
            } else if (hashAlgorithm.equalsIgnoreCase("SHA512")) {
                alg = "SHA512withRSA";
            }
        }
        return alg;
    }

}
