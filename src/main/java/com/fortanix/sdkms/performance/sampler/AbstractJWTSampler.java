/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.DigestApi;
import com.fortanix.sdkms.v1.api.SecurityObjectsApi;
import com.fortanix.sdkms.v1.api.SignAndVerifyApi;
import com.fortanix.sdkms.v1.model.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.fortanix.sdkms.v1.api.*;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class AbstractJWTSampler extends AbstractSDKMSSamplerClient {
    KeyObject aesKeyId;
    KeyObject hmacKeyId;
    KeyObject certificateObj;
    public static String cert = "MIIDazCCAlOgAwIBAgIUcWNiUbefplnYcAgVz1IclX2MpXcwDQYJKoZIhvcNAQELBQAwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgMClNvbWUtU3RhdGUxITAfBgNVBAoMGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZDAeFw0yMDA1MTkxNDQzMTFaFw0yMTA1MTkxNDQzMTFaMEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDiaeqruinCRfeUSyko9YM30thSQMbZ+bK0UuVhSTo8zPy3Z0vTQqm+QSA7cgOxSmSYWDcrCwZi36putbOL24wCi2nSBNlFz25MkEnZ8i6eQh/s61gAisL4/b8yu4wg5tJPmL13Dylvr0nXN/ydqwenQ+XE0qPloXkhRkkL77cV3jg8y/vQnKMQGNoL9DA9HVJbjJH0xt3Ow8docvYttmz6MItmzy5MW4XFSdjl17hcqhVYTYkoAuMtIoBlgTpBvgi6gU4m2FprgWDbVD12ZjkS3RXtVM6uAYbpq1iYEsl/eLenWvo7tdVHTjuF0qbn491HcsNYJODagWPRX2y9/EuJAgMBAAGjUzBRMB0GA1UdDgQWBBR0k6xxhwo085+T/WfdYGHKo/cXGDAfBgNVHSMEGDAWgBR0k6xxhwo085+T/WfdYGHKo/cXGDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBrO42QhV5v0t2w4Yz09FRi97CD6NuHF1PorJYFmX5DaGMGLh1PHf1yJCHwzsd00dP2Ucjh5OagRJcOBd+eeHndQweazc8eAbk3+Pp0z4C2lUZVtNRyfTW0BDaleCX9P7u7HRLgUHSDmEAzx0uEl/XdZN3nwVtHorZLWO9DaxkSl/p9YKc1XhJ4Y6zTzROVtDeP/ylGH0GOm+LA5E8mdMXDVY8Dw9khJq2KPjeia5C0DmtieFeKczfWHuFeyOYAziq0mA+yDO2v/5jEQacyaREU2T5N4AesgNp9xhhWfchPFStZ0grSPh+Tsn1fCA/15OBdSJ2SCtbeboyL7Wv4Pv6B";

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        SecurityObjectsApi securityObjectsApi = new SecurityObjectsApi(this.apiClient);
        List<KeyOperations> aesKeyOperations = new ArrayList<KeyOperations>(Arrays.asList(KeyOperations.ENCRYPT, KeyOperations.DECRYPT, KeyOperations.EXPORT));
        SobjectRequest aes_sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(ObjectType.AES).keySize(256).keyOps(aesKeyOperations);
        try {
            this.aesKeyId = securityObjectsApi.generateSecurityObject(aes_sobjectRequest);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        List<KeyOperations> hmacKeyOperations = new ArrayList<KeyOperations>(Arrays.asList(KeyOperations.MACGENERATE, KeyOperations.MACVERIFY, KeyOperations.EXPORT));
        SobjectRequest hmac_sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(ObjectType.HMAC).keySize(256).keyOps(hmacKeyOperations);
        try {
            this.hmacKeyId = securityObjectsApi.generateSecurityObject(hmac_sobjectRequest);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        SobjectRequest cert_sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(ObjectType.CERTIFICATE).value(Base64.decode(cert));
        try {
            this.certificateObj = securityObjectsApi.importSecurityObject(cert_sobjectRequest);
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    private byte[] exportKey(KeyObject keyObject) {
        SecurityObjectsApi securityObjectsApi = new SecurityObjectsApi(this.apiClient);
        try {
            KeyObject obj = securityObjectsApi.getSecurityObjectValue(keyObject.getKid());
//            System.out.println(obj.getValue());
            return obj.getValue();
        } catch (ApiException e) {
            e.printStackTrace();
            return null;
        }
    }

    private KeyObject createCertificate(String certificate) {
        try {
            SobjectRequest sobjectRequest = new SobjectRequest();
            sobjectRequest.objType(ObjectType.CERTIFICATE)
                    .name(UUID.randomUUID().toString())
//                ._transient(true)
                    .value(Base64.decode(certificate));

            SecurityObjectsApi securityObjectsApi = new SecurityObjectsApi(this.apiClient);
            return securityObjectsApi.importSecurityObject(sobjectRequest);
        } catch (Exception e) {
            System.out.println("Certificate generate failed: " + e);
            return null;
        }
    }

    private static byte[] truncateDigest(byte[] digest) {
        return Arrays.copyOfRange(digest, 0, digest.length/2);
    }

    private MacGenerateResponse performMac(KeyObject keyObject, byte[] data, DigestAlgorithm digestAlgorithm) {
        MacGenerateRequest macGenerateRequest = new MacGenerateRequest();
        macGenerateRequest
                .alg(digestAlgorithm)
                .data(data);
        try {
            DigestApi digestApi = new DigestApi(this.apiClient);
            return digestApi.computeMac(keyObject.getKid(), macGenerateRequest);
        } catch (Exception e) {
            System.out.println("Mac generate failed: " + e);
            return null;
        }
    }

    private static String computeLength(String s) {
        int bitlength = s.length() * 8;
        int[] a = new int[8];
        for (int i = 7; i >= 0; i--) {
            a[i] = bitlength % 256;
            bitlength = bitlength / 256;
        }
        String str = null;
        for (int i = 0; i < 8; i++) {
            str += (char)a[i];
        }
        return str;
    }

    private EncryptResponse encryptData(KeyObject keyObject, byte[] data, CryptMode cryptMode, ObjectType alg) {
        EncryptRequest encryptRequest = new EncryptRequest();
        encryptRequest
                .alg(alg)
                .plain(data)
                .mode(cryptMode);
        try {
            EncryptionAndDecryptionApi encryptionAndDecryptionApi = new EncryptionAndDecryptionApi(this.apiClient);
            return encryptionAndDecryptionApi.encrypt(keyObject.getKid(), encryptRequest);
        } catch (Exception e) {
            System.out.println("Encryption failed: " + e);
            return null;
        }
    }

    private KeyObject generateKey(List<KeyOperations> keyOperations, ObjectType objectType, String name, int keySize, boolean isTransientKey) {
        SobjectRequest sobjectRequest = new SobjectRequest()
                .name(name)
                .objType(objectType)
                .keySize(keySize)
                .keyOps(keyOperations)._transient(isTransientKey);

        try {
            SecurityObjectsApi securityObjectsApi = new SecurityObjectsApi(this.apiClient);
//            System.out.println(keyObject.getName());
//            System.out.println(keyObject.getKid());
            return securityObjectsApi.generateSecurityObject(sobjectRequest);
        } catch (Exception e) {
            System.out.println("Generating key failed: " + e);
            return null;
        }
    }

    private static String base64Url(String string) {
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(byte[] bytes) {
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String generateJws(HashMap<String, String> input, String header) {
        try {
            String jws_header = base64Url(header);
            String jws_payload = base64Url(input.get("payload"));
            String jws_signing_input = jws_header + "." + jws_payload;

            SignRequest signRequest = new SignRequest();
            signRequest
                    .hashAlg(DigestAlgorithm.SHA256)
                    .mode((SignatureMode) (new SignatureMode()).pkCS1V15(new Object()))
                    .data(jws_signing_input.getBytes());

            SignAndVerifyApi signAndVerifyApi = new SignAndVerifyApi(this.apiClient);
            SignResponse signResponse = signAndVerifyApi.sign(input.get("key"), signRequest);
            String jws_signature_value = base64Url(signResponse.getSignature());
            return jws_header + "." + jws_payload + "." + jws_signature_value;
        } catch (Exception e) {
            System.out.println("Generating JWS failed: " + e);
            return null;
        }
    }

    private static String generateHeader() {
        return "{\"cty\":\"JWT\",\"enc\":\"A256CBC-HS512\",\"alg\":\"RSA-OAEP-256\"}";
    }

    public String encrypt(HashMap<String, String> input) throws Exception {
        String header = generateHeader();
        String jws = generateJws(input, header);
//        List<KeyOperations> aesKeyOperations = new ArrayList<KeyOperations>(Arrays.asList(KeyOperations.ENCRYPT, KeyOperations.DECRYPT, KeyOperations.EXPORT));
//        KeyObject aesKey = generateKey(aesKeyOperations, ObjectType.AES, UUID.randomUUID().toString(), 256, false);
        EncryptResponse aesEncryptResponse = encryptData(this.aesKeyId, jws.getBytes(), CryptMode.CBC, ObjectType.AES);
//        List<KeyOperations> hmacKeyOperations = new ArrayList<KeyOperations>(Arrays.asList(KeyOperations.MACGENERATE, KeyOperations.MACVERIFY, KeyOperations.EXPORT));
//        KeyObject hmacKey = generateKey(hmacKeyOperations, ObjectType.HMAC, UUID.randomUUID().toString(), 256, false);

        String aad = base64Url(header);
        String al = computeLength(aad);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(aad.getBytes());
        output.write(aesEncryptResponse.getIv());
        output.write(aesEncryptResponse.getCipher());
        output.write(al.getBytes());
        byte[] hmac_input = output.toByteArray();

        MacGenerateResponse macGenerateResponse = performMac(this.hmacKeyId, hmac_input, DigestAlgorithm.SHA512);
        byte[] digest = truncateDigest(macGenerateResponse.getMac());

//        KeyObject certificate = createCertificate(input.get("cert"));

        byte[] exportedAesKey = this.aesKeyId.getValue();
        byte[] exportedHmacKey = this.hmacKeyId.getValue();
        ByteArrayOutputStream output2 = new ByteArrayOutputStream();
        output2.write(exportKey(this.hmacKeyId));
        output2.write(exportKey(this.aesKeyId));
        byte[] combinedKeys = output2.toByteArray();

        EncryptResponse encryptKeyResponse = encryptData(this.certificateObj, combinedKeys, CryptMode.OAEP_MGF1_SHA256, ObjectType.RSA);

        String jwe = base64Url(header) + "." + base64Url(encryptKeyResponse.getCipher()) + "." + base64Url(aesEncryptResponse.getIv()) + "." + base64Url(aesEncryptResponse.getCipher()) + "." + base64Url(digest);
//        System.out.println(jwe);
        return jwe;
    }
}
