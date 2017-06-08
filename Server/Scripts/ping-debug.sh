#!/bin/sh
sudo babeld wlx0013efc505fc &
res=$(zenity --entry --title="Ping debug" --text="Ping test to Raspberry Pi number:")
ping -c 1 192.168.1.$res
if [ "$?" = 0 ]; then
	zenity --info --title="Success" --text="Ping successful"
else
	zenity --info --title="Failure" --text="Ping unsuccessful"
fi
