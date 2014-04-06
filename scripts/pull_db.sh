#!/bin/bash

source settings.sh

# Pulling from /data requires root
adb root 1>/dev/null

if adb pull $ANDROID_LOCATION $DB_NAME; then
	# Only launch the browser if the file was pulled successfully

	if command -v $PREFERRED_BROWSER 2>/dev/null; then
		# SQLiteBrowser exists, use it
        $PREFERRED_BROWSER $DB_NAME
    elif command -v sqlite3 2>/dev/null; then
    	# Use the command line instead
        sqlite3 $DB_NAME
    else
    	echo >&2 "The database was pulled, but no application was found to open it."
    fi
fi
