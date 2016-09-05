#!/bin/bash

# this script starts a las2peer node providing the code-generation-service
# pls execute it from the root folder of your deployment, e. g. ./bin/start_network.sh
# since this service only works in combination with the model persistence service,
# it tries to connect to a running node (which runs this persistence service)

java -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9012 -b 134.61.167.78:9011 startService\(\'i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1\'\) interactive
