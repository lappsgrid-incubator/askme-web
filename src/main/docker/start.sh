#!/usr/bin/env bash

PORT=11111

if [[ -n "$1" ]] ; then
    IP=$1
fi
OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=$PORT -Djava.rmi.server.hostname=$IP -Dcom.sun.management.jmxremote.rmi.port=$PORT"

#java $OPTS -Xmx4G -jar askme-web.jar
java -Xmx4G -jar service.jar
