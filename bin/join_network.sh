#!/bin/bash

# this script starts a las2peer node providing the code-generation-service
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh
# since this service only works in combination with the model persistence service,
# it tries to connect to a running node (which runs this persistence service)
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5432

#java  -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9012 -b 134.61.69.177:9011 --service-directory service startService\(\'i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1\'\) interactive
#java  -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9012 -b 134.61.86.239:9011 --service-directory service startService\(\'i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1\'\) interactive
java  -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9012 -b 192.168.178.26:9011 --service-directory service startService\(\'i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1\'\) interactive