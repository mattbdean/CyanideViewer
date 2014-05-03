#!/bin/bash

# http://stackoverflow.com/a/246128/1275092
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source "$DIR"/settings.sh

# First, run as the application and make the database world readable and world writable
# Then, pull the database
# Finally, run as the application and make the database readable and writable to the owner
# http://stackoverflow.com/a/18472135/1275092
if adb shell "run-as $PACKAGE chmod 666 $ANDROID_LOCATION" \
	&& adb pull "$ANDROID_LOCATION" "$DIR/$DB_NAME" \
	&& adb shell "run-as $PACKAGE chmod 600 $ANDROID_LOCATION"; then
	# Only launch the browser if the file was pulled successfully

	if command -v "$PREFERRED_BROWSER" 2>/dev/null; then
		# SQLiteBrowser exists, use it
		"$PREFERRED_BROWSER" "$DIR/$DB_NAME"
	elif command -v sqlite3 2>/dev/null; then
		# Use the command line instead
		sqlite3 "$DIR/$DB_NAME"
	else
		echo >&2 "The database was pulled, but no application was found to open it."
	fi
fi
