#!/bin/bash
#
###################################################################
# Peer to Peer Network - Jason D Stock - stock - October 02, 2019 #
###################################################################
#
# Run script for Linux cluster.
#
# Configure the 'conf/application.properties' to specify the host for
# Discovery and the Store node. Configure the 'conf/machine_list' to
# specify the number of peers to join and any line arguments.

# Configurations

DIR="$( cd "$( dirname "$0" )" && pwd )"
JAR_PATH="$DIR/conf/:$DIR/build/libs/peer-to-peer-network.jar"
MACHINE_LIST="$DIR/conf/machine_list"
APPLICATION_PROPERTIES="$DIR/conf/application.properties"

function prop {
    grep "${1}" ${APPLICATION_PROPERTIES}|cut -d'=' -f2
}

# Launch Discovery

LINES=`find . -name "*.java" -print | xargs wc -l | grep "total" | awk '{$1=$1};1'`
echo Project has "$LINES" lines

gradle clean; gradle build
gnome-terminal --geometry=170x60 -e "ssh -t $(prop 'discovery.host') 'java -cp $JAR_PATH cs555.system.node.Discovery; bash;'"

sleep 1

# Launch Peers

SCRIPT="java -cp $JAR_PATH cs555.system.node.Peer"

for ((j=0; j<${1:-1}; j++))
do
    COMMAND='gnome-terminal'
    for i in `cat $MACHINE_LIST`
    do
        echo 'logging into '$i
        ARG=`echo $i | cut -d"+" -f2`
        OPTION='--tab -e "ssh -t '$i' '$SCRIPT' '$ARG'"'
        COMMAND+=" $OPTION"
    done
    eval $COMMAND &
done

sleep 1

# Launch Store

gnome-terminal --geometry=132x60 -e "ssh -t $(prop 'peer.host') 'java -cp $JAR_PATH cs555.system.node.Store; bash;'"
