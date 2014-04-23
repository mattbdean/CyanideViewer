#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

source "$DIR"/settings.sh

# Remove both comics.db and comics.db-journal
adb shell rm "$ANDROID_LOCATION*"
