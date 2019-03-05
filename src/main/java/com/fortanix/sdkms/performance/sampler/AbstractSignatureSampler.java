/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.SecurityObjectsApi;
import com.fortanix.sdkms.v1.api.SignAndVerifyApi;
import com.fortanix.sdkms.v1.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.UUID;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public abstract class AbstractSignatureSampler extends AbstractSDKMSSamplerClient {

    static final DigestAlgorithm HASH_ALGORITHM = DigestAlgorithm.SHA1;
    String keyId;
    SignAndVerifyApi signAndVerifyApi;
    SignRequest signRequest;
    SignRequestEx signRequestEx;
    BatchSignRequest batchSignRequest;
    byte[] hash;
    private SecurityObjectsApi securityObjectsApi;

    @Override
    public void setupTest(JavaSamplerContext context) {
        String algorithm = context.getParameter(ALGORITHM, "RSA");
        String keySize = context.getParameter(KEY_SIZE, "1024");
        String filePath = context.getParameter(FILE_PATH);
        int batchSize = context.getIntParameter(BATCH_SIZE, 0);

        ObjectType objectType = ObjectType.fromValue(algorithm);
        String input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(HASH_ALGORITHM.toString());
        } catch (NoSuchAlgorithmException e) {
            // suppressed
        }
        md.update(input.getBytes());
        this.hash = md.digest();
        super.setupTest(context);
        this.securityObjectsApi = new SecurityObjectsApi(this.apiClient);
        SobjectRequest sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(objectType);
        if (objectType == ObjectType.EC) {
            sobjectRequest.setEllipticCurve(EllipticCurve.fromValue(keySize));
        } else {
            sobjectRequest.setKeySize(Integer.parseInt(keySize));
        }
        try {
            this.keyId = this.securityObjectsApi.generateSecurityObject(sobjectRequest).getKid();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in creating key : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.signRequest = new SignRequest().hashAlg(HASH_ALGORITHM).hash(this.hash);
        this.signRequestEx = new SignRequestEx().hashAlg(HASH_ALGORITHM).hash(this.hash).key(new SobjectDescriptor().kid(this.keyId));
        if (batchSize != 0){
            this.batchSignRequest = new BatchSignRequest();
            for ( int i = 0; i < batchSize ; i++ ) {
                this.batchSignRequest.add(this.signRequestEx);
            }
        }
        this.signAndVerifyApi = new SignAndVerifyApi(this.apiClient);
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            this.securityObjectsApi.deleteSecurityObject(this.keyId);
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in deleting key : " + e.getMessage(), e);
        }
        super.teardownTest(context);
    }
}
