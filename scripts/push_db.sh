#!/bin/bash

source settings.sh

if [ ! -f "$DB_NAME" ]; then
	echo >&2 "The comics database ($DB_NAME) was not found. Did you pull it yet?"
	exit 1
fi


adb push $DB_NAME $ANDROID_LOCATION
