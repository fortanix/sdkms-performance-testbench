# SDKMS Rest Performance Test Bench
---

# Overview
Performance test bench for SDKMS using REST API interface. It generates reports in HTML and CSV format. SDKMS REST performance test bench dynamically generates the JMeter JMX file and provides configurable way to specify thread count and time duration to run the operation.

**Note:** Added support for SDKMS JCE provider as the client for operations, in 0.2.0 version. For more information on JCE provider, visit [Support Site](https://support.fortanix.com/hc/en-us/articles/360018362951-JCE)

Performance Test Bench has been tested on Ubuntu 16.04 server.

For more information on SDKMS visit [SDKMS Site](https://fortanix.com/products/sdkms/) .

# Prerequisites
* [JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [maven](https://maven.apache.org/download.cgi)

## Manual environment setup
* Following environment variables need to be set:

	 `FORTANIX_API_KEY=<Fortanix SDKMS API KEY>`

	 [Optional] `FORTANIX_API_ENDPOINT=<SDKMS URL>`

	 [Optional] `SDKMS_SSL_TRUST_STORE`=<Path to SSL Trust Store>

	Example:

		export FORTANIX_API_KEY="OS00ZDQN2U...............aWdZVnp4UVBSX2JSWE"
		export FORTANIX_API_ENDPOINT="https://sdkms.fortanix.com"

	> FORTANIX\_API\_ENDPOINT is optional and default to https://apps.sdkms.fortanix.com .
 	> SDKMS\_SSL\_TRUST_STORE is needed only for on-premises deployments.

## Automatic environment setup
### *Dependencies*
* [Python3](https://www.python.org/downloads/)

### *Usage*
Basic usage with random user, account details and test endpoint to export the env file. Default endpoint used will be [sdkms.test.fortanix.com](sdkms.test.fortanix.com)
```
python setup_script.py
```

Optional Arguments to create a user with particular email and password or using existing account ID, group ID or app ID.

```
-h, --help
		Show the help messages

-e, --endpoint
		Endpoint to be used for fetching certificates, generating keystore and calling APIs

-u, --email
		Email ID to be used for signing up/logging in

-p, --password
		Password to be used for signing up/logging in

-c, --create
		Flag to create new user with given email and password

-a, --account
		Account ID to be used for generating API Key

-g, --group
		Group ID to be used for generating API Key

-x, --app
		App ID to be used for generating API Key
```

### *Example*

* Run the script with optional arguments, if any

```
python setup_script.py -e sdkms.test.fortanix.com
```

* It will generate an env file, which can be sourced to set all necessary environment variables

```
source ./env
```

# Setup performance Test bench
## Build
* Clone the repository or download the zip.
* Execute:

	`#./test-bench.sh build`

* For more information execute:

	> `#./test-bench.sh --help`

	> `#./test-bench.sh run --help`

## Clean up
To delete the generated artifacts, execute below command:

	#./test-bench.sh clean


# Supported Operations (REST API interface)
## AES Key Generation
Supported key sizes are 128, 192 and 256.

	Example:
	# test-bench.sh run keygen --algorithm AES --keysize 128 --transient true --threadcount 5 --time 30

## RSA Key Generation
Supported key sizes are between 1024 to 8192.

	Example:
	# test-bench.sh run keygen --algorithm RSA --keysize 1024 --transient true --threadcount 5 --time 30

## EC Key Generation
For EC key --keysize are SecP192K1, SecP224K1, SecP256K1, NistP192, NistP224, NistP256, NistP384 and NistP521.

	Example:
	# test-bench.sh run keygen --algorithm EC --transient true

## AES Encryption
AES encryption is supported for all key sizes (128, 192 and 256) and
supported modes are CBC, GCM and FPE.

	Example:
	# test-bench.sh run encryption --algorithm AES --keysize 128 --mode GCM --filepath filepath

The final metrics shows sample time for encryption.

## AES Decryption
AES decryption is supported for all key sizes (128, 192 and 256) and
supported modes are CBC, GCM and FPE.

	Example:
	# test-bench.sh run decryption --algorithm AES --keysize 128 --mode GCM --filepath filepath

The final metrics shows sample time for decryption.

## RSA Encryption
RSA encryption is supported for all key sizes (1024 to 8192).

	Example:
	# test-bench.sh run encryption --algorithm RSA --keysize 2048 --mode GSM --filepath filepath

The final metrics shows sample time for encryption.

## RSA Decryption
RSA decryption is supported for all key sizes (1024 to 8192).

	Example:
	# test-bench.sh run encryption --algorithm RSA --keysize 2048

The final metrics shows sample time for decryption.

## RSA Sign and Verification
RSA signing and verification is supported for all key sizes.

	Example:
	# test-bench.sh run sign --algorithm RSA --keysize 2048
	# test-bench.sh run verify --algorithm RSA --keysize 2048

## EC Sign and Verification
We can capture EC sign and verify performance metrics as below:

	Example:
	# test-bench.sh run sign --algorithm EC
	# test-bench.sh run verify --algorithm EC
	
## Plugin Invocation
We can capture Plugin performance metrics as below:

	Example:
	# test-bench.sh run plugin --pluginId <plugin-id> --pluginType <plugin-type> --threadcount 10 --time 120

> All above operation run by default with 50 thread for 5 minutes. All above operation support --threadcount and --time to overwrite the default thread count and duration.

> Key generated during operation get deleted at the end of operation.

# Supported Operations (JCE provider)
## AES Encryption
AES encryption is supported for all key sizes (128, 192 and 256) and all modes (CBC, GCM, ECB, etc) using JCE Provider Cipher interface. 
**Note:** These are singlepart Cipher operations.   

        Example:
        # test-bench.sh run encryption --algorithm AES --keysize 128 --mode GCM --interface jce --filepath filepath

## AES Decryption
AES decryption is supported for all key sizes (128, 192 and 256) and all modes (CBC, GCM, ECB, etc) using JCE Provider Cipher interface.
**Note:** These are singlepart Cipher operations.

        Example:
        # test-bench.sh run decryption --algorithm AES --keysize 128 --mode GCM --interface jce --filepath filepath


# Report Location
Let's assume we have cloned the repo at /opt/rest-api.

- HTML

	`/opt/rest-api/target/jmeter/reports/`

- CSV

	`/opt/rest-api/target/jmeter/results/`
	
# Adding File Identifiers
Sometimes when running multiple sequential test cases that are similar in nature, it is possible that the output report files may get overwritten when the tool saves them with the same name. To avoid this, we can add a file identifier with the 'file-idf' parameter (optional) to distinguish them from one another:

	Example:
		# test-bench.sh run plugin --pluginId <plugin-id> --pluginType <plugin-type> --threadcount 10 --time 120 --file-idf encryptPluginResult
		# test-bench.sh run plugin --pluginId <plugin-id> --pluginType <plugin-type> --threadcount 10 --time 120 --file-idf decryptPluginResult

# Sample Output
operation | threads | duration(sec) | total operations | average latency(ms) | p90 latency(ms) | p99 latency(ms) | min latency (ms) | max latency (ms) | Error % | Throughput per sec | capacity (kb/s)
------------ | ------------- | ------------ | ------------- | ------------ | ------------- | ------------ | ------------- | ------------ | ------------- | ------------ | -------------
SDKMS Signature Generation | 1 | 300 | 16020 | 13 | 21 | 26 | 6 | 154 | 0.00% | 53.4 | NA

* capacity columns is only supported for AES Encryption/Decryption with provided filepath.


# Contributing

We gratefully accept bug reports and contributions from the community.
By participating in this community, you agree to abide by [Code of Conduct](./CODE_OF_CONDUCT.md).
All contributions are covered under the Developer's Certificate of Origin (DCO).

## Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
have the right to submit it under the open source license
indicated in the file; or

(b) The contribution is based upon previous work that, to the best
of my knowledge, is covered under an appropriate open source
license and I have the right under that license to submit that
work with modifications, whether created in whole or in part
by me, under the same open source license (unless I am
permitted to submit under a different license), as indicated
in the file; or

(c) The contribution was provided directly to me by some other
person who certified (a), (b) or (c) and I have not modified
it.

(d) I understand and agree that this project and the contribution
are public and that a record of the contribution (including all
personal information I submit with it, including my sign-off) is
maintained indefinitely and may be redistributed consistent with
this project or the open source license(s) involved.

# License

This project is primarily distributed under the terms of the Mozilla Public License (MPL) 2.0, see [LICENSE](./LICENSE) for details.
