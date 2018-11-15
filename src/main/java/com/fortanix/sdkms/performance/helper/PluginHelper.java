/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.helper;

import com.fortanix.sdkms.performance.payload.RequestPayload;

import java.util.List;

public abstract class PluginHelper {

    private PluginType pluginType;
    private String pluginId;

    PluginHelper(PluginType pluginType, String pluginId) {
        this.pluginType = pluginType;
        this.pluginId = pluginId;
    }

    public abstract RequestPayload createRequestPayload(String keyId, int radix, String tweak, String data);

    public abstract RequestPayload createRequestPayload(String keyId, int radix, String tweak, List<String> data);

    public String getPluginId() {
        return pluginId;
    }
}
