ARG FLAVOR=spring-boot 
# Other options: embedded-tomcat, standalone-tomcat

ARG JAVA_VERSION=8u252-b09-debian
FROM adoptopenjdk/openjdk8:jdk${JAVA_VERSION}-slim as jdk
FROM adoptopenjdk/openjdk8:jre${JAVA_VERSION} as jre
FROM bitnami/tomcat:9.0.44-debian-10-r8 as tomcat


FROM jdk as mavencache
ENV MAVEN_OPTS=-Dmaven.repo.local=/mvn
WORKDIR /app
COPY .mvn/ /app/.mvn/
COPY mvnw /app/ 
COPY pom.xml /app/
COPY reloading-connector/pom.xml  /app/reloading-connector/ 
COPY reloading-connector-spring-boot/pom.xml /app/reloading-connector-spring-boot/ 
COPY reloading-connector-embedded-tomcat/pom.xml /app/reloading-connector-embedded-tomcat/
COPY examples/pom.xml /app/examples/
COPY examples/spring-boot/pom.xml  /app/examples/spring-boot/ 
COPY examples/embedded-tomcat/pom.xml /app/examples/embedded-tomcat/
RUN ./mvnw dependency:go-offline


FROM mavencache as mavenbuild
COPY reloading-connector /app/reloading-connector
COPY reloading-connector-spring-boot  /app/reloading-connector-spring-boot
COPY reloading-connector-embedded-tomcat  /app/reloading-connector-embedded-tomcat
COPY examples /app/examples
RUN ./mvnw package

COPY --from=tomcat /opt/bitnami/tomcat/lib/ /dist/app/lib
COPY entrypoint.sh /dist/
RUN mkdir /dist/certs
COPY createCerts.sh /dist


FROM jre as spring-boot-aggregator
COPY --from=mavenbuild /app/examples/spring-boot/target/spring-boot-*.jar /dist/app/app.jar
COPY --from=mavenbuild /dist /dist


FROM jre as spring-boot
COPY --from=spring-boot-aggregator --chown=1001:0 /dist /


FROM jre as embedded-tomcat-aggregator
COPY --from=mavenbuild /app/examples/embedded-tomcat/target/tomcat-jar-with-dependencies.jar /dist/app/app.jar
COPY --from=mavenbuild /dist /dist

FROM jre as embedded-tomcat
COPY --from=embedded-tomcat-aggregator --chown=1001:0 /dist /


FROM tomcat as standalone-tomcat-aggreagator
USER root
RUN mkdir -p /dist/opt/bitnami/tomcat/lib/ /dist/opt/bitnami/tomcat/conf/certs
#RUN curl -o /dist/opt/bitnami/tomcat/lib/reloading-connector.jar https://repo1.maven.org/maven2/info/schnatterer/tomcat-reloading-connector/reloading-connector/0.1.0/reloading-connector-0.1.0.jar
COPY --from=mavenbuild /app/reloading-connector/target/reloading-connector-*.jar  /dist/opt/bitnami/tomcat/lib/
COPY examples/standalone-tomcat/server.xml /dist/opt/bitnami/tomcat/conf/server.xml
COPY examples/standalone-tomcat/entrypoint.sh /dist/entrypoint.sh
# Add links so all flavors have /createCerts.sh at same folder
RUN ln -s /opt/bitnami/tomcat/conf/certs /dist/certs
COPY createCerts.sh /dist

FROM tomcat as standalone-tomcat
COPY --from=standalone-tomcat-aggreagator --chown=1001:0 /dist /


FROM ${FLAVOR} 
USER 1001:0
ENTRYPOINT [ "/entrypoint.sh" ]
