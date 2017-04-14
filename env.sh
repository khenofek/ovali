#!/bin/sh

export CURRENT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[')
echo "Current version: "${CURRENT_VERSION}

export OVALI_DEPLOY_CONF=../deploy/myConf.json
