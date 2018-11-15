/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortanix.sdkms.performance.helper.PluginFactory;
import com.fortanix.sdkms.performance.helper.PluginHelper;
import com.fortanix.sdkms.performance.payload.RequestPayload;
import com.fortanix.sdkms.performance.util.RandomStringGenerator;
import com.fortanix.sdkms.v1.ApiException;
import com.fortanix.sdkms.v1.api.PluginsApi;
import com.fortanix.sdkms.v1.api.SecurityObjectsApi;
import com.fortanix.sdkms.v1.model.ObjectType;
import com.fortanix.sdkms.v1.model.PluginInvokeRequest;
import com.fortanix.sdkms.v1.model.SobjectRequest;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.security.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginSampler extends AbstractSDKMSSamplerClient {

    public static final Logger LOGGER = Logger.getLogger(PluginSampler.class.getName());

    String keyId;
    private SecurityObjectsApi securityObjectsApi;

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.setProperty("radix", "10");
        arguments.setProperty("tweakLength", "10");
        arguments.setProperty("dataLength", "10");
        arguments.setProperty("batchSize", "1");
        return arguments;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        super.setupTest(context);
        this.securityObjectsApi = new SecurityObjectsApi(this.apiClient);
        SobjectRequest sobjectRequest = new SobjectRequest().name(UUID.randomUUID().toString()).objType(ObjectType.AES).keySize(256);
        try {
            this.keyId = this.securityObjectsApi.generateSecurityObject(sobjectRequest).getKid();
        } catch (ApiException e) {
            LOGGER.log(Level.INFO, "failure in creating key : " + e.getMessage(), e);
            throw new ProviderException(e.getMessage());
        }
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        // select the pluginHelper type
        String pluginType = context.getParameter("pluginType");
        String pluginId = context.getParameter("pluginId");

        // get the pluginHelper
        PluginHelper pluginHelper = PluginFactory.getPlugin(pluginType, pluginId);

        // generate test case
        int radix;
        if (context.containsParameter("radix")) {
            try {
                radix = context.getIntParameter("radix");
            } catch (NumberFormatException e) {
                radix = getDefaultParameters().getPropertyAsInt("radix");
            }
        } else {
            radix = getDefaultParameters().getPropertyAsInt("radix");
        }
        int tweakLength;
        if (context.containsParameter("tweakLength")) {
            try {
                tweakLength = context.getIntParameter("tweakLength");
            } catch (NumberFormatException e) {
                tweakLength = getDefaultParameters().getPropertyAsInt("tweakLength");
            }
        } else {
            tweakLength = getDefaultParameters().getPropertyAsInt("tweakLength");
        }
        int dataLength;
        if (context.containsParameter("dataLength")) {
            try {
                dataLength = context.getIntParameter("dataLength");
            } catch (NumberFormatException e) {
                dataLength = getDefaultParameters().getPropertyAsInt("dataLength");
            }
        } else {
            dataLength = getDefaultParameters().getPropertyAsInt("dataLength");
        }
        int batchSize;
        if (context.containsParameter("batchSize")) {
            try {
                batchSize = context.getIntParameter("batchSize");
            } catch (NumberFormatException e) {
                batchSize = getDefaultParameters().getPropertyAsInt("batchSize");
            }
        } else {
            batchSize = getDefaultParameters().getPropertyAsInt("batchSize");
        }
        String tweak = RandomStringGenerator.generateTweak(radix, tweakLength);

        List<String> dataList = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            dataList.add(RandomStringGenerator.generateData(radix, dataLength));
        }

        // create pluginHelper request payload
        RequestPayload requestPayload = pluginHelper.createRequestPayload(this.keyId, radix, tweak, dataList);

        SampleResult result = new SampleResult();

        // serialize request payload object as JSON string
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = null;
        try {
            jsonString = mapper.writeValueAsString(requestPayload);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.INFO, "failure in serializing request payload object as JSON string: " + e.getMessage(), e);
            return result;
        }

        // invoke a pluginHelper
        PluginsApi pluginsApi = new PluginsApi(this.apiClient);

        result.sampleStart();

        try {
            retryOperationIfSessionExpires(new RetryableOperation() {
                PluginsApi pluginsApi;
                String pluginId;
                String input;

                RetryableOperation init(PluginsApi pluginsApi, String pluginId, String input) {
                    this.pluginsApi = pluginsApi;
                    this.pluginId = pluginId;
                    this.input = input;
                    return this;
                }

                @Override
                public Object execute() throws ApiException {
                    return this.pluginsApi.invokePlugin(this.pluginId, new PluginInvokeRequest(this.input));
                }
            }.init(pluginsApi, pluginHelper.getPluginId(), jsonString));
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in invoking plugin : " + e.getMessage());
            result.setSuccessful(false);
        }

        result.setSampleCount(batchSize);
        result.sampleEnd();

        return result;
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
