#!/bin/bash

ENVTYPE=${ENVTYPE:-local}

if [[ $ENVTYPE == "local" ]]; then
   PARAMETER_STORE_PATH=""
else
   PARAMETER_STORE_PATH="-Dparameter.store.path=/${ENVTYPE}/${ENVID}/${SVCTYPE}/env"
fi

echo "Going to run java -jar ${JAVA_OPTS} ${PARAMETER_STORE_PATH} -Djava.security.egd=file:/dev/./urandom /app.jar"

java -jar ${JAVA_OPTS} ${PARAMETER_STORE_PATH} -Djava.security.egd=file:/dev/./urandom /app.jar
