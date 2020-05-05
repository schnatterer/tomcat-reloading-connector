#!/usr/bin/env bash

CERT_VALIDITY_DAYS=${CERT_VALIDITY_DAYS:-30}

BASEDIR=$(dirname $0)
ABSOLUTE_BASEDIR="$( cd ${BASEDIR} && pwd )"

set -o errexit -o nounset -o pipefail

createSelfSignedCert() {
    
    certDir=${BASEDIR}/target/certs
    cert=crt.pem
    pk=pk.pem
    ca=ca.crt.pem
    host=localhost
    ipAddress='127.0.0.1'

    mkdir -p ${certDir}

    if [[ ! -f "${cert}" ]]; then

        echo "Creating and trusting self-signed certificate for host ${host}"

        cd "${certDir}"
        
        # Create CA
        openssl req -newkey rsa:4096 -keyout ca.pk.pem -x509 -new -nodes -out ${ca} \
          -subj "/OU=Unknown/O=Unknown/L=Unknown/ST=unknown/C=DE"  -days "${CERT_VALIDITY_DAYS}"

        subjectAltName="$(printf "subjectAltName=IP:%s,DNS:%s" "${ipAddress}" "${host}")"
        openssl req -new -newkey rsa:4096 -nodes -keyout ${pk} -out csr.pem \
               -subj "/CN=${host}/OU=Unknown/O=Unknown/L=Unknown/ST=unknown/C=DE" \
               -config <(cat /etc/ssl/openssl.cnf <(printf "\n[SAN]\n%s" "${subjectAltName}"))

        # Sign Cert
        # Due to a bug in openssl, extensions are not transferred to the final signed x509 cert
        # https://www.openssl.org/docs/man1.1.0/man1/x509.html#BUGS
        # So add them while signing. The one added with "req" will probably be ignored.
        openssl x509 -req -in csr.pem -CA ${ca} -CAkey ca.pk.pem -CAcreateserial -out ${cert} -days "${CERT_VALIDITY_DAYS}" \
                -extensions v3_ca -extfile <(printf "\n[v3_ca]\n%s" "${subjectAltName}")
    fi
}

createSelfSignedCert