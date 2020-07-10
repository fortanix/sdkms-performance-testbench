package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.SecurityObjectsApi;
import com.fortanix.sdkms.v1.model.CryptMode;
import com.fortanix.sdkms.v1.model.FpeOptions;
import com.fortanix.sdkms.v1.model.ObjectType;
import com.fortanix.sdkms.v1.model.SobjectRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;
import static com.fortanix.sdkms.performance.sampler.Constants.FILE_PATH;

public abstract class EncryptDecryptBaseJCESampler extends JCEBaseSampler {

    protected Key key;
    protected Cipher cipher;
    protected String input;
    protected KeyStore keyStore;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        String algorithm = context.getParameter(ALGORITHM, "AES");
        int keySize = context.getIntParameter(KEY_SIZE, 256);
        String mode = context.getParameter(MODE, "CBC");
        String jceCipherAlgo = getJCEAlgo(algorithm, keySize, mode);

        String filePath = context.getParameter(FILE_PATH);
        this.input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }

        String keyAlg = algorithm;
        if ("DES3".equals(algorithm)) keyAlg = "DESede";
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(keyAlg, "sdkms-jce");
            keyGenerator.init(keySize);
            Key tempKey = keyGenerator.generateKey(); // by default it generates transient key
            this.keyStore = KeyStore.getInstance("SDKMS-local", "sdkms-jce");
            keyStore.load(null, null);
            keyStore.setKeyEntry("testbenchkey", tempKey, null, null); // this persists the key
            this.key = keyStore.getKey("testbenchkey", null);
            this.cipher = Cipher.getInstance(jceCipherAlgo, "sdkms-jce");
        } catch (NoSuchAlgorithmException | NoSuchProviderException | CertificateException |
                KeyStoreException | IOException | UnrecoverableKeyException | NoSuchPaddingException e) {
            throw new ProviderException("Unable to initialize cipher. ", e);
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
    private String getJCEAlgo(String algorithm, int keySize, String mode) {
        String padding = "PKCS5PADDING";
        if ("CBCNOPAD".equals(mode)) {
            padding = "NoPadding";
        }
        if ("DES".equals(algorithm)) {
            return algorithm + "/" + mode + "/" + padding;
        }
        if ("DES3".equals(algorithm)) {
            return "DESede" + "/" + mode + "/" + padding;
        }
        if ("AES".equals(algorithm)) {
            return algorithm + "_" + keySize + mode + "/" + padding;
        }
        throw new ProviderException("Algorithm " + algorithm + " not supported for Cipher in testbench");
    }
}
