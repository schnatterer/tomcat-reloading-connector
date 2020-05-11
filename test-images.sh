#!/usr/bin/env bash
# Simple smoke test for all container images / examples
set -o errexit -o nounset -o pipefail

DEBUG=${DEBUG:-''}
[[ ! -z "${DEBUG}" ]] && set -x

function main () {
    
    docker build -t tomcat-reloading-connector-spring .
    testImage 'tomcat-reloading-connector-spring'
    
    docker build -t tomcat-reloading-connector-embedded --build-arg=FLAVOR=embedded-tomcat .
    testImage 'tomcat-reloading-connector-embedded'
    
    docker build -t 'tomcat-reloading-connector-standalone' --build-arg=FLAVOR=standalone-tomcat .
    testImage 'tomcat-reloading-connector-standalone'
}

function testImage() {
    
    CONTAINER=$(docker run --rm -d "$1")
    CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${CONTAINER})
    
    # Check Container running
    docker container inspect ${CONTAINER} > /dev/null
     # Wait for container to be ready
    sleep 7
    
    BEFORE=$(queryCertValidity "${CONTAINER_IP}")
    
    docker exec ${CONTAINER} /createCerts.sh
    sleep 5
    
    AFTER=$(queryCertValidity "${CONTAINER_IP}")
    [[ "${BEFORE}" != "${AFTER}" ]]
    
    curl -k --silent --fail "https://${CONTAINER_IP}:8443" > /dev/null
    
    # Print logs for debugging
    [[ ! -z "${DEBUG}" ]] && docker logs ${CONTAINER}
    docker stop ${CONTAINER}
}

function queryCertValidity() {
    echo | \
     openssl s_client -showcerts -servername "$1" -connect "$1:8443" 2>/dev/null | \
     openssl x509 -inform pem -noout -text | grep -A2 Validity
}

main "$@"