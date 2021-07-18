package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.jce.provider.AlgorithmParameters;
import com.fortanix.sdkms.jce.provider.SecurityObjectParameterSpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
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
        String defaultKeySize = algorithm.equalsIgnoreCase("EC") ? "NistP192" : "1024";
        String keySize = context.getParameter(KEY_SIZE, defaultKeySize);
        String filePath = context.getParameter(FILE_PATH);
        String hashAlgorithm = context.getParameter(HASH_ALGORITHM, "SHA256");
        this.pssParameterDigest = hashAlgorithm;
       // this.input = "random-text";
        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[256]; //Means 2048 bit
        random.nextBytes(r);
        String s = Base64.encodeBase64String(r);
        this.input = s;
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
            if(algorithm.equalsIgnoreCase("EC")) {
                SecurityObjectParameterSpec parameterSpec = new SecurityObjectParameterSpec((new ECGenParameterSpec(getAlgorithmParameter(keySize))), false);
                kpg.initialize(new ECGenParameterSpec(getAlgorithmParameter(keySize)), null); // spec
                kpg.initialize(parameterSpec, null);
            }
            else {
                kpg.initialize(Integer.parseInt(keySize));
                SecurityObjectParameterSpec parameterSpec = new SecurityObjectParameterSpec(false);
                kpg.initialize(parameterSpec, null);
            }
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            LOGGER.log(Level.SEVERE, "failure in key generation : " + e.getMessage(), e);
        }
        return kpg.genKeyPair();
    }

    private String jceAlgo(String algorithm, String hashAlgorithm) {
        if(algorithm.equalsIgnoreCase("EC")) {
            return hashAlgorithm.toUpperCase() + "with" + algorithm.toUpperCase() + "DSA";
        }
        return hashAlgorithm.toUpperCase() + "with" + algorithm.toUpperCase();
    }

    private static String getAlgorithmParameter(String curve) {
        if (curve.equalsIgnoreCase("SecP192K1")) {
            return AlgorithmParameters.Secp192k1;
        } else if (curve.equalsIgnoreCase("SecP224K1")) {
            return AlgorithmParameters.Secp224k1;
        } else if (curve.equalsIgnoreCase("SecP256K1")) {
            return AlgorithmParameters.Secp256k1;
        } else if (curve.equalsIgnoreCase("NistP192")) {
            return AlgorithmParameters.NistP192;
        } else if (curve.equalsIgnoreCase("NistP224")) {
            return AlgorithmParameters.NistP224;
        } else if (curve.equalsIgnoreCase("NistP256")) {
            return AlgorithmParameters.NistP256;
        } else if (curve.equalsIgnoreCase("NistP384")) {
            return AlgorithmParameters.NistP384;
        } else if (curve.equalsIgnoreCase("NistP521")) {
            return AlgorithmParameters.NistP521;
        } else if (curve.equalsIgnoreCase("Gost256A")) {
            return AlgorithmParameters.Gost256A;
        } else if (curve.equalsIgnoreCase("Ed25519")) {
            return AlgorithmParameters.Ed25519;
        }
        return null;
    }

}
