package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.jce.provider.SecurityObjectParameterSpec;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import javax.crypto.KeyGenerator;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.ALGORITHM;
import static com.fortanix.sdkms.performance.sampler.Constants.KEY_SIZE;

public class KeyGenerationJCESampler extends JCEBaseSampler {
    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        String algorithm = context.getParameter(ALGORITHM, "RSA");
        String keySize = context.getParameter(KEY_SIZE, "1024");
        if (algorithm.equalsIgnoreCase("RSA") || algorithm.equalsIgnoreCase("EC")) {
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
                kpg.generateKeyPair();
                result.setSuccessful(true);
            } catch (InvalidAlgorithmParameterException e) {
                LOGGER.log(Level.SEVERE, "failure in key generation : " + e.getMessage(), e);
                result.setSuccessful(false);
            }
        } else {
            if (algorithm.equalsIgnoreCase("DES3")) algorithm = "TripleDES";
            KeyGenerator keyGenerator = null;
            try {
                keyGenerator = KeyGenerator.getInstance(algorithm, "sdkms-jce");
                SecurityObjectParameterSpec parameterSpec = new SecurityObjectParameterSpec(false);
                keyGenerator.init(Integer.parseInt(keySize));
                keyGenerator.init(parameterSpec);
                keyGenerator.generateKey();
                result.setSuccessful(true);
            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
                LOGGER.log(Level.SEVERE, "failure in key generation : " + e.getMessage(), e);
                result.setSuccessful(false);
            }
        }
        result.sampleEnd();
        return result;
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
}
