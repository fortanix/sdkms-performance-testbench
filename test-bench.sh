#!/bin/bash

set -eo pipefail
  
if [ ! -f $PWD/env ]; then
   echo "env file missing. Run python3.7 ./setup_script.py - to create"
   exit
fi
source $PWD/env

# Redirect messages to stderr
function error() {
    (>&2 echo "[E] $*")
}

function info() {
    (>&2 echo "[I] $*")
}

ERROR=1
SUCCESS=0
HELP="--help"
RUN="run"
SCRIPT_NAME="test-bench.sh"
TB_OPERATIONS=(build_task clean_task run_task)
RUN_OPERATIONS=(keygen_task encryption_task decryption_task sign_task verify_task plugin_task)
THREAD_COUNT_TEXT="##ThreadCount##"
EXECUTION_TIME_TEXT="##ExecutionTime##"
ALGORITHM_TEXT="##Algorithm##"
HASH_ALGORITHM_TEXT="##HashAlgorithm##"
KEYSIZE_TEXT="##Keysize##"
TRANSIENT_TEXT="##Transient##"
FILEPATH_TEXT="##FilePath##"
MODE_TEXT="##Mode##"
KEYNAME_TEXT="##Keyname##"
CIPHER_TEXT="##Cipher##"
IV_TEXT="##Iv##"
PLAIN_TEXT="##Plain##"
PLUGIN_TYPE_TEXT="##pluginType##"
PLUGIN_ID_TEXT="##pluginId##"
RADIX_TEXT="##radix##"
TWEAK_LENGTH_TEXT="##tweakLength##"
DATA_LENGTH_TEXT="##dataLength##"
BATCH_SIZE_TEXT="##batchSize##"
SDKMS_REST_API_HOME=${SDKMS_REST_API_HOME:-${PWD}}

THREAD_COUNT="5"
EXECUTION_TIME="30"
ALGORITHM="RSA"
HASH_ALGORITHM="SHA1"
KEYSIZE="1024"
TRANSIENT="false"
FILEPATH=""
MODE=""
KEYNAME=""
CIPHER=""
IV=""
PLAIN=""
MAX_FILE=""
PLUGIN_TYPE=""
PLUGIN_ID=""
RADIX=""
TWEAK_LENGTH=""
DATA_LENGTH=""
BATCH_SIZE=""
JVM_ARGS_INPUT=""
JVM_ARGS=""
FILE_IDF=$(date +%F-%H_%M_%S)
INTERFACE="sdk"

#Function to update jmx text using value
function update_jmx(){
    rm -rf $SDKMS_REST_API_HOME"/target/jmeter/testFiles/"
    rm -rf $SDKMS_REST_API_HOME"/target/jmx/"
    mkdir $SDKMS_REST_API_HOME"/target/jmx"
    cp $SDKMS_REST_API_HOME$1 $SDKMS_REST_API_HOME$2
    JMX_FILE=$SDKMS_REST_API_HOME$2
    #Replacing params based on input/default
    sed -i.bak s/$THREAD_COUNT_TEXT/$THREAD_COUNT/g $JMX_FILE
    sed -i.bak s/$EXECUTION_TIME_TEXT/$EXECUTION_TIME/g $JMX_FILE
    sed -i.bak s/$ALGORITHM_TEXT/$ALGORITHM/g $JMX_FILE
    sed -i.bak s/$HASH_ALGORITHM_TEXT/$HASH_ALGORITHM/g $JMX_FILE
    sed -i.bak s/$KEYSIZE_TEXT/$KEYSIZE/g $JMX_FILE
    sed -i.bak s/$TRANSIENT_TEXT/$TRANSIENT/g $JMX_FILE
    sed -i.bak s/$MODE_TEXT/$MODE/g $JMX_FILE
    sed -i.bak s/$KEYNAME_TEXT/$KEYNAME/g $JMX_FILE
    sed -i.bak s/$PLAIN_TEXT/$PLAIN/g $JMX_FILE
    sed -i.bak s/$CIPHER_TEXT/$CIPHER/g $JMX_FILE
    sed -i.bak s/$IV_TEXT/$IV/g $JMX_FILE
    if [[ "${MAX_FILE}" == "true" ]]; then
        FILEPATH=$SDKMS_REST_API_HOME"/src/test/resources/LargeFile.txt"
    fi
    sed -i.bak 's|'$FILEPATH_TEXT'|'$FILEPATH'|g' $JMX_FILE
    sed -i.bak s/$PLUGIN_TYPE_TEXT/$PLUGIN_TYPE/g $JMX_FILE
    sed -i.bak s/$PLUGIN_ID_TEXT/$PLUGIN_ID/g $JMX_FILE
    sed -i.bak s/$RADIX_TEXT/$RADIX/g $JMX_FILE
    sed -i.bak s/$TWEAK_LENGTH_TEXT/$TWEAK_LENGTH/g $JMX_FILE
    sed -i.bak s/$DATA_LENGTH_TEXT/$DATA_LENGTH/g $JMX_FILE
    sed -i.bak s/$BATCH_SIZE_TEXT/$BATCH_SIZE/g $JMX_FILE
}

#Function to validate environment variables
function validate(){
if [ -z "${FORTANIX_API_KEY}" ]; then
  error "FORTANIX_API_KEY is not set"
  exit ${ERROR_SDKMS_API_KEY};
fi

if [ -z "${FORTANIX_API_ENDPOINT}" ]; then
  info "FORTANIX_API_ENDPOINT is not set. Using default endpoint : https://apps.sdkms.fortanix.com"
  FORTANIX_API_ENDPOINT="https://apps.sdkms.fortanix.com"
else
  info "Using endpoint: ${FORTANIX_API_ENDPOINT}"
fi

SDKMS_REST_API_JAR=$SDKMS_REST_API_HOME"/target/sdkms-jmeter-sampler-0.2.0.jar"
if [ -f "$SDKMS_REST_API_JAR" ]; then
  info "Performance test artifacts present for test execution"
else
  error "Performance test artifacts not found for test execution"
  info "Please execute ./test-bench.sh build and run the test again"
  exit ${ERROR_PERFORMANCE_ARTIFACTS};
fi
}

#Function to extract JVM args from input string
function update_jvm_args() {
  if [ ! -z "${JVM_ARGS_INPUT}" ]; then
    JVM_ARGS=$(sed "s/,/ /g" <<<$JVM_ARGS_INPUT)
  fi
}

#Function to extract input paraameters
function get_input(){
while [ $# -gt 0 ]; do
  case "$1" in
    --interface)
      INTERFACE="$2"
      ;;
    --algorithm)
      ALGORITHM="$2"
      ;;
    --keysize)
      KEYSIZE="$2"
      ;;
    --threadcount)
      THREAD_COUNT="$2"
      ;;
    --time)
      EXECUTION_TIME="$2"
      ;;
    --transient)
      TRANSIENT="$2"
      ;;
    --filepath)
      FILEPATH="$2"
      ;;
    --hash-algorithm)
      HASH_ALGORITHM="$2"
      ;;
    --mode)
      MODE="$2"
      ;;
    --keyname)
      KEYNAME="$2"
      ;;
    --plain)
      PLAIN="$2"
      ;;
    --cipher)
      CIPHER="$2"
      ;;
    --iv)
      IV="$2"
      ;;
    --max-file)
      MAX_FILE="$2"
      ;;
    --pluginType)
      PLUGIN_TYPE="$2"
      ;;
    --pluginId)
      PLUGIN_ID="$2"
      ;;
    --radix)
      RADIX="$2"
      ;;
    --tweakLength)
      TWEAK_LENGTH="$2"
      ;;
    --dataLength)
      DATA_LENGTH="$2"
      ;;
    --batchSize)
      BATCH_SIZE="$2"
      ;;
    --jvm-args)
      JVM_ARGS_INPUT="$2"
      ;;
	--file-idf)
      FILE_IDF="$2"
      ;;
      *)
  esac
  shift
done

update_jvm_args
}

#Function to print start of test
function print_start(){
    info "#######################################################################################"
    info "Running Jmeter for operation: "$OPERATION" using algorithm: "$ALGORITHM", keysize: "$KEYSIZE", mode:"$MODE", interface: "$INTERFACE", inputFilePath: "$FILEPATH", transient: "$TRANSIENT", Threadcount: "$THREAD_COUNT", time: "$EXECUTION_TIME
    info "#######################################################################################"
}

function print_end(){
    info "#######################################################################################"
    info "End of Jmeter Test"
    info "#######################################################################################"

    HTML_OUTPUT_DIR=$(ls -td -- $SDKMS_REST_API_HOME"/target/jmeter/reports/"* | head -n 1)
    CSV_OUTPUT_FILE=$(ls -td -- $SDKMS_REST_API_HOME"/target/jmeter/results/"* | head -n 1)
    FILE_IDF_SUB="$FILE_IDF"
    if [ ! -z "$MODE" ]
    then
        FILE_IDF_SUB="$MODE"
    fi
    if [ -z "$BATCH_SIZE" ]
    then
        AGGREGATE_OUTPUT_FILE=$SDKMS_REST_API_HOME"/target/jmeter/results/"$2"-"$ALGORITHM"-"$KEYSIZE"-"$INTERFACE"-"$THREAD_COUNT"THREADS-"$EXECUTION_TIME"SECONDS-"$FILE_IDF_SUB"-AGGREGATE.csv"
    else
        AGGREGATE_OUTPUT_FILE=$SDKMS_REST_API_HOME"/target/jmeter/results/"BATCH""$2"-"$ALGORITHM"-"$KEYSIZE"-"$INTERFACE"-"$THREAD_COUNT"THREADS-"$EXECUTION_TIME"SECONDS-BATCHSIZE-"$BATCH_SIZE"-"$FILE_IDF_SUB"-AGGREGATE.csv"
    fi
    java -jar $SDKMS_REST_API_HOME"/target/jmeter/lib/ext/cmdrunner-2.0.jar" --tool Reporter --generate-csv $AGGREGATE_OUTPUT_FILE --input-jtl $CSV_OUTPUT_FILE --plugin-type AggregateReport

    BANDWIDTH="NA"
    if [[ ( "$2" == "ENCRYPTION" ) || ( "$2" == "DECRYPTION" ) ]] && [[ "${FILEPATH}" ]];
    then
       FILE_SIZE=$(wc -c < $FILEPATH)
       OPERATIONS=$(awk -F, 'NR>2 { print $2 ; }' $AGGREGATE_OUTPUT_FILE)
       BANDWIDTH=$(($(($OPERATIONS*$FILE_SIZE)) / $((1024*$EXECUTION_TIME))))
    fi

    OP_NAME=$AGGREGATE_OUTPUT_FILE
    TRIM_PATTERN="-AGGREGATE.csv"
    OP_NAME=${AGGREGATE_OUTPUT_FILE/$TRIM_PATTERN/}
    OP_NAME=$(echo $OP_NAME | awk -F "/" '{print $NF}')

    awk -v THREADS="$THREAD_COUNT" -v CAPACITY="$BANDWIDTH" -v TIME="$EXECUTION_TIME" -v OP_NAME="$OP_NAME" 'BEGIN { FS=","; OFS="," }; FNR == 1 { print "operation","threads","duration(sec)","total operations","average latency(ms)","p90 latency(ms)","p99 latency(ms)","min latency (ms)","max latency (ms)","Error %","Throughput per sec","Cipher capacity (kb/s)" }; FNR  ==2 { print OP_NAME,THREADS,TIME,$2,$3,$5,$7,$8,$9,$10,$11,CAPACITY }' $AGGREGATE_OUTPUT_FILE > temp.csv && mv temp.csv $AGGREGATE_OUTPUT_FILE;

    mv $HTML_OUTPUT_DIR $SDKMS_REST_API_HOME"/target/jmeter/reports/"$2"-"$ALGORITHM"-"$KEYSIZE"-"$INTERFACE"-"$THREAD_COUNT"THREADS-"$EXECUTION_TIME"SECONDS-"$FILE_IDF_SUB"-HTML"
    mv $CSV_OUTPUT_FILE $SDKMS_REST_API_HOME"/target/jmeter/results/"$2"-"$ALGORITHM"-"$KEYSIZE"-"$INTERFACE"-"$THREAD_COUNT"THREADS-"$EXECUTION_TIME"SECONDS-"$FILE_IDF_SUB"-DATA.csv"
    info "Html output can be found at "$HTML_OUTPUT_DIR"/index.html"
    info "csv output can be found at "$AGGREGATE_OUTPUT_FILE
}

function tb_operation() {
    if [ -z "$1" ]; then
       ARG1="--help"
    else
       ARG1=$1
    fi
    # convert to lower-case
    key=$(echo "$ARG1" | tr '[:upper:]' '[:lower:]')
    case ${key} in
        build)
            opt="build_task"
            ;;
        clean)
            opt="clean_task"
            ;;
        --help)
            opt="help_task"
            ;;
        run)
        opt="run_task"
         ;;
        merge)
        opt="merge_task"
         ;;
        *)
            return ${ERROR}
    esac
    shift
    echo "${opt}"
}

function build_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo " build compiles the REST API performance test bench and install the performance artifacts in target directory"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} build"
        return
    fi
    info "Build operation is selected"
    mvn install:install-file
    mvn install -DskipTests;
}

function clean_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo " clean deletes the performance artifacts and target directory."
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} clean"
        return
    fi
    info "Clean operation is selected"
    mvn clean;
}

function keygen_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "keygen captures metrics for RSA,EC,AES,DES,DES3 and Tokenization key Creation"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run keygen [--algorithm RSA|AES|EC|DES|DES3] [--keysize 1024|2048] [--interface sdk|jce] [--transient true|false] [--threadcount 50|100] [--time 300|600]"
        echo "   options:"
        echo "   --algorithm    Key creation algorithm. Supported algorithms are RSA,AES,DES,DES3 and EC."
        echo "                  default value is RSA."
        echo "   --keysize      keysize or curve name for the provided algorithm. Supported values:"
        echo "                  RSA: '1024 to 8192'"
        echo "                  AES: '128, 192, or 256'"
        echo "                  DES : 56"
        echo "                  DES3: 168"
        echo "                  EC curves: SecP192K1, SecP224K1, SecP256K1, NistP192, NistP224, NistP256, NistP384, NistP521"
        echo "                  default value is 1024."
        echo "   --interface    interface for performing the operation. Values: sdk, jce"
        echo "                  Default interface is sdk."
        echo "   --transient    true|false . In order to test transient key generation. Default is false."
        echo "   --threadcount  Number of concurrent threads per second to be executed."
        echo "                  default value is 50."
        echo "   --time         Time in seconds to hold the jmeter execution."
        echo "                  default value is 300."
		echo "   --file-idf     A distinct identifier to add to the output CSV file for easy identification."
        echo "                  helps in cases when there are multiple consecutive executions of the same operation."
		echo "                  prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Keygen operation is selected"
    FILE_NAME=SDKMS_REST_API_KEYGEN
    OPERATION=KEYGEN
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/key-generate-${INTERFACE}-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function encryption_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "encryption captures metrics for RSA,DES,DES3,AES and Tokenization key Encryption"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run encryption [--algorithm RSA|AES|DES|DES3] [--keysize 1024|2048] [--interface sdk|jce] [--mode CBC] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600]"
        echo "   options:"
        echo "   --algorithm    Encryption algorithm. Supported algorithms are RSA,DES,DES3 and AES."
        echo "                  default value is RSA."
        echo "   --keysize      keysize for the provided algorithm."
        echo "                  Supported keysize RSA: '1024 to 8192', AES: '128, 192, or 256', DES: 56 and DES3: 168."
        echo "                  default value is 1024."
        echo "   --interface    interface for performing the operation. Values: sdk, jce"
        echo "                  Default interface is sdk."
        echo "   --mode         [Optional] encryption mode, only for AES algorithm. Values: ECB, CBC, CBCNOPAD, CFB, CTR, GCM, CCM, FPE"
        echo "                  Default value is CBC."
        echo "   --batchsize    Create a batch encrypt request with the provided batch size."
        echo "                  Default is 0 (non batch) single verify request."
        echo "   --filepath     Input plain text file to use for encryption. By default a random string is used"
        echo "   --threadcount  Number of concurrent threads per second to be executed."
        echo "                  default value is 50."
        echo "   --time         Time in seconds to hold the jmeter execution."
        echo "                  default value is 300."
		echo "   --file-idf     A distinct identifier to add to the output CSV file for easy identification."
        echo "                  helps in cases when there are multiple consecutive executions of the same operation."
		echo "                  prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Encryption operation is selected"
        FILE_NAME=SDKMS_REST_API_ENCRYPTION
    OPERATION=ENCRYPTION
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/encrypt-${INTERFACE}-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function decryption_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "decryption captures metrics for RSA,DES,DES3,AES and Tokenization key Decryption"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run decryption [--algorithm RSA|AES|DES|DES3] [--keysize 1024|2048] [--interface sdk|jce] [--mode CBC] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600]"
        echo "   options:"
        echo "   --algorithm    Decryption algorithm. Supported algorithms are RSA and AES."
        echo "                  default value is RSA."
        echo "   --keysize      keysize for the provided algorithm."
        echo "                  Supported keysize RSA: '1024 to 8192', AES: '128, 192, or 256', DES: 56 and DES3: 168."
        echo "                  default value is 1024."
        echo "   --interface    interface for performing the operation. Values: sdk, jce"
        echo "                  Default interface is sdk."
        echo "   --mode         [Optional] encryption mode, only for AES algorithm. Values: ECB, CBC, CBCNOPAD, CFB, CTR, GCM, CCM, FPE"
        echo "                  Default value is CBC."
        echo "   --batchsize    Create a batch decrypt request with the provided batch size."
        echo "                  Default is 0 (non batch) single verify request."
        echo "   --filepath     Input plain text file, the cipher of which to be used for decryption. By default a random string is used"
        echo "   --threadcount  Number of concurrent threads per second to be executed."
        echo "                  default value is 50."
        echo "   --time         Time in seconds to hold the jmeter execution."
        echo "                  default value is 300."
		echo "   --file-idf     A distinct identifier to add to the output CSV file for easy identification."
        echo "                  helps in cases when there are multiple consecutive executions of the same operation."
		echo "                  prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Decryption operation is selected"
    FILE_NAME=SDKMS_REST_API_DECRYPTION
    OPERATION=DECRYPTION
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/decrypt-${INTERFACE}-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function valentino_encryption_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "decryption captures metrics for RSA,DES,DES3,AES and Tokenization key Decryption"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run decryption [--algorithm RSA|AES|DES|DES3] [--keysize 1024|2048] [--interface sdk|jce] [--mode CBC] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600]"
        echo "   options:"
        echo "   --algorithm    Decryption algorithm. Supported algorithms are RSA and AES."
        echo "                  default value is RSA."
        echo "   --keyName      keysize for the provided algorithm."
        echo "                  Supported keysize RSA: '1024 to 8192', AES: '128, 192, or 256', DES: 56 and DES3: 168."
        echo "                  default value is 1024."
        echo "   --interface    interface for performing the operation. Values: sdk, jce"
        echo "                  Default interface is sdk."
        echo "   --mode         [Optional] encryption mode, only for AES algorithm. Values: ECB, CBC, CBCNOPAD, CFB, CTR, GCM, CCM, FPE"
        echo "                  Default value is CBC."
        echo "   --batchsize    Create a batch decrypt request with the provided batch size."
        echo "                  Default is 0 (non batch) single verify request."
        echo "   --filepath     Input plain text file, the cipher of which to be used for decryption. By default a random string is used"
        echo "   --threadcount  Number of concurrent threads per second to be executed."
        echo "                  default value is 50."
        echo "   --time         Time in seconds to hold the jmeter execution."
        echo "                  default value is 300."
		echo "   --file-idf     A distinct identifier to add to the output CSV file for easy identification."
        echo "                  helps in cases when there are multiple consecutive executions of the same operation."
		echo "                  prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Decryption operation is selected"
    FILE_NAME=SDKMS_REST_API_DECRYPTION
    OPERATION=DECRYPTION
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/encrypt-sdk-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function sign_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "sign captures metrics for RSA or EC signature generation"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run sign [--algorithm RSA|EC] [--keysize 1024|2048] [--interface sdk|jce] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600] [--batchsize 10|100|1000]"
        echo "   options:"
        echo "   --algorithm       Signature generation algorithm. Supported algorithms are RSA and EC"
        echo "                     default value is RSA."
        echo "   --keysize         keysize or curve name for signature generation algorithm. Supported keysize are 1024 to 8192"
        echo "                     EC supported curves: SecP192K1, SecP224K1, SecP256K1, NistP192, NistP224, NistP256, NistP384, NistP521"
        echo "                     default value is 1024."
        echo "   --interface       interface for performing the operation. Values: sdk, jce"
        echo "                     Default interface is sdk."
        echo "   --filepath        Input plain text file to use for signing. By default a random string is used"
        echo "   --hash-algorithm  Message digest algorithm. Supported algorithm are SHA1, SHA256, SHA384, SHA512"
        echo "   --threadcount     Number of concurrent threads per second to be executed."
        echo "                     default value is 50."
        echo "   --time            Time in seconds to hold the jmeter execution."
        echo "                     default value is 300."
        echo "   --batchsize       Create a batch sign request with the provided batch size."
        echo "                     Default is 0 (non batch) single sign request."
		echo "   --file-idf        A distinct identifier to add to the output CSV file for easy identification."
        echo "                     helps in cases when there are multiple consecutive executions of the same operation."
		echo "                     prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Sign operation is selected"
    FILE_NAME=SDKMS_REST_API_SIGN
    OPERATION=SIGN
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/sign-generate-${INTERFACE}-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function encrypt_valentino_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "sign captures metrics for RSA or EC signature generation"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run sign [--algorithm RSA|EC] [--keysize 1024|2048] [--interface sdk|jce] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600] [--batchsize 10|100|1000]"
        echo "   options:"
        echo "   --algorithm       Signature generation algorithm. Supported algorithms are RSA and EC"
        echo "                     default value is RSA."
        echo "   --keysize         keysize or curve name for signature generation algorithm. Supported keysize are 1024 to 8192"
        echo "                     EC supported curves: SecP192K1, SecP224K1, SecP256K1, NistP192, NistP224, NistP256, NistP384, NistP521"
        echo "                     default value is 1024."
        echo "   --interface       interface for performing the operation. Values: sdk, jce"
        echo "                     Default interface is sdk."
        echo "   --filepath        Input plain text file to use for signing. By default a random string is used"
        echo "   --hash-algorithm  Message digest algorithm. Supported algorithm are SHA1, SHA256, SHA384, SHA512"
        echo "   --threadcount     Number of concurrent threads per second to be executed."
        echo "                     default value is 50."
        echo "   --time            Time in seconds to hold the jmeter execution."
        echo "                     default value is 300."
        echo "   --batchsize       Create a batch sign request with the provided batch size."
        echo "                     Default is 0 (non batch) single sign request."
		echo "   --file-idf        A distinct identifier to add to the output CSV file for easy identification."
        echo "                     helps in cases when there are multiple consecutive executions of the same operation."
		echo "                     prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Sign operation is selected"
    FILE_NAME=SDKMS_REST_API_SIGN
    OPERATION=SIGN
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/encrypt-valentino-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function decrypt_valentino_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "sign captures metrics for RSA or EC signature generation"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run sign [--algorithm RSA|EC] [--keysize 1024|2048] [--interface sdk|jce] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600] [--batchsize 10|100|1000]"
        echo "   options:"
        echo "   --algorithm       Signature generation algorithm. Supported algorithms are RSA and EC"
        echo "                     default value is RSA."
        echo "   --keysize         keysize or curve name for signature generation algorithm. Supported keysize are 1024 to 8192"
        echo "                     EC supported curves: SecP192K1, SecP224K1, SecP256K1, NistP192, NistP224, NistP256, NistP384, NistP521"
        echo "                     default value is 1024."
        echo "   --interface       interface for performing the operation. Values: sdk, jce"
        echo "                     Default interface is sdk."
        echo "   --filepath        Input plain text file to use for signing. By default a random string is used"
        echo "   --hash-algorithm  Message digest algorithm. Supported algorithm are SHA1, SHA256, SHA384, SHA512"
        echo "   --threadcount     Number of concurrent threads per second to be executed."
        echo "                     default value is 50."
        echo "   --time            Time in seconds to hold the jmeter execution."
        echo "                     default value is 300."
        echo "   --batchsize       Create a batch sign request with the provided batch size."
        echo "                     Default is 0 (non batch) single sign request."
		echo "   --file-idf        A distinct identifier to add to the output CSV file for easy identification."
        echo "                     helps in cases when there are multiple consecutive executions of the same operation."
		echo "                     prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Sign operation is selected"
    FILE_NAME=SDKMS_REST_API_SIGN
    OPERATION=SIGN
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/decrypt-valentino-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function encrypt_valentino_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "sign captures metrics for RSA or EC signature generation"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run sign [--algorithm RSA|EC] [--keysize 1024|2048] [--interface sdk|jce] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600] [--batchsize 10|100|1000]"
        echo "   options:"
        echo "   --algorithm       Signature generation algorithm. Supported algorithms are RSA and EC"
        echo "                     default value is RSA."
        echo "   --keysize         keysize or curve name for signature generation algorithm. Supported keysize are 1024 to 8192"
        echo "                     EC supported curves: SecP192K1, SecP224K1, SecP256K1, NistP192, NistP224, NistP256, NistP384, NistP521"
        echo "                     default value is 1024."
        echo "   --interface       interface for performing the operation. Values: sdk, jce"
        echo "                     Default interface is sdk."
        echo "   --filepath        Input plain text file to use for signing. By default a random string is used"
        echo "   --hash-algorithm  Message digest algorithm. Supported algorithm are SHA1, SHA256, SHA384, SHA512"
        echo "   --threadcount     Number of concurrent threads per second to be executed."
        echo "                     default value is 50."
        echo "   --time            Time in seconds to hold the jmeter execution."
        echo "                     default value is 300."
        echo "   --batchsize       Create a batch sign request with the provided batch size."
        echo "                     Default is 0 (non batch) single sign request."
		echo "   --file-idf        A distinct identifier to add to the output CSV file for easy identification."
        echo "                     helps in cases when there are multiple consecutive executions of the same operation."
		echo "                     prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Sign operation is selected"
    FILE_NAME=SDKMS_REST_API_SIGN
    OPERATION=SIGN
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/encrypt-valentino-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function verify_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "verify captures metrics for RSA or EC signature verification"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run verify [--algorithm RSA|EC] [--keysize 1024|2048] [--interface sdk|jce] [--threadcount 50|100] [--time 300|600] [--batchsize 10|100|1000]"
        echo "   options:"
        echo "   --algorithm       Signature verification algorithm. Supported algorithms are RSA and EC"
        echo "                     default value is RSA."
        echo "   --keysize         keysize or curve name for signature verification algorithm. Supported keysize are 1024 to 8192"
        echo "                     EC supported curves: SecP192K1, SecP224K1, SecP256K1, NistP192, NistP224, NistP256, NistP384, NistP521"
        echo "                     default value is 1024."
        echo "   --interface       interface for performing the operation. Values: sdk, jce"
        echo "                     Default interface is sdk."
        echo "   --filepath        Custom plain text file, the signature of which will be used for verification. By default a random string is used"
        echo "   --hash-algorithm  Message digest algorithm. Supported algorithm are SHA1, SHA256, SHA384, SHA512"
        echo "   --threadcount     Number of concurrent threads per second to be executed."
        echo "                     default value is 50."
        echo "   --time            Time in seconds to hold the jmeter execution."
        echo "                     default value is 300."
        echo "   --batchsize       Create a batch verify request with the provided batch size."
        echo "                     Default is 0 (non batch) single verify request."
		echo "   --file-idf        A distinct identifier to add to the output CSV file for easy identification."
        echo "                     helps in cases when there are multiple consecutive executions of the same operation."
		echo "                     prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Verify operation is selected"
    FILE_NAME=SDKMS_REST_API_VERIFY
    OPERATION=VERIFY
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/sign-verify-${INTERFACE}-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function mac_generate_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "mac generate captures metrics for Hmac digest generation"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run mac-generate [--algorithm Hmac] [--hash-algorithm SHA1] [--keysize 160] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600]"
        echo "   options:"
        echo "   --algorithm       Mac generation algorithm. Supported algorithms are Hmac"
        echo "                     default value is Hmac."
        echo "   --hash-algorithm  hash algorithm. Supported algorithms are SHA1|SHA256|SHA384|SHA512"
        echo "                     default value is SHA1."
        echo "   --keysize         keysize for mac generation algorithm. Supported keysize are 160|256|384|512"
        echo "                     default value is 160."
        echo "   --filepath        Input plain text file to use for signing. By default a random string is used"
        echo "   --threadcount     Number of concurrent threads per second to be executed."
        echo "                     default value is 50."
        echo "   --time            Time in seconds to hold the jmeter execution."
        echo "                     default value is 300."
		echo "   --file-idf        A distinct identifier to add to the output CSV file for easy identification."
        echo "                     helps in cases when there are multiple consecutive executions of the same operation."
		echo "                     prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "mac generate operation is selected"
    FILE_NAME=SDKMS_REST_API_MAC_GENERATE
    OPERATION=MAC_GENERATE
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/mac-generate-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function mac_verify_task() {
    if [ "$1" == "${HELP}" ];
    then
        echo "mac verify captures metrics for Hmac digest verification"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run mac-verify [--algorithm Hmac] [--hash-algorithm SHA1] [--keysize 160] [--filepath </path/to/file>] [--threadcount 50|100] [--time 300|600]"
        echo "   options:"
        echo "   --algorithm       Mac verification algorithm. Supported algorithms are Hmac"
        echo "                     default value is Hmac."
        echo "   --hash-algorithm  hash algorithm. Supported algorithms are SHA1|SHA256|SHA384|SHA512"
        echo "                     default value is SHA1."
        echo "   --keysize         keysize for mac generation algorithm. Supported keysize are 160|256|384|512"
        echo "                     default value is 160."
        echo "   --filepath        Input plain text file to use for signing. By default a random string is used"
        echo "   --threadcount     Number of concurrent threads per second to be executed."
        echo "                     default value is 50."
        echo "   --time            Time in seconds to hold the jmeter execution."
        echo "                     default value is 300."
		echo "   --file-idf        A distinct identifier to add to the output CSV file for easy identification."
        echo "                     helps in cases when there are multiple consecutive executions of the same operation."
		echo "                     prevents the older CSV from getting overwritten by the new output file. Adds an epoch timestamp at the end of filename by default."
        echo ""
        echo "One can also pass proxy(http/https) related jvm args as a csv string: --jvm-args '-Dhttps.proxyHost=proxy,-Dhttps.proxyPort=8080,-Dhttps.proxyUser=user,-Dhttps.proxyPassword=pwd'"
        return
    fi
    info "Mac verify operation is selected"
    FILE_NAME=SDKMS_REST_API_MAC_VERIFY
    OPERATION=MAC_VERIFY
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/mac-verify-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function plugin_task() {
    info "Plugin operation is selected"
    FILE_NAME=SDKMS_REST_API_PLUGIN
    OPERATION=PLUGIN
    validate
    get_input ${@:1}
    update_jmx "/src/test/jmeter/plugin-template.jmx" "/target/jmx/"$FILE_NAME".jmx"
    print_start
    mvn verify -Djmx.path="target/jmx" $JVM_ARGS
    print_end $FILE_NAME $OPERATION
}

function help_task() {
    echo " REST API performance test bench"
    echo " test-bench.sh supports three subcommands, clean, build and run respectively"
    echo "    usage:"
    echo "      # ${SCRIPT_NAME} clean|build|run"
    echo " For more detail on subcommand, execute subcommand --help"
    echo "    example:"
    echo "      test-bench.sh run --help"
}

function run_task(){
    if [ "$1" == "${HELP}" ];
    then
        echo " runs the REST API performance test bench for various operations"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} run keygen|encryption|decryption|sign|verify|plugin"
        return
    fi
    task=""
    key=$(echo "$1" | tr '[:upper:]' '[:lower:]')
    case ${key} in
        encryption)
            task="encryption_task"
            ;;
        decryption)
            task="decryption_task"
            ;;
        keygen)
            task="keygen_task"
            ;;
        sign)
            task="sign_task"
            ;;
        verify)
            task="verify_task"
            ;;
        mac-generate)
            task="mac_generate_task"
            ;;
        mac-verify)
            task="mac_verify_task"
            ;;
        plugin)
            task="plugin_task"
            ;;
        encryption-valentino)
            task="valentino_encryption_task"
            ;;
        valentino-encryption)
            task="encrypt_valentino_task"
            ;;
        valentino-decryption)
            task="decrypt_valentino_task"
            ;;
        --help)
            echo " run supports four subcommands, encryption, keygen, sign and verify respectively."
            echo "    usage:"
            echo "      # ${SCRIPT_NAME} run encryption|keygen|sign|verify"
            echo " For more detail on subcommand, execute subcommand --help"
            echo "    example:"
            echo "      test-bench.sh run encryption --help"
        ;;
        *)
        error "Unsupported operation (${key})"
            return ${ERROR}
    esac

    "${task}" ${@:2}

}
function merge_task(){
    if [ "$1" == "${HELP}" ];
    then
        echo " merge merges the output aggregated csv files into a single csv file"
        echo "   usage:"
        echo "   # ${SCRIPT_NAME} merge [--input filename1.csv,filename2.csv..]"
        echo "   options:"
        echo "   --input    List of input files for merging. Comma seperated full path to file is expected"
        echo "              By default it will pick all aggregate csv files under $SDKMS_REST_API_HOME/target/jmeter/results"
        return
    fi

    info "Operation: merge"
    if [ -z "${2}" ]; then
      file_list=$( ls "${SDKMS_REST_API_HOME}/target/jmeter/results/"*AGGREGATE.csv 2> /dev/null )
    else
      IFS=',' read -r -a file_list <<< "$2"
    fi
    # check file_list is empty
    if [ -z "$file_list" ]; then
        error "No aggregated csv files found"
        return ${ERROR}
    fi
    current_time=$(date "+%Y.%m.%d-%H.%M.%S")
    out_file="${SDKMS_REST_API_HOME}/target/jmeter/results/MergeOutput-${current_time}.csv"
    i=0                                       # Reset a counter
    for filename in ${file_list[@]}; do
    if [ "$filename"  != "${out_file}" ] ;      # Avoid recursion
    then
      if [[ $i -eq 0 ]] ; then
        head -1  $filename >   ${out_file} # Copy header if it is the first file
      fi
      tail -n +2  $filename >>  ${out_file} # Append from the 2nd line each file
    i=$(( $i + 1 ))                        # Increase the counter
    fi
    done
    awk -F, '$1!="TOTAL"' ${out_file} > temp.csv && mv temp.csv ${out_file}
	info "All files have been merged successfully!"
}

opt=$(tb_operation $@)

if [ $? -eq 0 ];
then
    # Calling selcted task and pass remainig arguments to task
   ${opt} ${@:2}
else
   error "Unsupported operation ($1)"
   info "For more detail execute  ${SCRIPT_NAME} --help"
fi
