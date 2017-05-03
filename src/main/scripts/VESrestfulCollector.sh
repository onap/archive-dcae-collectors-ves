#!/bin/sh

###
# ============LICENSE_START=======================================================
# PROJECT
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

usage() {
        echo "VESrestfulCollector.sh <start/stop>"
}


collector_start() {
        collectorPid=`pgrep -f org.openecomp.dcae.commonFunction`

        if [ ! -z "$collectorPid" ]; then
                echo  "WARNING: VES Restful Collector already running as PID $collectorPid";
                echo  "Startup Aborted!!!"
                exit 1
        fi

        # move into base directory
        BASEDIR=`dirname $0`
        cd $BASEDIR/..

        # run java. The classpath is the etc dir for config files, and the lib dir
        # for all the jars.
        nohup $JAVA -cp "etc${PATHSEP}lib/*" $JAVA_OPTS -Dhttps.protocols=TLSv1.1,TLSv1.2 $MAINCLASS $* &
        if [ $? -ne 0 ]; then
                echo "VES Restful Collector has been started!!!"
        fi


}

collector_stop() {
         collectorPid=`pgrep -f org.openecomp.dcae.commonFunction`
         if [ ! -z "$collectorPid" ]; then
                echo "Stopping PID $collectorPid"
                
                kill -9 $collectorPid
                sleep 5
                if [ ! "$(pgrep -f org.openecomp.dcae.commonFunction)" ]; then
                         echo "VES Restful Collector has been stopped!!!"
                else
                         echo "VES Restful Collector is being stopped!!!"
                fi
         else
                echo  "WARNING: No VES Collector instance is currently running";
                exit 1
         fi


}

## Check usage
if [ $# -ne 1 ]; then
        usage
        exit
fi


## Pre-setting

# use JAVA_HOME if provided
if [ -z "$JAVA_HOME" ]; then
        echo "ERROR: JAVA_HOME not setup"
        echo "Startup Aborted!!"
        exit 1
        #JAVA=java
else
        JAVA=$JAVA_HOME/bin/java
fi


MAINCLASS=org.openecomp.dcae.commonFunction.CommonStartup

# determine a path separator that works for this platform
PATHSEP=":"
case "$(uname -s)" in

        Darwin)
                ;;

         Linux)
                ;;

         CYGWIN*|MINGW32*|MSYS*)
                PATHSEP=";"
                ;;

        *)
                ;;
esac


if [ -z "$CONSUL_HOST" ] || [ -z "$CONFIG_BINDING_SERVICE" ] || [ -z "$DOCKER_HOST" ]; then
  	echo "INFO: USING STANDARD CONTROLLER CONFIGURATION"
else

    echo "INFO: DYNAMIC CONFIG INTERFACE SUPPORTED"
    CONFIG_UPDATER=org.openecomp.dcae.commonFunction.DynamicPropertyReader
    $JAVA -cp "etc${PATHSEP}lib/*"  $CONFIG_UPDATER $* 
    if [ $? -ne 0 ]; then
    	echo "ERROR: Failed to read dynamic configuration from consul"
    else
    	echo "INFO: Dynamic config updated successfully!"
    fi      
fi


case $1 in
        "start")
                collector_start
                ;;
        "stop")
                collector_stop
                ;;
        *)
                usage
                ;;
esac
