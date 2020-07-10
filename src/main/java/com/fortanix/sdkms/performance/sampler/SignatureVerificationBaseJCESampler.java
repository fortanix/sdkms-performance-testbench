package com.fortanix.sdkms.performance.sampler;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public abstract class SignatureVerificationBaseJCESampler extends JCEBaseSampler {

    protected String input;
    protected Signature signature;
    protected KeyStore keyStore;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        String algorithm = context.getParameter(ALGORITHM, "RSA");
        int keySize = context.getIntParameter(KEY_SIZE, 2048);
        String hashAlg = context.getParameter(HASH_ALGORITHM, "SHA1").toUpperCase();
        String filePath = context.getParameter(FILE_PATH);

        input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }

        try {
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(algorithm, "sdkms-jce");
            keyGenerator.initialize(keySize);
            KeyPair tempKey = keyGenerator.generateKeyPair(); // by default it generates transient key
            this.keyStore = KeyStore.getInstance("SDKMS-local", "sdkms-jce");
            keyStore.load(null, null);
            keyStore.setKeyEntry("testbenchkey", tempKey.getPrivate(), null, null); // this persists the key
            PrivateKey key = (PrivateKey)keyStore.getKey("testbenchkey", null);

            // e.g. SHA1withRSA
            this.signature = Signature.getInstance(getJCEHashAlg(hashAlg)+"with"+algorithm, "sdkms-jce");
            this.signature.initSign(key);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException | IOException |
                CertificateException | UnrecoverableKeyException | InvalidKeyException e) {
            throw new ProviderException("Unable to initialize signature", e);
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            this.keyStore.deleteEntry("testbenchkey");
        } catch (KeyStoreException e) {
            LOGGER.log(Level.INFO, "failure in deleting key : " + e.getMessage(), e);
        }

    }

    protected String getJCEHashAlg(String hashAlg) {
        if ("SHA256".equals(hashAlg)) {
            return "SHA-256";
        }
        if ("SHA284".equals(hashAlg)) {
            return "SHA-384";
        }
        if ("SHA512".equals(hashAlg)) {
            return "SHA-512";
        }
        if ("SHA1".equals(hashAlg)) {
            return hashAlg;
        }
        throw new ProviderException("Hash Alg " + hashAlg + " not supported in testbench");
    }
}
