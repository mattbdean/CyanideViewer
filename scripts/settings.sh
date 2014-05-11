#!/bin/bash

## The name of the database. This will be the name of the file pushed/pulled to/from the device
DB_NAME="comics.db"

## The name of the package of the application
PACKAGE="net.dean.cyanideviewer"

## The location on the Android device on which the database
ANDROID_LOCATION="/data/data/$PACKAGE/databases/$DB_NAME"

## The preferred browser to view SQLite3 databases with
PREFERRED_BROWSER="sqlitebrowser"