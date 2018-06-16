#!/bin/bash

ARTIFACT_NAME="servantV3-0.0.1-SNAPSHOT-fat.jar"

BUILDING_FOLDER="/tmp/servantV3/"
BACKUP_FOLDER="/opt/servant/backup/"
INSTALLATION_FOLDER="/opt/servant/"

if [ -d "$BUILDING_FOLDER" ]; then
    echo "cleaning up temporary folder"
    rm -rf $BUILDING_FOLDER
fi

echo "shutting down service"
sudo /etc/init.d/vertxd stop


echo "download repo"
git clone https://github.com/ucpdh23/Servant.git $BUILDING_FOLDER

echo "building project"
cd $BUILDING_FOLDER
export MAVEN_OPTS="-Xmx1024m"
mvn clean package
rc=$?
if [[ $rc -ne 0 ]] ; then
  echo 'Cannot build the project, please review traces for more details'; exit $rc
fi

echo "shutting down service"
sudo /etc/init.d/vertxd stop

if [ -f "${INSTALLATION_FOLDER}/${ARTIFACT_NAME}" ]; then
    if [ ! -d "$BACKUP_FOLDER" ]; then
        mkdir $BACKUP_FOLDER
    fi

    echo "backing up previous version"
    TIMESTAMP=$(date +"%Y%m%d%H%M")
    mv ${INSTALLATION_FOLDER}/${ARTIFACT_NAME} ${BACKUP_FOLDER}/${TIMESTAMP}_${ARTIFACT_NAME}
fi

echo "installing artifact"
cp "${BUILDING_FOLDER}/target/${ARTIFACT_NAME}" "${INSTALLATION_FOLDER}"

echo "starting process"
sudo /etc/init.d/vertxd start
