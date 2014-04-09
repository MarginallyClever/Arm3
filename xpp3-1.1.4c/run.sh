#!/bin/sh

#
# You can set JAVA_HOME to point ot JDK 1.3 
# or shell will try to deterine java location using which
#
# written by Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]

#JAVA_HOME=/l/jdk1.3

# 
# No need to modify anything after this line.
# --------------------------------------------------------------------


if [ -z "$JAVA_HOME" ] ; then
  JAVA=`/usr/bin/which java`
  if [ -z "$JAVA" ] ; then
    echo "Cannot find JAVA. Please set your PATH."
    exit 1
  fi
  JAVA_BIN=`dirname $JAVA`
  JAVA_HOME=$JAVA_BIN/..
else
  JAVA=$JAVA_HOME/bin/java
fi

#echo "JAVA= $JAVA"

#if [ ! "`$JAVA -version 2>&1 | grep "\ 1\.3"`" ]; then 
#if [ ! "`$JAVA -version 2>&1 | egrep "\ 1\.[23456789].*"`" ]; then 
#    echo Required 1.2 verion of JDK: can not use $JAVA
#    echo Current version is:
#    $JAVA -version
#    exit 1
#fi 

#JAVA_OPTS="$JAVA_OPTS -Djava.compiler=NONE"

#echo set required LOCALCLASSPATH
LOCALCLASSPATH=`/bin/sh $PWD/classpath.sh run`

MY_JAVA="$JAVA $JAVA_OPTS $JAVA_DEBUG_OPTS -cp $LOCALCLASSPATH"


if [ -z "$1" ] ; then
   echo Please specify test name.
   exit 1
fi

NAME=$1
shift

if [ "$NAME" = "junit" ] ; then
  CMD="$MY_JAVA org.xmlpull.v1.tests.PackageTests $*"
elif [ "$NAME" = "eventtypes" ] ; then
  CMD="$MY_JAVA eventtypes.EventTypes $*"
else
  CMD="$MY_JAVA $NAME $*"
fi

echo $CMD
$CMD
