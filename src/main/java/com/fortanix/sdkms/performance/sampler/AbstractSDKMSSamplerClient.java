/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiClient;
import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.AuthenticationApi;
import com.fortanix.sdkms.v1.auth.ApiKeyAuth;
import com.fortanix.sdkms.v1.model.AuthResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.security.ProviderException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.fortanix.sdkms.performance.sampler.Constants.*;

public abstract class AbstractSDKMSSamplerClient extends AbstractJavaSamplerClient {

    static final Logger LOGGER = Logger.getLogger(AbstractSDKMSSamplerClient.class.getName());

    ApiClient apiClient;
    private AuthenticationApi authenticationApi;

    protected void login() {
        AuthResponse authResponse = null;
        try {
            authResponse = this.authenticationApi.authorize();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in logging in : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) this.apiClient.getAuthentication("bearerToken");
        apiKeyAuth.setApiKey(authResponse.getAccessToken());
        apiKeyAuth.setApiKeyPrefix("Bearer");
    }

    public void setupTest(JavaSamplerContext context) {
        this.apiClient = new ApiClient();
        Map<String, String> envVars = System.getenv();
        String basePath = envVars.get(ENV_SDKMS_SERVER_URL);
        if (StringUtils.isBlank(basePath)) {
            throw new ProviderException("Missing Environment Variable: " + ENV_SDKMS_SERVER_URL);
        }
        String basicAuthString = envVars.get(ENV_SDKMS_API_KEY);
        if (StringUtils.isBlank(basicAuthString)) {
            throw new ProviderException("Missing Environment Variable: " + ENV_SDKMS_API_KEY);
        }
        // configure trust store for server certificates (optional)
        if (envVars.containsKey(TRUST_STORE_ENV_VAR)) {
            System.setProperty("javax.net.ssl.trustStore", envVars.get(TRUST_STORE_ENV_VAR));
        }
        this.apiClient.setBasePath(basePath);
        this.apiClient.setBasicAuthString(basicAuthString);
        this.authenticationApi = new AuthenticationApi(this.apiClient);
        this.login();
    }

    private boolean isValidSession(ApiException e) {
        return !(INVALID_SESSION_CODES.contains(e.getCode()) || INVALID_SESSION_MESSAGES.contains(e.getMessage()));
    }

    Object retryOperationIfSessionExpires(RetryableOperation command) throws ApiException {
        try {
            return command.execute();
        } catch (ApiException e) {
            if (this.isValidSession(e)) {
                throw e;
            } else {
                LOGGER.info("Session has expired, trying to re-login");
                this.login();
                return command.execute();
            }
        }
    }

    public void teardownTest(JavaSamplerContext context) {
        try {
            this.authenticationApi.terminate();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in logging out : " + e.getMessage(), e);
        }
    }
}
