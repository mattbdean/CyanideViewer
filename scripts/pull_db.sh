#!/bin/sh

adb pull /data/data/net.dean.cyanideviewer/databases/comics.db comics.db
sqlitebrowser comics.db &
