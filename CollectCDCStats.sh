#!/bin/bash
SCRIPT_DIR=$( dirname $( readlink -f $0 ) )
source ${SCRIPT_DIR}/conf/CollectCDCStats.properties
JAVA=${CDC_AS_HOME}/jre64/jre/bin/java

$JAVA -cp "${SCRIPT_DIR}/lib/*:${CDC_AS_HOME}/lib/*:${SCRIPT_DIR}/lib/downloaded/*" com.ibm.replication.iidr.warehouse.CollectCDCStats "$@"
