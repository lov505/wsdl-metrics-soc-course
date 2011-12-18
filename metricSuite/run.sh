#!/bin/bash
METRICSUITE_HOME=/home/mcrasso/Desktop/teaching/courses/COS/cos2011/final/mgarriga/wsdl-metrics-soc-course/metricSuite
java -cp $METRICSUITE_HOME/bin/:lib/easywsdl-schema-2.1.jar:$METRICSUITE_HOME/lib/easywsdl-wsdl-2.1.jar:$METRICSUITE_HOME/lib/log4j-1.2.16.jar edu.isistan.easywsdl.Main $@
