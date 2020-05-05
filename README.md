tomcat-reloading-connector
======
[![Build Status](https://travis-ci.org/schnatterer/tomcat-reloading-connector.svg?branch=master)](https://travis-ci.org/schnatterer/tomcat-reloading-connector)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=info.schnatterer.tomcat-reloading-connector%3Atomcat-reloading-connector-parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=info.schnatterer.tomcat-reloading-connector%3Atomcat-reloading-connector-parent)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=info.schnatterer.tomcat-reloading-connector%3Atomcat-reloading-connector-parent&metric=sqale_index)](https://sonarcloud.io/dashboard?id=info.schnatterer.tomcat-reloading-connector%3Atomcat-reloading-connector-parent)
[![](https://img.shields.io/microbadger/layers/schnatterer/tomcat-reloading-connector)](https://hub.docker.com/r/schnatterer/tomcat-reloading-connector)
[![](https://img.shields.io/docker/image-size/schnatterer/tomcat-reloading-connector)](https://hub.docker.com/r/schnatterer/tomcat-reloading-connector)

Tomcat connector that automatically reloads SSLConfig.

# How to use?

Right now, this only offers a specialized form of `org.apache.coyote.http11.Http11AprProtocol` that watches folder in 
which the first configured certificate resides for changes and reloads SSLConfig on change.
 
Http11AprProtocol means this only works with 
[Apache Portable Runtime (APR) based Native library for Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/apr.html).

[![Maven Central](https://img.shields.io/maven-central/v/info.schnatterer.tomcat-reloading-connector/reloading-connector.svg)](https://search.maven.org/search?q=a:reloading-connector%20AND%20g:info.schnatterer.tomcat-reloading-connector)

You can also get snapshot versions from our [snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/info/schnatterer/moby-names-generator/) 
(for the most recent commit).
To do so, add the following repo to your `pom.xml` or `settings.xml`:

```xml
<repository>
    <id>snapshots-repo</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <releases><enabled>false</enabled></releases>
    <snapshots><enabled>true</enabled></snapshots>
</repository>
```

## Tomcat

* Drop the reloading-connector.jar into your tomcat's library folder.
* Configure the `ReloadingHttp11AprProtocol` in your `server.xml`.
* Example: 
```xml
<Connector port="8443" protocol= "info.schnatterer.tomcat.ReloadingHttp11AprProtocol" SSLEnabled="true" >
    <UpgradeProtocol className="org.apache.coyote.http2.Http2Protocol" />
    <SSLHostConfig>
        <Certificate certificateKeyFile="path/privkey.pem"
         certificateFile="path/cert.pem"
         certificateChainFile="path/fullchain.pem"
         type="RSA" />
    </SSLHostConfig>
</Connector>
```
## Embedded Tomcat

* Add the `info.schnatterer.tomcat-reloading-connector:tomcat-reloading-connector:VERSION` dependency to your 
  embedded tomcat project.
* Create a `Connector` with the `ReloadingHttp11AprProtocol` and configure it.
* See [example](tomcat). 

# Try it

## Docker 
```bash
CONTAINER=$(docker run --rm -p8443:8443 -d schnatterer/tomcat-reloading-connector-example)
sleep 2

# View web app
curl -k https://localhost:8443
# View cert
echo | openssl s_client -showcerts -servername localhost -connect localhost:8443 2>/dev/null | openssl x509 -inform pem -noout -text | grep -A2 Validity

# Reload certs
docker exec ${CONTAINER} /app/createCerts.sh
# View new cert
sleep 5
echo | openssl s_client -showcerts -servername localhost -connect localhost:8443 2>/dev/null | openssl x509 -inform pem -noout -text | grep -A2 Validity
docker stop ${CONTAINER}
```

## Locally

```bash
mvn package

tomcat/createCerts.sh

# Copy lib binaries from bitnami image
# Or compile yourself
# https://tomcat.apache.org/tomcat-9.0-doc/apr.html
# Download: https://tomcat.apache.org/download-native.cgi
# Deps: sudo apt-get install libapr1 libapr1-dev 
CONTAINER=$(docker create bitnami/tomcat:9.0.31-debian-10-r25 )
docker cp ${CONTAINER}:/opt/bitnami/tomcat/lib /tmp
docker rm ${CONTAINER}
mkdir tomcat/target/lib
mv /tmp/lib/libapr* /tmp/lib/libtcnative* tomcat/target/lib

# Start tomcat
(cd tomcat && LD_LIBRARY_PATH="$(pwd)/target/lib:${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" target/bin/webapp)
```

## Releasing

`./mvnw release:prepare`

Sets versions in pom.xml, commits, tags and pushes to SCM. Travis builds tag and pushes to Maven Central. 