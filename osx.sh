#!/bin/bash
#
###################################################################
# Peer to Peer Network - Jason D Stock - stock - October 02, 2019 #
###################################################################
#
# Run script for local MacOSX cluster.
#
# Configure the 'conf/machine_list' to specify the number of peers to join and
# any line arguments. Discovery will start in the terminal, then a new window
# will be opened for the Peer and the Store to run. Execute './osx.sh' to start
# the list of Peers, and run './osx.sh s' to start the Store node.

DIR="$( cd "$( dirname "$0" )" && pwd )"
JAR_PATH="$DIR/conf/:$DIR/build/libs/peer-to-peer-network.jar"
MACHINE_LIST="$DIR/conf/machine_list"
COMPILE="$( ps -ef | grep [c]s555.system.node.Discovery )"

SCRIPT="java -cp $JAR_PATH cs555.system.node.Peer"

function new_tab() {
    ARG=`echo $1 | cut -d"+" -f2`
    osascript \
        -e "tell application \"Terminal\"" \
        -e "tell application \"System Events\" to keystroke \"t\" using {command down}" \
        -e "do script \"$SCRIPT $ARG\" in front window" \
        -e "end tell" > /dev/null
}

if [ -z "$COMPILE" ]
then
LINES=`find . -name "*.java" -print | xargs wc -l | grep "total" | awk '{$1=$1};1'`
    echo Project has "$LINES" lines
    gradle clean
    gradle build
    open -a Terminal .
    open -a Terminal .
    java -cp $JAR_PATH cs555.system.node.Discovery;
elif [ "$1" = "s" ]
then
    java -cp $JAR_PATH cs555.system.node.Store;
else
    for i in `cat $MACHINE_LIST`
    do
        new_tab "$i"
        # sleep 0.30
    done
fi
