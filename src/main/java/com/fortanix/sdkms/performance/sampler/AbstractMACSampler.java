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
import com.fortanix.sdkms.v1.model.DigestAlgorithm;
import com.fortanix.sdkms.v1.model.MacGenerateRequest;
import com.fortanix.sdkms.v1.model.ObjectType;
import com.fortanix.sdkms.v1.model.SobjectRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProviderException;
import java.util.UUID;
import java.util.logging.Level;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public abstract class AbstractMACSampler extends AbstractSDKMSSamplerClient {

    String keyId;
    MacGenerateRequest macGenerateRequest;
    DigestApi digestApi;
    DigestAlgorithm hashAlgorithm;
    String text;
    private SecurityObjectsApi securityObjectsApi;

    @Override
    public void setupTest(JavaSamplerContext context) {
        String algorithm = context.getParameter(ALGORITHM, "HMAC");
        String keySize = context.getParameter(KEY_SIZE, "160");
        String hashAlgorithm = context.getParameter(HASH_ALGORITHM, "SHA1");
        String filePath = context.getParameter(FILE_PATH);
        ObjectType objectType = ObjectType.fromValue(algorithm);
        this.hashAlgorithm = DigestAlgorithm.fromValue(hashAlgorithm);
        String input = "random-text";
        if (StringUtils.isNotEmpty(filePath)) {
            try {
                input = new String(Files.readAllBytes(Paths.get(filePath)));
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "failure in reading input from file : " + e.getMessage(), e);
                throw new ProviderException(e.getMessage());
            }
        }
        this.text = input;
        super.setupTest(context);
        this.securityObjectsApi = new SecurityObjectsApi(this.apiClient);
        SobjectRequest sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(objectType).keySize(Integer.parseInt(keySize));
        try {
            this.keyId = this.securityObjectsApi.generateSecurityObject(sobjectRequest).getKid();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in creating key : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        this.macGenerateRequest = new MacGenerateRequest().alg(this.hashAlgorithm).data(this.text.getBytes());
        this.digestApi = new DigestApi(this.apiClient);
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
