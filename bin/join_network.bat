:: this script starts a las2peer node providing the code-generation-service
:: pls execute it from the bin folder of your deployment by double-clicking on it
:: since this service only works in combination with the model persistence service,
:: it tries to connect to a running node (which runs this persistence service)

%~d0
cd %~p0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.tools.L2pNodeLauncher -w -p 9012 -b 192.168.178.22:9011 --service-directory service startService('i5.las2peer.services.codeGenerationService.CodeGenerationService@0.1') interactive
pause
