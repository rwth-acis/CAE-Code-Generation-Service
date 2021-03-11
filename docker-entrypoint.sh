#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi
NODE_ID_SEED=${NODE_ID_SEED:-$RANDOM}
EXTRA_ETH_WAIT=${EXTRA_ETH_WAIT:-30}
CONFIG_ENDPOINT_WAIT=${CONFIG_ENDPOINT_WAIT:-21600}
ETH_PROPS_DIR=/src/etc/
ETH_PROPS=i5.las2peer.registry.data.RegistryConfiguration.properties
function waitForEndpoint {
    /src/wait-for-command/wait-for-command.sh -c "nc -z ${1} ${2:-80}" --time ${3:-10} --quiet
}

function host { echo ${1%%:*}; }
function port { echo ${1#*:}; }
# set some helpful variables
export SERVICE_PROPERTY_FILE='etc/i5.las2peer.services.codeGenerationService.CodeGenerationService.properties'
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' gradle.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' gradle.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' gradle.properties)
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

if [ -n "$LAS2PEER_CONFIG_ENDPOINT" ]; then
    echo Attempting to autoconfigure registry blockchain parameters ...
    if waitForEndpoint $(host ${LAS2PEER_CONFIG_ENDPOINT}) $(port ${LAS2PEER_CONFIG_ENDPOINT}) $CONFIG_ENDPOINT_WAIT; then
        echo "Port is available (but that may just be the Docker daemon)."
        echo Downloading ...
        wget --quiet --tries=inf "http://${LAS2PEER_CONFIG_ENDPOINT}/${ETH_PROPS}" -O "${ETH_PROPS_DIR}${ETH_PROPS}"
        echo done.
    else
        echo Registry configuration endpoint specified but not accessible. Aborting.
        exit 1
    fi
fi

if [ -n "$LAS2PEER_BOOTSTRAP" ]; then
    if waitForEndpoint $(host ${LAS2PEER_BOOTSTRAP}) $(port ${LAS2PEER_BOOTSTRAP}) 600; then
        echo Las2peer bootstrap available, continuing.
    else
        echo Las2peer bootstrap specified but not accessible. Aborting.
        exit 3
    fi
fi

# it's realistic for different nodes to use different accounts (i.e., to have
# different node operators). this function echos the N-th mnemonic if the
# variable WALLET is set to N. If not, first mnemonic is used
function selectMnemonic {
    declare -a mnemonics=("differ employ cook sport clinic wedding melody column pave stuff oak price" "memory wrist half aunt shrug elbow upper anxiety maximum valve finish stay" "alert sword real code safe divorce firm detect donate cupboard forward other" "pair stem change april else stage resource accident will divert voyage lawn" "lamp elbow happy never cake very weird mix episode either chimney episode" "cool pioneer toe kiwi decline receive stamp write boy border check retire" "obvious lady prize shrimp taste position abstract promote market wink silver proof" "tired office manage bird scheme gorilla siren food abandon mansion field caution" "resemble cattle regret priority hen six century hungry rice grape patch family" "access crazy can job volume utility dial position shaft stadium soccer seven")
    if [[ ${WALLET} =~ ^[0-9]+$ && ${WALLET} -lt ${#mnemonics[@]} ]]; then
    # get N-th mnemonic
        echo "${mnemonics[${WALLET}]}"
    else
        # note: zsh and others use 1-based indexing. this requires bash
        echo "${mnemonics[0]}"
    fi
}

#prepare pastry properties
echo external_address = $(curl -s https://ipinfo.io/ip):${LAS2PEER_PORT} > etc/pastry.properties

echo Starting las2peer node ...
if [ -n "$LAS2PEER_ETH_HOST" ]; then
    echo ... using ethereum boot procedure: 
    java $(echo $ADDITIONAL_JAVA_ARGS) \
        -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher \
        --service-directory service \
        --port $LAS2PEER_PORT \
        $([ -n "$LAS2PEER_BOOTSTRAP" ] && echo "--bootstrap $LAS2PEER_BOOTSTRAP") \
        --node-id-seed $NODE_ID_SEED \
        --observer \
        --ethereum-mnemonic "$(selectMnemonic)" \
        $(echo $ADDITIONAL_LAUNCHER_ARGS) \
        startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) \
        startWebConnector \
        "node=getNodeAsEthereumNode()" "registry=node.getRegistryClient()" "n=getNodeAsEthereumNode()" "r=n.getRegistryClient()" \
        $(echo $ADDITIONAL_PROMPT_CMDS) \
        interactive
else
    echo ... using non-ethereum boot procedure:
    java $(echo $ADDITIONAL_JAVA_ARGS) \
        -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher \
        --service-directory service \
        --port $LAS2PEER_PORT \
        $([ -n "$LAS2PEER_BOOTSTRAP" ] && echo "--bootstrap $LAS2PEER_BOOTSTRAP") \
        --node-id-seed $NODE_ID_SEED \
        $(echo $ADDITIONAL_LAUNCHER_ARGS) \
        startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) \
        startWebConnector \
        $(echo $ADDITIONAL_PROMPT_CMDS) \
        interactive
fi
