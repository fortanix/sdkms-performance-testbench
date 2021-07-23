import argparse
from OpenSSL import SSL, crypto
from socket import socket
import sys
import crypt
import os
import urllib
import shutil
import random
import secrets
import string
import requests
import base64

if not os.path.exists('certificates'):
    os.makedirs('certificates')
else:
    shutil.rmtree('certificates')
    os.makedirs('certificates')

if not os.path.exists('keystore'):
    os.makedirs('keystore')
else:
    shutil.rmtree('keystore')
    os.makedirs('keystore')


def get_certificate(host, port, cert_file_pathname):
    s = socket()
    context = SSL.Context(SSL.TLSv1_2_METHOD)
    print('Connecting to {0} to get certificate...'.format(host))
    conn = SSL.Connection(context, s)
    certs = []

    try:
        conn.connect((host, port))
        conn.do_handshake()
        certs = conn.get_peer_cert_chain()

    except SSL.Error as e:
        print('Error: {0}'.format(str(e)))
        exit(1)

    try:
        for index, cert in enumerate(certs):
            cert_components = dict(cert.get_subject().get_components())
            if(sys.version_info[0] >= 3):
                cn = (cert_components.get(b'CN')).decode('utf-8')
            else:
                cn = cert_components.get('CN')
            print('Centificate {0} - CN: {1}'.format(index, cn))

            try:
                temp_certname = '{0}_{1}.crt'.format(cert_file_pathname, index)
                with open(temp_certname, 'w+') as output_file:
                    if(sys.version_info[0] >= 3):
                        output_file.write((crypto.dump_certificate(
                            crypto.FILETYPE_PEM, cert).decode('utf-8')))
                    else:
                        output_file.write(
                            (crypto.dump_certificate(crypto.FILETYPE_PEM, cert)))
            except IOError:
                print('Exception:  {0}'.format(IOError.strerror))

        certificates = os.listdir('certificates/')[1:]
        with open('certificates/cert_cn.crt', 'w') as outfile:
            for certificate in certificates:
                with open('certificates/'+certificate) as infile:
                    outfile.write(infile.read())

        for certificate in certificates:
            os.remove('certificates/'+certificate)
    except SSL.Error as e:
        print('Error: {0}'.format(str(e)))
        exit(1)


# Default endpoint to get certificate
CERTIFICATE_ENDPOINT = "sdkms.test.fortanix.com"


if __name__ == '__main__':
    desc = "Performance Bench Setup Script"

    parser = argparse.ArgumentParser(description=desc)
    parser.add_argument(
        "-e", "--endpoint", help="Endpoint that you want to generate certificates for")
    parser.add_argument(
        "-u", "--email", help="Email ID used to login/register on the endpoint")
    parser.add_argument(
        "-p", "--password", help="Password used to login/register on the endpoint")
    parser.add_argument(
        "-c", "--create", help="Flag to create new user with given email and password", action="store_true")
    parser.add_argument(
        "-a", "--account", help="Account ID to use for creating API key")
    parser.add_argument(
        "-g", "--group", help="Group ID to use for creating API key")
    parser.add_argument(
        "-x", "--app", help="App ID to use for creating API key")
    args = parser.parse_args()

    if args.endpoint:
        endpoint = args.endpoint
        url = urllib.parse.urlparse(endpoint)
        netloc = url.netloc or url.path
        CERTIFICATE_ENDPOINT = netloc.split('/')[0]

    domain = '.'.join(CERTIFICATE_ENDPOINT.split('.')[-2:])

    user_details = {
        'create': True,
        'email': ''.join(secrets.choice(
            string.ascii_uppercase + string.digits)for i in range(10)) + '@' + domain,
        'password': 'admin@123',
        'first_name': 'Test User',
        'last_name': 'Postman',
        'account_name': ''.join(secrets.choice(
            string.ascii_uppercase + string.digits)for i in range(10)) + 'TestAccount',
        'group_name': ''.join(secrets.choice(
            string.ascii_uppercase + string.digits)for i in range(10)) + 'TestGroup',
        'app_name': ''.join(secrets.choice(
            string.ascii_uppercase + string.digits)for i in range(10)) + 'TestApp'
    }

    if args.email and not args.password:
        print('Error: Add password using -p/--password')
        exit(1)

    if args.password and not args.password:
        print('Error: Add email using -u/--email')
        exit(1)

    if args.email and args.password and not args.create:
        user_details['create'] = False
        user_details['email'] = args.email
        user_details['password'] = args.password

    if args.create:
        if not args.email and not args.password:
            print(
                'Error: Cannot create account without email and password (Add -u and -p)')
            exit(1)
        user_details['email'] = args.email
        user_details['password'] = args.password

    if args.account:
        if not args.email and not args.password:
            print(
                'Error: Cannot authenticate without email and password (Add -u and -p)')
            exit(1)

    if args.group:
        if not args.email and not args.password:
            print(
                'Error: Cannot authenticate without email and password (Add -u and -p)')
            exit(1)

    if args.app:
        if not args.email and not args.password:
            print(
                'Error: Cannot authenticate without email and password (Add -u and -p)')
            exit(1)

    get_certificate(CERTIFICATE_ENDPOINT, 443, "certificates/cert")

    try:
        os.system('openssl crl2pkcs7 -nocrl -certfile {} -certfile {} -out certificates/outfile.p7b'.format(
            'certificates/cert_0.crt', 'certificates/cert_cn.crt'))
        os.system(
            'openssl pkcs7 -print_certs -in certificates/outfile.p7b -out certificates/certificate.cer')
    except:
        print('Error: openssl pem to pkcs7 conversion error')
        exit(1)

    KEYSTORE_PASSWORD = 'password'
    KEYSTORE_ALIAS = 'smartkey' + str(random.randint(0, 1000))

    try:
        os.system('keytool -import -trustcacerts -file certificates/certificate.cer -keystore keystore/keystore1.jks -storepass {} -alias {} -noprompt'.format(KEYSTORE_PASSWORD, KEYSTORE_ALIAS))
    except:
        print('Error: keytool error. Unable to generate Keystore')
        exit(1)

    API_ENDPOINT = 'https://'+CERTIFICATE_ENDPOINT

    test_email = user_details['email']

    test_password = user_details['password']

    if user_details['create']:
        first_name = user_details['first_name']
        last_name = user_details['last_name']

        user_creation_request = {
            'user_email': test_email,
            'user_password': test_password,
            'first_name': first_name,
            'last_name': last_name,
            'recaptcha_response': '03AOP2lf58wnS5HqYUreAVBIKqGKMFTJJ_ZO5-1qdZkzAu-upL9nvCCGx9AKit4yzZWDoMLnjjHbs71SjKEDfiNeXKggD7K0OiWjABVErMlt0zruF1VlBh3Wa_uWNbBvGKzNh4dYLthra_V7lwOsPPS0mP1EXPhMp9BVLRCZOtHl6wYZKpjWbDHvQtW3YQGl2Y11YqVAKdekSmT6r_Kct3uuESk5Iltmg34j9HsJ8ONuoo4bzn7moy3SOjE060XyGqu5z2VPv3oVPwEhQdqOM2sbAg9ZdMNwUMgw16e4uRKeJsf45xlGyg6WU',
        }

        try:
            user_creation_response = requests.post(
                API_ENDPOINT+'/sys/v1/users', json=user_creation_request)
        except:
            print('Error: User creation request failed.')
            exit(1)

    if user_creation_response.status_code >= 400:
        print(user_creation_response.text)
        print('Status - {} | Error: User creation request failed.'.format(
            user_creation_response.status_code))
        exit(1)

    basic_token = test_email + ':' + test_password
    basic_token = basic_token.encode('ascii')
    basic_token = base64.b64encode(basic_token)
    basic_token = basic_token.decode('ascii')

    try:
        user_auth_response = requests.post(
            API_ENDPOINT+'/sys/v1/session/auth', headers={'Authorization': 'Basic {}'.format(basic_token)})
    except:
        print('Error: User auth request failed.')
        exit(1)

    if user_auth_response.status_code >= 400:
        print(user_auth_response.text)
        print('Status - {} | Error: User auth request failed.'.format(user_auth_response.status_code))
        exit(1)

    access_token = user_auth_response.json()['access_token']
    user_account_id = ''

    if not args.account:
        account_name = user_details['account_name']
        user_account_request = {
            'name': account_name
        }

        try:
            user_account_response = requests.post(API_ENDPOINT+'/sys/v1/accounts', headers={
                'Authorization': 'Bearer {}'.format(access_token)}, json=user_account_request)
        except:
            print('Error: User auth request failed.')
            exit(1)

        user_account_id = user_account_response.json()['acct_id']
    else:
        user_account_id = args.account

    user_account_select_req = {
        'acct_id': user_account_id
    }

    try:
        user_account_select_res = requests.post(API_ENDPOINT+'/sys/v1/session/select_account', headers={
                                                'Authorization': 'Bearer {}'.format(access_token)}, json=user_account_select_req)
    except:
        print('Error: User auth request failed.')
        exit(1)

    group_id = ''

    if not args.group:
        group_name = user_details['group_name']
        group_creation_request = {
            'name': group_name,
            'description': 'Created using API',
            'acct_id': user_account_id
        }

        try:
            group_creation_response = requests.post(API_ENDPOINT+'/sys/v1/groups', headers={
                                                    'Authorization': 'Bearer {}'.format(access_token)}, json=group_creation_request)
        except:
            print('Error: User auth request failed.')
            exit(1)

        group_id = group_creation_response.json()['group_id']
    else:
        group_id = args.group

    app_id = ''

    if not args.app:
        app_name = user_details['app_name']
        app_creation_request = {
            'name': app_name,
            'add_groups': [group_id],
            'default_group': group_id
        }

        try:
            app_creation_response = requests.post(API_ENDPOINT+'/sys/v1/apps', headers={
                'Authorization': 'Bearer {}'.format(access_token)}, json=app_creation_request)
        except:
            print('Error: User auth request failed.')
            exit(1)

        app_id = app_creation_response.json()['app_id']
    else:
        app_id = args.app

    try:
        app_secret_response = requests.get(API_ENDPOINT+'/sys/v1/apps/' + app_id + '/credential', headers={
            'Authorization': 'Bearer {}'.format(access_token)})
    except:
        print('Error: User auth request failed.')
        exit(1)

    app_secret = app_secret_response.json()['credential']['secret']
    app_api_key = app_id + ':' + app_secret
    app_api_key = app_api_key.encode('ascii')
    app_api_key = base64.b64encode(app_api_key)
    app_api_key = app_api_key.decode('ascii')

    with open('env', 'w') as env_file:
        env_file.write('export SDKMS_API_KEY={}\n'.format(app_api_key))
        env_file.write('export FORTANIX_API_KEY={}\n'.format(app_api_key))
        env_file.write('export SDKMS_API_ENDPOINT={}\n'.format(API_ENDPOINT))
        env_file.write(
            'export FORTANIX_API_ENDPOINT={}\n'.format(API_ENDPOINT))
        env_file.write('export SDKMS_SSL_TRUST_STORE={}\n'.format(
            'keystore/keystore1.jks'))
        env_file.write('export FORTANIX_SSL_TRUST_STORE={}\n'.format(
            'keystore/keystore1.jks'))
