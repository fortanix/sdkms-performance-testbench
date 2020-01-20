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
import com.fortanix.sdkms.v1.model.EllipticCurve;
import com.fortanix.sdkms.v1.model.KeyObject;
import com.fortanix.sdkms.v1.model.ObjectType;
import com.fortanix.sdkms.v1.model.SobjectRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import java.sql.Timestamp;
import org.apache.jmeter.threads.JMeterContextService;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public class KeyGenerationSampler extends AbstractSDKMSSamplerClient {

    private SecurityObjectsApi securityObjectsApi;
    private List<String> keyIds = new ArrayList<>();

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        String algorithm = context.getParameter(ALGORITHM, "RSA");
        String keySize = context.getParameter(KEY_SIZE, "1024");
        boolean isTransientKey = Boolean.parseBoolean(context.getParameter(TRANSIENT, "false"));
        ObjectType objectType = ObjectType.fromValue(algorithm);
        SobjectRequest sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(objectType).enabled(true)._transient(isTransientKey);
        if (objectType == ObjectType.EC) {
            sobjectRequest.setEllipticCurve(EllipticCurve.fromValue(keySize));
        } else {
            sobjectRequest.setKeySize(Integer.parseInt(keySize));
        }
        this.securityObjectsApi = new SecurityObjectsApi(this.apiClient);
        String keyId = null;
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            KeyObject keyObject = (KeyObject) retryOperationIfSessionExpires(new RetryableOperation() {
                SecurityObjectsApi securityObjectsApi;
                SobjectRequest sobjectRequest;

                RetryableOperation init(SecurityObjectsApi securityObjectsApi, SobjectRequest sobjectRequest) {
                    this.securityObjectsApi = securityObjectsApi;
                    this.sobjectRequest = sobjectRequest;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.securityObjectsApi.generateSecurityObject(this.sobjectRequest);
                }
            }.init(this.securityObjectsApi, sobjectRequest));
            if (!isTransientKey) {
                keyId = keyObject.getKid();
            }
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in creating key : " + e.getMessage());
            result.setSuccessful(false);
        }
        result.sampleEnd();
        if (StringUtils.isNotEmpty(keyId)) {
            keyIds.add(keyId);
        }
        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        this.login(); /* Forcefully re-authenticates thread in case the idle-timeout has kicked
                         in and expires the session */

        for (String keyId : keyIds) {
            try {
                this.securityObjectsApi.deleteSecurityObject(keyId);

                /* Adding a logging statement that prints the key being deleted, at random intervals
                   to give the User a sense of progress */

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                if (((timestamp.getTime() - JMeterContextService.getTestStartTime())/5) % 9 == 0)
                    LOGGER.log(Level.INFO, "Deleted key : " + keyId);

            } catch (ApiException e) {
                LOGGER.log(Level.INFO, "failure in deleting key : " + e.getMessage(), e);
            }
        }
        super.teardownTest(context);
    }
}
