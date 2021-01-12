package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.jce.provider.SecurityObjectParameterSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

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
        String hashAlgorithm = context.getParameter(HASH_ALGORITHM);
        this.mgf1ParameterSpec = getMgf1ParameterSpecFromString(hashAlgorithm);
        this.input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }
        this.key = getPrivateKey(algorithm, keySize);
        this.algorithm = jceAlgo(algorithm, hashAlgorithm);
    }

    private MGF1ParameterSpec getMgf1ParameterSpecFromString(String mgf1Parameter) {
        if (mgf1Parameter.equalsIgnoreCase("SHA256")) {
            return MGF1ParameterSpec.SHA256;
        } else if(mgf1Parameter.equalsIgnoreCase("SHA512")) {
            return MGF1ParameterSpec.SHA512;
        } else {
            throw new ProviderException("MGF Parameter " + mgf1Parameter + " not supported in testbench");
        }
    }

    private KeyPair getPrivateKey(String algorithm, String keySize) {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance(algorithm, "sdkms-jce");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOGGER.log(Level.SEVERE, "failure in key generation : " + e.getMessage(), e);
        }
        SecurityObjectParameterSpec parameterSpec;
        if (algorithm.equalsIgnoreCase("EC")) {
            parameterSpec = getECCurveFromString(keySize);
        } else {
            parameterSpec = new SecurityObjectParameterSpec(false);
            assert kpg != null;
            kpg.initialize(Integer.parseInt(keySize));
        }

        try {
            assert kpg != null;
            kpg.initialize(parameterSpec, null);
        } catch (InvalidAlgorithmParameterException e) {
            LOGGER.log(Level.SEVERE, "failure in key generation : " + e.getMessage(), e);
        }
        return kpg.generateKeyPair();
    }


    private SecurityObjectParameterSpec getECCurveFromString(String curve) {

        if (curve.equalsIgnoreCase("SecP192K1")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("SecP192K1")), false);
        } else if (curve.equalsIgnoreCase("SecP224K1")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("SecP224K1")), false);
        } else if (curve.equalsIgnoreCase("SecP256K1")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("SecP256K1")), false);
        } else if (curve.equalsIgnoreCase("NistP192")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("Nist P-192")), false);
        } else if (curve.equalsIgnoreCase("NistP224")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("Nist P-224")), false);
        } else if (curve.equalsIgnoreCase("NistP256")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("Nist P-256")), false);
        } else if (curve.equalsIgnoreCase("NistP384")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("Nist P-384")), false);
        } else if (curve.equalsIgnoreCase("NistP521")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("Nist P-521")), false);
        } else if (curve.equalsIgnoreCase("Gost256A")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("GOST 256A")), false);
        } else if (curve.equalsIgnoreCase("Ed25519")) {
            return new SecurityObjectParameterSpec((new ECGenParameterSpec("Ed25519")), false);
        }
        return null;
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
