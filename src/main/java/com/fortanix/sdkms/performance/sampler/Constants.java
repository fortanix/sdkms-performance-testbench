/* Copyright (c) Fortanix, Inc.
 *
 * Licensed under the GNU General Public License, version 2 <LICENSE-GPL or
 * https://www.gnu.org/licenses/gpl-2.0.html> or the Apache License, Version
 * 2.0 <LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0>, at your
 * option. This file may not be copied, modified, or distributed except
 * according to those terms. */

package com.fortanix.sdkms.performance.sampler;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

class Constants {

    static final String ENV_SDKMS_SERVER_URL = "SDKMS_API_ENDPOINT";
    static final String ENV_SDKMS_API_KEY = "SDKMS_API_KEY";
    static final String TRUST_STORE_ENV_VAR = "SDKMS_SSL_TRUST_STORE";

    static final List<Integer> INVALID_SESSION_CODES = Arrays.asList(HttpURLConnection.HTTP_UNAUTHORIZED);
    static final List<String> INVALID_SESSION_MESSAGES = Arrays.asList("Session does not exist");

    static final String ALGORITHM = "algorithm";
    static final String KEY_SIZE = "keySize";
    static final String TRANSIENT = "transient";
    static final String MODE = "mode";
    static final String FILE_PATH = "filePath";
    static final String HASH_ALGORITHM = "hashAlgorithm";
    static final String BATCH_SIZE = "batchSize";
}
