#!/usr/bin/env bash
/createCerts.sh
exec "/opt/bitnami/scripts/tomcat/entrypoint.sh" "/opt/bitnami/scripts/tomcat/run.sh"