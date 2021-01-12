package com.fortanix.sdkms.performance.sampler.jce;

import com.fortanix.sdkms.jce.provider.SdkmsJCE;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;

public abstract class JCEBaseSampler extends AbstractJavaSamplerClient {

    static final Logger LOGGER = Logger.getLogger(JCEBaseSampler.class.getName());

    // Loading SDKMS JCE provider by reflection to make sure test bench compiles even when
    // SDKMS JCE jar not being present.
    @Override
    public void setupTest(JavaSamplerContext context) {
        Security.addProvider(new SdkmsJCE());
    }
}