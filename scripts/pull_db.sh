#!/bin/sh

adb pull /data/data/net.dean.cyanideviewer.app/databases/comics.db comics.db
sqlitebrowser comics.db &
