#!/usr/bin/env bash

HEAP_SIZE="1g"

export BASE_DIR="$(dirname $0)/.."

JVM_OPTS="${JVM_OPTS} -Xmx${HEAP_SIZE}"
JVM_OPTS="${JVM_OPTS} -Dlogback.configurationFile=${BASE_DIR}/conf/logger-conf.xml"
JVM_OPTS="${JVM_OPTS} -Dmath_admin.config_file=${BASE_DIR}/conf/config.json"

java ${JVM_OPTS} -jar ${BASE_DIR}/lib/math_admin.jar