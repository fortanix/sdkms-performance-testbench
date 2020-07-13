/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.performance.helper.EncryptionDecryptionFactory;
import com.fortanix.sdkms.performance.helper.EncryptionDecryptionHelper;
import com.fortanix.sdkms.performance.helper.EncryptionDecryptionType;
import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.EncryptionAndDecryptionApi;
import com.fortanix.sdkms.v1.api.SecurityObjectsApi;
import com.fortanix.sdkms.v1.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProviderException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Arrays;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public abstract class AbstractEncryptionAndDecryptionSampler extends AbstractSDKMSSamplerClient {

    String keyId;
    EncryptRequest encryptRequest;
    EncryptRequestEx encryptRequestEx;
    EncryptionAndDecryptionApi encryptionAndDecryptionApi;
    EncryptionDecryptionHelper encryptionDecryptionHelper;
    BatchEncryptRequest batchEncryptRequest;
    private SecurityObjectsApi securityObjectsApi;

    @Override
    public void setupTest(JavaSamplerContext context) {
        String algorithm = context.getParameter(ALGORITHM, "RSA");
        int keySize = context.getIntParameter(KEY_SIZE, 1024);
        String mode = context.getParameter(MODE, "CBC");
        String filePath = context.getParameter(FILE_PATH);
        int batchSize = context.getIntParameter(BATCH_SIZE, 0);
        ObjectType objectType = ObjectType.fromValue(algorithm);
        String input = "random-text";
        if(CryptMode.fromValue(mode) == CryptMode.FPE) {
            input = "36088650107272";
        }
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }
        super.setupTest(context);
        this.securityObjectsApi = new SecurityObjectsApi(this.apiClient);
        SobjectRequest sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(objectType).keySize(keySize);
        if(CryptMode.fromValue(mode) == CryptMode.FPE) {
            // Standard fpe policy for Credit Card datatype has been fixed here.
            // TODO: To support more datatypes as input parameter.
            FpeOptions fpeOptions = new FpeOptions().radix(10).preserve(Arrays.asList(0,1,2,3,-1,-2,-3,-4)).luhnCheck(true).name("Credit Card");
            sobjectRequest.fpe(fpeOptions);
        }
        try {
            this.keyId = this.securityObjectsApi.generateSecurityObject(sobjectRequest).getKid();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in creating key : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.encryptionDecryptionHelper = EncryptionDecryptionFactory.getHelper(EncryptionDecryptionType.valueOf(algorithm), CryptMode.fromValue(mode));
        this.encryptRequestEx = this.encryptionDecryptionHelper.createEncryptRequest(new SobjectDescriptor().kid(this.keyId), input);
        this.encryptRequest = this.encryptionDecryptionHelper.createEncryptRequest(input);
        BatchEncryptRequestInner batchEncryptRequestInner = new BatchEncryptRequestInner().kid(this.keyId).request(encryptRequest);

        if (batchSize != 0){
            this.batchEncryptRequest = new BatchEncryptRequest();
            for ( int i = 0; i < batchSize ; i++ ) {
                this.batchEncryptRequest.add(batchEncryptRequestInner);
            }
        }
        this.encryptionAndDecryptionApi = new EncryptionAndDecryptionApi(this.apiClient);
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
