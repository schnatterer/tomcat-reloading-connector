#!/usr/bin/env bash
export LD_LIBRARY_PATH="/app/lib:${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
cd /app
/app/createCerts.sh
exec /app/bin/webapp