/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import com.fortanix.sdkms.v1.ApiException;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import java.util.HashMap;

public class JWTGenerationSampler extends AbstractJWTSampler {

    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            HashMap<String, String> map = new HashMap<>();
            map.put("operation", "encrypt");
            map.put("payload" , "hello world");
            map.put("key" , "0b8572eb-1489-4335-b904-60d69095d493");
            map.put("cert" , AbstractJWTSampler.cert);

            String jwe = encrypt(map);
            LOGGER.info("JWE: " + jwe);
            result.setSuccessful(true);
        } catch (ApiException e) {
            LOGGER.info("failure in generating signature : " + e.getMessage());
            result.setSuccessful(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        result.sampleEnd();
        return result;
    }
}
