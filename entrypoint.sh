#!/usr/bin/env bash
export LD_LIBRARY_PATH="/app/lib:${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
./createCerts.sh
exec java -jar -Dcatalina.home=/tmp /app/app.jar