#!/bin/bash
#
#
# 1 build the docker image with both service manager and ves collector
# 2 tag and then push to the remote repo if not verify
#


phase=$1

case $phase in 
  verify|merge|release) 
    echo Running $phase job
    ;;
  *)
    echo Unknown phase \'$phase\'
    exit 1
esac


# DCAE Controller service manager for VES collector
DCM_AR="${WORKSPACE}/manager.zip"
if [ ! -f "${DCM_AR}" ]
then
    echo "FATAL error cannot locate ${DCM_AR}"
    exit 2
fi

# unarchive the service manager
TARGET="${WORKSPACE}/target"
STAGE="${TARGET}/stage"
DCM_DIR="${STAGE}/opt/app/manager"
[ ! -d "${DCM_DIR}" ] && mkdir -p "${DCM_DIR}"
unzip -qo -d "${DCM_DIR}" "${DCM_AR}"

# unarchive the collector
VERSION=$(xpath -e '/project/version/text()' pom.xml)
AR=${WORKSPACE}/target/OpenVESCollector-${VERSION}-bundle.tar.gz
APP_DIR=${STAGE}/opt/app/SEC

[ -d ${STAGE}/opt/app/OpenVESCollector-${VERSION} ] && rm -rf ${STAGE}/opt/app/OpenVESCollector-$VERSION

[ ! -f $APP_DIR ] && mkdir -p ${APP_DIR}

gunzip -c ${AR} | tar xvf - -C ${APP_DIR} --strip-components=1

#
# generate the manager start-up.sh
#
## [ -f "${DCM_DIR}/start-manager.sh" ] && exit 0

cat <<EOF > "${DCM_DIR}/start-manager.sh"
#!/bin/bash

MAIN=org.openecomp.dcae.controller.service.standardeventcollector.servers.manager.DcaeControllerServiceStandardeventcollectorManagerServer
ACTION=start

WORKDIR=/opt/app/manager

LOGS=\$WORKDIR/logs

mkdir -p \$LOGS

cd \$WORKDIR

echo 10.0.4.102 \$(hostname).dcae.simpledemo.openecomp.org >> /etc/hosts

if [ ! -e config ]; then
	echo no configuration directory setup: \$WORKDIR/config
	exit 1
fi

exec java -cp ./config:./lib:./lib/*:./bin \$MAIN \$ACTION > logs/manager.out 2>logs/manager.err

EOF

chmod 775 "${DCM_DIR}/start-manager.sh"


#
# generate docker file
#
cat <<EOF > "${STAGE}/Dockerfile"
FROM ubuntu:14.04

MAINTAINER dcae@lists.openecomp.org

WORKDIR /opt/app/manager

ENV HOME /opt/app/SEC
ENV JAVA_HOME /usr

RUN apt-get update && apt-get install -y \
        bc \
        curl \
        telnet \
        vim \
        netcat \
        openjdk-7-jdk

COPY opt /opt

EXPOSE 9999

CMD [ "/opt/app/manager/start-manager.sh" ]
EOF

#
# build the docker image. tag and then push to the remote repo
#
IMAGE='openecomp/dcae-controller-common-event'
#TAG='1.0.0'
VERSION=$(xpath -e "//project/version/text()" "pom.xml")
EXT=$(echo "$VERSION" | rev | cut -s -f1 -d'-' | rev)
if [ -z "$EXT" ]; then
    VERSION=$(echo "${VERSION}-STAGING")
fi
TIMESTAMP=$(date +%C%y%m%dT%H%M%S)
TAG="$VERSION-$TIMESTAMP"
LFQI="${IMAGE}:${TAG}"
BUILD_PATH="${WORKSPACE}/target/stage"

# build a docker image
echo docker build --rm -t "${LFQI}" "${BUILD_PATH}"
docker build --rm -t "${LFQI}" "${BUILD_PATH}"

case $phase in 
  verify) 
    exit 0
  ;;
esac

#
# push the image
#
# io registry  DOCKER_REPOSITORIES="nexus3.openecomp.org:10001 \
# release registry                   nexus3.openecomp.org:10002 \
# snapshot registry                   nexus3.openecomp.org:10003"
REPO='nexus3.openecomp.org:10003'

if [ ! -z "$REPO" ]; then
    RFQI="${REPO}/${LFQI}"
    # tag
    docker tag "${LFQI}" "${RFQI}"

    # push to remote repo
    docker push "${RFQI}"


    TAG="LATEST"
    LFQI="${IMAGE}:${TAG}"
    RFQI2="${REPO}/${LFQI}"
    echo "$LFQI"
    echo "$RFQI2"
    docker tag "${RFQI}" "${RFQI2}"
    docker push "${RFQI2}"
fi
