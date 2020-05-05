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
COPY tomcat/pom.xml /app/tomcat/
RUN ./mvnw dependency:go-offline

FROM mavencache as mavenbuild
COPY reloading-connector /app/reloading-connector
COPY tomcat /app/tomcat
RUN ./mvnw package

FROM jre as aggragtor
COPY --from=tomcat /opt/bitnami/tomcat/lib/ /app/lib
COPY entrypoint.sh /app/
COPY --from=mavenbuild /app/tomcat/createCerts.sh /app/
COPY --from=mavenbuild /app/tomcat/target/tomcat.jar /app/
COPY --from=mavenbuild /app/tomcat/target/repo/ /app/repo
COPY --from=mavenbuild /app/tomcat/target/bin/ /app/bin
COPY --from=mavenbuild /app/tomcat/target/classes/ /app/

FROM jre
COPY --from=aggragtor --chown=1001:0 /app /app
USER 1001:0
ENTRYPOINT [ "/app/entrypoint.sh" ]