# CAE-Code-Generation-Service
This service mainly handles the code generation of the Community Application
Editor.
It is used by the [Model Persistence Service](https://github.com/rwth-acis/CAE-Model-Persistence-Service).

## Are you looking for the official CAE instance?
If you just want to try out the Community Application Editor you don't need to set up your own environment. For building applications there are the following spaces:
* [Microservice Editor](http://cloud10.dbis.rwth-aachen.de:8081/spaces/CAEMicroservice)
* [Frontend Widget Editor](http://cloud10.dbis.rwth-aachen.de:8081/spaces/CAEFrontend)
* [Application Editor](http://cloud10.dbis.rwth-aachen.de:8081/spaces/CAEApplication)

## Installation
For information on how to install the CAE take a look at the [wiki](https://github.com/rwth-acis/CAE/wiki/Deployment-and-Configuration).

## How to run using Docker


First build the image:
```bash
docker build . -t cae-code-generation-service
```

Then you can run the image like this:

```bash
docker run -p 8080:8080 -p 9011:9011 cae-code-generation-service
```

The REST-API will be available via *http://localhost:8080/CodeGen* and the las2peer node is available via port 9011.

In order to customize your setup you can set further environment variables.

### Node Launcher Variables

Set [las2peer node launcher options](https://github.com/rwth-acis/las2peer-Template-Project/wiki/L2pNodeLauncher-Commands#at-start-up) with these variables.
The las2peer port is fixed at *9011*.

| Variable | Default | Description |
|----------|---------|-------------|
| BOOTSTRAP | unset | Set the --bootstrap option to bootrap with existing nodes. The container will wait for any bootstrap node to be available before continuing. |
| SERVICE_PASSPHRASE | someNewPass | Set the second argument in *startService('<service@version>', '<SERVICE_PASSPHRASE>')*. |
| SERVICE_EXTRA_ARGS | unset | Set additional launcher arguments. Example: ```--observer``` to enable monitoring. |

### Service Variables


| Variable | Default |
|----------|---------|
| GIT_USER | "" |
| GIT_PASSWORD | "" |
| GIT_USER_MAIL | "" |
| GIT_ORGANIZATION | CAE-Community-Application-Editor |
| TEMPLATE_REPOSITORY | CAE-Templates |
| DEPLOYMENT_REPO | CAE-Deployment-Temp |
| USE_MODEL_SYNCHRONIZATION | true |
| JENKINS_URL | http://jenkins:8090/ |
| JENKINS_JOB_TOKEN | secretAuth |
| BUILD_JOB_NAME | CAEDeploymentJob |
| DOCKER_JOB_NAME | DockerJob |
| USED_GIT_HOST | GitHub |
| BASE_URL | https://github.com/ |
| TOKEN | secretAuth |
| WIDGET_HOME_BASE_URL | http://role:8086/ |
| OIDC_PROVIDER | https://api.learning-layers.eu/o/oauth2 |

### Other Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DEBUG  | unset | Set to any value to get verbose output in the container entrypoint script. |

### Volumes

The following places should be persisted in volumes in productive scenarios:

| Path | Description |
|------|-------------|
| /src/node-storage | Pastry P2P storage. |
| /src/etc/startup | Service agent key pair and passphrase. |
| /src/log | Log files. |
