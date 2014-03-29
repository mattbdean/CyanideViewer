#!/bin/sh

adb pull /data/data/net.dean.cyanideviewer.app/databases/comics comics.db
sqlitebrowser comics.db &
