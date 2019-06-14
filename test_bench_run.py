# Copyright (c) 2019 Fortanix Private Ltd.
# All Rights Reserved.
# Developed by : govardhan.yadava@fortanix.com

import urllib3
import os
import argparse
import subprocess
import re
import requests
import csv

my_path = os.path.abspath(os.path.dirname(__file__))
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
DEFAULT_API_ENDPOINT = "https://sdkms.test.fortanix.com"
csv_dict = dict()
THREAD_COUNTS = [10, 20, 50]
RSA_KEYSIZE = [1024, 2048, 3072, 4096, 5120, 6144, 7168, 8192]
AES_KEYSIZE = [128, 192, 256]
DES_KEYSIZE = [56]
DES3_KEYSIZE = [168]
ALGORITHMS = ['RSA', 'EC', 'AES', 'DES', 'DES3']
CIPHERMODES = [
    'ECB', 'CBC', 'CFB', 'CTR', 'GCM', 'CCM', 'CBCNOPAD', 'OFB', 'KW', 'KWP'
]
OPERATIONS = ['encryption', 'decryption', 'sign', 'verify', 'keygen']
DIGEST_ALG = ['SHA1', 'SHA256', 'SHA384', 'SHA512']
MACKEY_SIZES = [160, 256, 384, 512]
SERVER_VERSION = None

def parse_arguments():

    parser = argparse.ArgumentParser(description='SDKMS performance test bench test args')
    parser.add_argument(
        '--api-endpoint',
        default=os.getenv('FORTANIX_API_ENDPOINT', DEFAULT_API_ENDPOINT))
    parser.add_argument(
        '--api-key', default=os.getenv('FORTANIX_API_KEY', None))
    parser.add_argument(
        '--build',
        dest='build',
        default='build',
        choices=['clean', 'build'],
        help='Build or Clean the test bench')
    parser.add_argument(
        '--op', dest='operation',
        default=None,
        nargs="+",
        choices=OPERATIONS)
    parser.add_argument(
        '--algorithm',
        dest='algorithm',
        default=None,
        nargs="+",
        choices=ALGORITHMS)
    parser.add_argument(
        '--threadcount',
        dest='threadcount',
        default=None,
        nargs="+",
        choices=THREAD_COUNTS)
    parser.add_argument('--time', dest='time', default='120', choices=[120,300])
    parser.add_argument(
        '--hash-algorithm',
        dest='hashalgorithm',
        default='SHA1',
        choices=DIGEST_ALG)
    parser.add_argument(
        '--keysize',
        dest='keysize',
        default=None,
        nargs="+",
        type=int,
        choices=RSA_KEYSIZE + AES_KEYSIZE + DES_KEYSIZE+ DES3_KEYSIZE )
    parser.add_argument(
        '--mode', dest='mode', default=None, choices=CIPHERMODES)

    global cl_args, SERVER_VERSION
    cl_args = parser.parse_args()
    SERVER_VERSION = requests.get(str(cl_args.api_endpoint) + "/sys/v1/version", verify=False,
                                  allow_redirects=True).json()['version']
    if cl_args.api_key is None:
        print('No API key specified.')
        print('Please specify an API key via the --api-key option or '
              'FORTANIX_API_KEY')
        print('environment variable')
        exit(1)

def build_test_bench():
    # Deleting the Target
    cmd = 'bash test-bench.sh ' + cl_args.build
    proc = subprocess.Popen(
        cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    output = proc.communicate()[0]
    if re.search("FAILURE", output):
        print("BUILD FAILURE")
        exit(1)
    else:
        print(output)

def run_perf_tests():
    global cl_args
    ops = threads = keysizes = algorithms = list()
    if cl_args.operation == None:
        ops = OPERATIONS
    else:
        ops = cl_args.operation
    if cl_args.algorithm == None:
        algorithms = ['RSA', 'AES']
    else:
        algorithms = cl_args.algorithm
    if cl_args.threadcount == None:
        threads = THREAD_COUNTS
    else:
        threads =cl_args.threadcount
    for thread in threads:
        for op in ops:
            if op == 'encryption' or op == 'decryption' or op == 'keygen':
                for alg in algorithms:
                    if alg == 'RSA':
                        if cl_args.keysize == None:
                            run(op, 2048, str(alg), str(cl_args.time), str(thread))
                        else:
                            for key in cl_args.keysize:
                                if key in RSA_KEYSIZE:
                                    run(op, key, str(alg), str(cl_args.time), str(thread))
                                else:
                                    print("ERROR: invalid KEY size for RSA Algorithm")
                    elif alg == 'AES':
                        if cl_args.keysize == None:
                            run(op, 128, str(alg), str(cl_args.time), str(thread))
                        else:
                            for key in cl_args.keysize:
                                if key in AES_KEYSIZE:
                                    run(op, key, str(alg), str(cl_args.time), str(thread))
                                else:
                                    print("ERROR: invalid KEY size for AES Algorithm")
                    elif cl_args.keysize == DES_KEYSIZE:
                        run(op, cl_args.keysize[0], str(alg), str(cl_args.time), str(thread))
                    elif cl_args.keysize == DES3_KEYSIZE:
                        run(op, cl_args.keysize[0], str(alg), str(cl_args.time), str(thread))
                    else:
                        print('ERROR: Provide Appropriate key size')
            elif op == 'sign' or op == 'verify':
                for alg in algorithms:
                    if alg == 'RSA':
                        if cl_args.keysize == None:
                            run(op, 2048, str(alg), str(cl_args.time), str(thread))
                        else:
                            for key in cl_args.keysize:
                                if key in RSA_KEYSIZE:
                                    run(op, key, str(alg), str(cl_args.time), str(thread))
                                else:
                                    print("ERROR: invalid KEY size for RSA Algorithm")
            else:
                continue



def run(op, keysize, alg, time, thread):
    cmd = 'bash test-bench.sh run ' + op + ' --algorithm ' + alg + ' --keysize ' + str(keysize) + ' --time ' \
          + str(time) + ' --threadcount ' + str(thread)
    print("Executing:[ {} ]".format(cmd))
    if alg == 'AES':
        cmd+=' --mode CBC'
    filename = op.upper() + '-' + alg + '-' + str(keysize) + '-' + str(thread) + 'THREADS-' \
               + str(time) + 'SECONDS-AGGREGATE.csv'
    print("Result filename: [ {} ]".format(filename))
    proc = subprocess.Popen(
        cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    output = proc.communicate()[0]
    if re.search("FAILURE", output):
        print("BUILD FAILURE {}".format(cmd))
        print(output)
        exit(1)
    else:
        process_csv(os.getcwd() + '/target/jmeter/results/' + filename)
        print(output)



def process_csv(file):
    global SERVER_VERSION
    with open(file, mode='r') as csv_file:
        csv_reader = csv.DictReader(csv_file)
        for row in csv_reader:
            for k, v in row.items():
                if k == 'Throughput per sec' or k == 'average latency(ms)' or k == 'p90 latency(ms)':
                    csv_dict[k] = v
        csv_dict['SDKMS_OP'] = file.split('/')[-1].replace('-', '_').split('.')[0]
        write_to_csv(csv_dict, SERVER_VERSION)

def write_to_csv(csv_dict, version):
    with open(version + ".csv", 'a') as csvfile:
        fieldnames = ['SDKMS_OP', 'Throughput per sec', 'average latency(ms)', 'p90 latency(ms)']
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        if os.stat(version+".csv").st_size == 0:
            writer.writeheader()
        writer.writerow({k:csv_dict[k] for k in fieldnames})
    csvfile.close()

parse_arguments()
build_test_bench()
run_perf_tests()
