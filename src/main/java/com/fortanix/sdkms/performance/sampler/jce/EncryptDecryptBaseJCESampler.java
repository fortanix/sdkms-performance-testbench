package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.jce.provider.SdkmsKey;
import com.fortanix.sdkms.jce.provider.SecurityObjectParameterSpec;
import com.fortanix.sdkms.jce.provider.service.ApiClientSetup;
import com.fortanix.sdkms.v1.api.SecurityObjectsApi;
import com.fortanix.sdkms.v1.model.CryptMode;
import com.fortanix.sdkms.v1.model.ObjectType;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public abstract class EncryptDecryptBaseJCESampler extends JCEBaseSampler {

    protected CryptMode mode;
    protected Key key;
    protected Cipher cipher;
    protected AlgorithmParameters params;
    protected String input;

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        String algorithm = context.getParameter(ALGORITHM, "AES");
        int keySize = context.getIntParameter(KEY_SIZE, 128);
        this.mode = CryptMode.fromValue(context.getParameter(MODE, "CBC"));
        String filePath = context.getParameter(FILE_PATH);
        String jceCipherAlgo = getJCEAlgo(ObjectType.fromValue(algorithm), keySize, mode);
        this.input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }

        String keyAlg = algorithm;
        if (ObjectType.DES3.getValue().equals(algorithm)) keyAlg = "DESede";
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(keyAlg, "sdkms-jce");
            SecurityObjectParameterSpec parameterSpec = new SecurityObjectParameterSpec(false);
            keyGenerator.init(keySize);
            keyGenerator.init(parameterSpec);
            this.key = keyGenerator.generateKey();
            this.cipher = Cipher.getInstance(jceCipherAlgo, "sdkms-jce");
            this.params = cipher.getParameters();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize cipher : " + e.getMessage(), e);
            throw new ProviderException("Unable to initialize cipher. ", e);
        }
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            new SecurityObjectsApi(ApiClientSetup.getInstance().getApiClient()).deleteSecurityObject(((SdkmsKey)this.key).getKeyDescriptor().getKid());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "failure in deleting key : " + e.getMessage(), e);
        }
        super.teardownTest(context);
    }
    private String getJCEAlgo(ObjectType objectType, int keySize, CryptMode mode) {
        String padding = "PKCS5PADDING";
        if (CryptMode.CBCNOPAD.equals(mode)) {
            padding = "NoPadding";
        }
        if (ObjectType.DES.equals(objectType)) {
            return objectType.getValue() + "/" + mode.getValue() + "/" + padding;
        }
        if (ObjectType.DES3.equals(objectType)) {
            return "DESede" + "/" + mode.getValue() + "/" + padding;
        }
        if (ObjectType.AES.equals(objectType)) {
            return "AES" + "_" + keySize + "/" + mode.getValue() + "/" + padding;
        }
        throw new ProviderException("Algorithm " + objectType.getValue() + " not supported for Cipher in testbench");
    }

}
