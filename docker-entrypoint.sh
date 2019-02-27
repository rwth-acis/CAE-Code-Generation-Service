#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# set some helpful variables
export SERVICE_PROPERTY_FILE='etc/i5.las2peer.services.codeGenerationService.CodeGenerationService.properties'
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' etc/ant_configuration/service.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}


# set defaults for optional service parameters
[[ -z "${SERVICE_PASSPHRASE}" ]] && export SERVICE_PASSPHRASE='someNewPass'
[[ -z "${GIT_USER}" ]] && export GIT_USER=''
[[ -z "${GIT_PASSWORD}" ]] && export GIT_PASSWORD=''
[[ -z "${GIT_USER_MAIL}" ]] && export GIT_USER_MAIL=''
[[ -z "${GIT_ORGANIZATION}" ]] && export GIT_ORGANIZATION='CAE-Community-Application-Editor'
[[ -z "${TEMPLATE_REPOSITORY}" ]] && export TEMPLATE_REPOSITORY='CAE-Templates'
[[ -z "${DEPLOYMENT_REPO}" ]] && export DEPLOYMENT_REPO='CAE-Deployment-Temp'
[[ -z "${USE_MODEL_SYNCHRONIZATION}" ]] && export USE_MODEL_SYNCHRONIZATION='true'
[[ -z "${JENKINS_URL}" ]] && export JENKINS_URL='http://jenkins:8090/'
[[ -z "${JENKINS_JOB_TOKEN}" ]] && export JENKINS_JOB_TOKEN='secretAuth'
[[ -z "${BUILD_JOB_NAME}" ]] && export BUILD_JOB_NAME='CAEDeploymentJob'
[[ -z "${DOCKER_JOB_NAME}" ]] && export DOCKER_JOB_NAME='DockerJob'
[[ -z "${USED_GIT_HOST}" ]] && export USED_GIT_HOST='GitHub'
[[ -z "${BASE_URL}" ]] && export BASE_URL='https://github.com/'
[[ -z "${TOKEN}" ]] && export TOKEN='secretAuth'
[[ -z "${WIDGET_HOME_BASE_URL}" ]] && export WIDGET_HOME_BASE_URL='http://role:8086/'
[[ -z "${OIDC_PROVIDER}" ]] && export OIDC_PROVIDER='https://api.learning-layers.eu/o/oauth2'

function set_in_service_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${SERVICE_PROPERTY_FILE}
}
set_in_service_config gitUser ${GIT_USER}
set_in_service_config gitPassword ${GIT_PASSWORD}
set_in_service_config gitUserMail ${GIT_USER_MAIL}
set_in_service_config gitOrganization ${GIT_ORGANIZATION}
set_in_service_config templateRepository ${TEMPLATE_REPOSITORY}
set_in_service_config deploymentRepo ${DEPLOYMENT_REPO}
set_in_service_config useModelSynchronization ${USE_MODEL_SYNCHRONIZATION}
set_in_service_config jenkinsUrl ${JENKINS_URL}
set_in_service_config jenkinsJobToken ${JENKINS_JOB_TOKEN}
set_in_service_config buildJobName ${BUILD_JOB_NAME}
set_in_service_config dockerJobName ${DOCKER_JOB_NAME}
set_in_service_config usedGitHost ${USED_GIT_HOST}
set_in_service_config baseURL ${BASE_URL}
set_in_service_config token ${TOKEN}
set_in_service_config widgetHomeBaseURL ${WIDGET_HOME_BASE_URL}
set_in_service_config oidcProvider ${OIDC_PROVIDER}

# wait for any bootstrap host to be available
if [[ ! -z "${BOOTSTRAP}" ]]; then
    echo "Waiting for any bootstrap host to become available..."
    for host_port in ${BOOTSTRAP//,/ }; do
        arr_host_port=(${host_port//:/ })
        host=${arr_host_port[0]}
        port=${arr_host_port[1]}
        if { </dev/tcp/${host}/${port}; } 2>/dev/null; then
            echo "${host_port} is available. Continuing..."
            break
        fi
    done
fi

# prevent glob expansion in lib/*
set -f
LAUNCH_COMMAND='java -cp lib/* i5.las2peer.tools.L2pNodeLauncher -s service -p '"${LAS2PEER_PORT} ${SERVICE_EXTRA_ARGS}"
if [[ ! -z "${BOOTSTRAP}" ]]; then
    LAUNCH_COMMAND="${LAUNCH_COMMAND} -b ${BOOTSTRAP}"
fi

# start the service within a las2peer node
if [[ -z "${@}" ]]
then
  exec ${LAUNCH_COMMAND} startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) startWebConnector
else
  exec ${LAUNCH_COMMAND} ${@}
fi
