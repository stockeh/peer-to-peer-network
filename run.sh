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

function jumpto
{
    label=$1
    cmd=$(sed -n "/$label:/{:a;n;p;ba};" $0 | grep -v ':$')
    eval "$cmd"
    exit
}

if [ "$1" = "peer" ]
then

jumpto peer

elif [ "$1" = "store" ]
then

jumpto store

fi

# Launch Discovery

LINES=`find . -name "*.java" -print | xargs wc -l | grep "total" | awk '{$1=$1};1'`
echo Project has "$LINES" lines

gradle clean; gradle build
gnome-terminal --geometry=170x50 -t "Discovery" -e "ssh -t $(prop 'discovery.host') cd '$DIR'; 'java -cp $JAR_PATH cs555.system.node.Discovery; bash;'"

sleep 2

# Launch Peers

peer:

SCRIPT="java -cp $JAR_PATH cs555.system.node.Peer"

k=0
COMMAND='gnome-terminal --geometry=200x40'
for i in `cat $MACHINE_LIST`
do
    echo 'logging into '$i
    ARG=`echo $i | cut -d"+" -f2`
    MACHINE=`echo $i | cut -d"+" -f1`
    OPTION='--tab -t "'$MACHINE' | '$ARG'" -e "ssh -t '$MACHINE' cd '$DIR'; sleep '$k'; echo '$SCRIPT' '$ARG'; '$SCRIPT' '$ARG'"'
    COMMAND+=" $OPTION"
    k=`echo $k + 1.50 | bc`
done
eval $COMMAND &

sleep 1

store: 

# Launch Store

gnome-terminal --geometry=160x50 -t "Store" -e "ssh -t $(prop 'store.host') cd '$DIR'; 'java -cp $JAR_PATH cs555.system.node.Store; bash;'"
