#!/bin/bash
#
# UCIPLink start and stop script
#
# $Id$

. /opt/seamless/etc/env/env.conf
. /opt/seamless/etc/env/functions

CLASSPATH=/opt/seamless/lib/uciplink/${project.artifactId}-${project.version}.jar:/opt/seamless/conf/uciplink
START_CLASS="com.seamless.ers.links.uciplink.UCIPLink"
START_FLAGS="-server -cp $CLASSPATH"
START_CMD="$ers_java $START_FLAGS $START_CLASS"
PIDFILE=/var/seamless/system/uciplink/uciplink.pid
LOGFILE=/var/seamless/log/uciplink/init.out

RETVAL=0

case "$1" in
	start)
		dostart "$START_CMD" "$PIDFILE" "$LOGFILE"
		;;
	status)
		getstatus $PIDFILE
		;;
	stop)
		dostop $PIDFILE $LOGFILE
	  	;;
	restart)
		dorestart "$START_CMD" "$PIDFILE" "$LOGFILE"
		;;
	*)
		echo $"Usage: $(basename $0) {start|status|stop|restart}"
		;;  	
esac

exit $RETVAL
