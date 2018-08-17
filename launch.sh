#!/bin/bash

ENVTYPE=${ENVTYPE:-local}

if [[ $ENVTYPE == "local" ]]; then
   PARAMETER_STORE_PATH=""
else
   PARAMETER_STORE_PATH="-Dparameter.store.print=true -Dparameter.store.path=/${ENVTYPE}/${ENVID}/${SVCTYPE}/env"
fi

echo "JAVA_OPTS: ${JAVA_OPTS}"
echo "PARAMETER_STORE_PATH: ${PARAMETER_STORE_PATH}"

java -jar \
    ${JAVA_OPTS} \
    ${PARAMETER_STORE_PATH} \
    -Djava.security.egd=file:/dev/./urandom /app.jar \
    --spring.config.location=classpath:/application.properties,classpath:/ingestion-defaults.properties
