#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source $DIR/settings.sh

if [ ! -f "$DIR/$DB_NAME" ]; then
	echo >&2 "The comics database ($DB_NAME) was not found. Did you pull it yet?"
	exit 1
fi


adb push $DIR/$DB_NAME $ANDROID_LOCATION
