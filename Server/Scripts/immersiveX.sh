#!/bin/sh
sudo babeld wlx0013efc505fc &
gnome-terminal -e ~/Documents/launchServer.sh &
zenity --info --text="Server running on http://127.0.0.1"
op=$(zenity --list --title="ImmersiveX" --radiolist \
    --text="What do you want to do?" --column "selection" \
    --column "Operation" \
    FALSE CONFIGURE_EVENT \
    FALSE REGISTER_USERS \
    FALSE START_READING \
    FALSE EVENT_ANALYSIS \
    --width=600 --height=400)
# If user clicks on cancel or exit, then terminate the server and exit, otherwise keep showing the menu
while [ "$?" -ne 1 ]; do
    if [ $op = CONFIGURE_EVENT ]
        then chromium-browser "127.0.0.1/admin"
    elif [ $op = REGISTER_USERS ]
        then sudo java -jar ~/Documents/immersiveX/UserRegistrationIX2.0.jar
    elif [ $op = START_READING ]
        then sudo java -jar ~/Documents/immersiveX/ServerIX2.0.jar
    else
        chromium-browser "127.0.0.1"
    fi
    op=$(zenity --list --title="ImmersiveX" --radiolist \
        --text="What do you want to do?" --column "selection" \
        --column "Operation" \
        FALSE CONFIGURE_EVENT \
        FALSE REGISTER_USERS \
        FALSE START_READING \
        FALSE EVENT_ANALYSIS \
        --width=600 --height=400)
done
sudo killall python3; exit
