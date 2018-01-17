#!/bin/bash

# this script starts a las2peer node providing the code-generation-service
# pls execute it feorks in combination with the model persistence service,
# it tries to connect to a nd node (which runs this persistence service)
#-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5432
java  -cp "lib/*" i5.las2peer.tools.L2pNodeLauncher -p 9012 -b 134.61.69.177:9011 --service-directory service startService\(\'i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1\'\) interactive