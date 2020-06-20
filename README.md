tomcat-reloading-connector
======
[![Build Status](https://travis-ci.org/schnatterer/tomcat-reloading-connector.svg?branch=master)](https://travis-ci.org/schnatterer/tomcat-reloading-connector)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=info.schnatterer.tomcat-reloading-connector%3Atomcat-reloading-connector-parent&metric=sqale_index)](https://sonarcloud.io/dashboard?id=info.schnatterer.tomcat-reloading-connector%3Atomcat-reloading-connector-parent)
[![](https://img.shields.io/docker/image-size/schnatterer/tomcat-reloading-connector-example)](https://hub.docker.com/r/schnatterer/tomcat-reloading-connector-example)

Tomcat connector that automatically reloads SSLConfig.

# How to use?

Right now, tomcat-reloading-connector offers a specialized `org.apache.coyote.http11.Http11AprProtocol` that watches
the folder that contains the first configured certificate for changes and reloads SSLConfig on change.
 
`Http11AprProtocol` means this will only work with 
[Apache Portable Runtime (APR) based Native library for Tomcat](https://tomcat.apache.org/tomcat-9.0-doc/apr.html).

## Dependency

```XML
<dependency>
  <groupId>info.schnatterer.tomcat-reloading-connector</groupId>
  <artifactId>tomcat-reloading-connector</artifactId>
  <version>0.1.0</version>
</dependency>
```

If you need the `jar` you could also download it manually, from here:

`https://repo1.maven.org/maven2/info/schnatterer/tomcat-reloading-connector/reloading-connector/0.1.0/reloading-connector-0.1.0.jar`

[![Maven Central](https://img.shields.io/maven-central/v/info.schnatterer.tomcat-reloading-connector/reloading-connector.svg)](https://search.maven.org/search?q=a:reloading-connector%20AND%20g:info.schnatterer.tomcat-reloading-connector)

You can also get snapshot versions from our [snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/info/schnatterer/tomcat-reloading-connector/) 
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

## Use with Tomcat

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

* See also [example](examples/standalone-tomcat). 

Note that the certificates are not reloaded instantly but there is a short delay (default: 3s) to make sure all files
related to the certificated (cert, key, chain) have been written and no inconsistent state is loaded by tomcat.  
If you need to customize this, you can set the delay in milliseconds via the environment variable 
`TOMCAT_DELAY_RELOAD_CERTIFICATES_MILLIS`.  
For example for letsencrypt the time between the creation of CSR and full chain usually is between 10-20s.

## Usage with Spring Boot

* Add the dependency to your embedded tomcat project.
* Create a `Connector` with the `ReloadingHttp11AprProtocol` and configure it.
* See [example](examples/spring-boot).
 
## Usage with Embedded Tomcat

* Add the dependency to your embedded tomcat project.
* Create a `Connector` with the `ReloadingHttp11AprProtocol` and configure it.
* See [example](examples/embedded-tomcat). 

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
docker exec ${CONTAINER} /createCerts.sh
# View new cert
sleep 5
echo | openssl s_client -showcerts -servername localhost -connect localhost:8443 2>/dev/null | openssl x509 -inform pem -noout -text | grep -A2 Validity
docker stop ${CONTAINER}
```

If you want to build the image yourself:  
(note that they are included into one `Dockerfile` to keep them DRY)

* `docker build .` builds the spring-boot image
* `docker build --build-arg=FLAVOR=embedded-tomcat .` builds the embedded tomcat image
* `docker build --build-arg=FLAVOR=standalone-tomcat .` builds the standalone tomcat image

## Locally

```bash
mvn package

# Copy lib binaries from bitnami image
# Or compile yourself
# https://tomcat.apache.org/tomcat-9.0-doc/apr.html
# Download: https://tomcat.apache.org/download-native.cgi
# Deps: sudo apt-get install libapr1 libapr1-dev 
CONTAINER=$(docker create bitnami/tomcat:9.0.31-debian-10-r25 )
docker cp ${CONTAINER}:/opt/bitnami/tomcat/lib /tmp
docker rm ${CONTAINER}
mkdir lib
mv /tmp/lib/libapr* /tmp/lib/libtcnative* lib

./createCerts.sh

# Start embedded tomcat
LD_LIBRARY_PATH="$(pwd)/lib:${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" java -jar examples/embedded-tomcat/target/tomcat-jar-with-dependencies.jar
# or spring boot
LD_LIBRARY_PATH="$(pwd)/lib:${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}" java -jar examples/spring-boot/target/spring-boot-*.jar
# Standalone example is docker only
```

## Releasing

```bash
./mvnw release:prepare -DreleaseVersion=0.3.0 -DdevelopmentVersion=0.3.1-SNAPSHOT
```

Sets versions in `pom.xml`, commits, tags and pushes to SCM. Travis builds tag and pushes to Maven Central. 