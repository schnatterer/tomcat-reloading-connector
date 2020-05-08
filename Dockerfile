ARG FLAVOR=spring-boot

ARG JAVA_VERSION=8u252-b09-debian
FROM adoptopenjdk/openjdk8:jdk${JAVA_VERSION}-slim as jdk
FROM adoptopenjdk/openjdk8:jre${JAVA_VERSION} as jre
FROM bitnami/tomcat:9.0.34-debian-10-r8 as tomcat

FROM jdk as mavencache
ENV MAVEN_OPTS=-Dmaven.repo.local=/mvn
WORKDIR /app
COPY .mvn/ /app/.mvn/
COPY mvnw /app/ 
COPY pom.xml /app
COPY reloading-connector/pom.xml  /app/reloading-connector/ 
COPY spring-boot/pom.xml  /app/spring-boot/ 
COPY embedded-tomcat/pom.xml /app/embedded-tomcat/
RUN ./mvnw dependency:go-offline

FROM mavencache as mavenbuild
COPY reloading-connector /app/reloading-connector
COPY embedded-tomcat /app/embedded-tomcat
COPY spring-boot /app/spring-boot
RUN ./mvnw package

FROM jre as spring-boot
COPY --from=mavenbuild /app/spring-boot/target/spring-boot-*.jar /dist/app/app.jar

FROM jre as embedded-tomcat
COPY --from=mavenbuild /app/embedded-tomcat/target/tomcat-jar-with-dependencies.jar /dist/app/app.jar

FROM ${FLAVOR} as aggregator
COPY --from=tomcat /opt/bitnami/tomcat/lib/ /dist/app/lib
COPY entrypoint.sh /dist/app/
RUN mkdir /dist/certs
COPY createCerts.sh /dist

FROM jre
COPY --from=aggregator --chown=1001:0 /dist /
USER 1001:0
ENTRYPOINT [ "/app/entrypoint.sh" ]

