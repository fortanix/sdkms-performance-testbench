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

public class Constants {

    public static final String ENV_SDKMS_SERVER_URL = "FORTANIX_API_ENDPOINT";
    public static final String ENV_SDKMS_API_KEY = "FORTANIX_API_KEY";
    public static final String TRUST_STORE_ENV_VAR = "SDKMS_SSL_TRUST_STORE";

    public static final List<Integer> INVALID_SESSION_CODES = Arrays.asList(HttpURLConnection.HTTP_UNAUTHORIZED);
    public static final List<String> INVALID_SESSION_MESSAGES = Arrays.asList("Session does not exist");

    public static final String KEYNAME = "keyName";
    public static final String ALGORITHM = "algorithm";
    public static final String KEY_SIZE = "keySize";
    public static final String TRANSIENT = "transient";
    public static final String MODE = "mode";
    public static final String FILE_PATH = "filePath";
    public static final String HASH_ALGORITHM = "hashAlgorithm";
    public static final String BATCH_SIZE = "batchSize";
}
