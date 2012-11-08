#!/bin/sh
[ -z "$DEVPATH" ] && DEVPATH='/Users/mathieuancelin/Desktop/devoxx2012/demo-devoxx-2012/nevernote-core/src/main/resources'
cd ./container/target/container-1.2.0-SNAPSHOT-all/container-1.2.0-SNAPSHOT
java -Ddev='true' -Ddevpath=$DEVPATH -jar bin/felix.jar 
