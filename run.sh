#!/bin/bash

#########################################################################
#                                                                       #
#    Fault Tolerant File System Using Replication and Erasure Coding    #
#                                                                       #
#             Jason D Stock - stock - September 04, 2019                #
#                                                                       #
#########################################################################

# Configurations

DIR="$( cd "$( dirname "$0" )" && pwd )"
JAR_PATH="$DIR/conf/:$DIR/build/libs/fault-tolerant-file-system.jar"
MACHINE_LIST="$DIR/conf/machine_list"
APPLICATION_PROPERTIES="$DIR/conf/application.properties"

function prop {
    grep "${1}" ${APPLICATION_PROPERTIES}|cut -d'=' -f2
}

# Launch discovery

LINES=`find . -name "*.java" -print | xargs wc -l | grep "total" | awk '{$1=$1};1'`
echo Project has "$LINES" lines

gradle clean; gradle build
gnome-terminal --geometry=170x60 -e "ssh -t $(prop 'discovery.host') 'java -cp $JAR_PATH cs555.system.node.Discovery; bash;'"

sleep 1

# Launch Chunk Servers

SCRIPT="java -cp $JAR_PATH cs555.system.node.Peer"

for ((j=0; j<${1:-1}; j++))
do
    COMMAND='gnome-terminal'
    for i in `cat $MACHINE_LIST`
    do
        echo 'logging into '$i
        OPTION='--tab -e "ssh -t '$i' '$SCRIPT'"'
        COMMAND+=" $OPTION"
    done
    eval $COMMAND &
done

sleep 1

# Launch peer

gnome-terminal --geometry=132x60 -e "ssh -t $(prop 'peer.host') 'java -cp $JAR_PATH cs555.system.node.Peer; bash;'"
