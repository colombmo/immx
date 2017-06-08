# Immersive Experience
___________________
Welcome to Immersive Experience. Here is a structure of the code and what is the purpose of each part of it. For further informations refer to the manual (https://github.com/colombmo/immx/blob/master/Manual.pdf) or to the comments in the code.
### Code structure
##### On Raspberry Pi
###### IE2.0-client - java
------------------------------------------------
> **Client.java**: class which handles connections between RPi and server via socket, handle RFID antenna setup, readings and filtering of data from RFID reader.
**OldRecords.java**: helper class to handle filtering of readings.
**Config.json**: Configuration file, it has to contain two options “raspberryId”, the identifier number of the RPi being configured, and “comPort”, the serial port the RFID antenna is communicating to, usually /dev/ttyUSB0.

##### On Server
###### IE2.0-server - java
-------
> **GUI.java**: Create Graphical User Interface to choose some parameters for the data collection and to launch/stop it. On start button clicked, launch a new thread and a socket connection for each RPi.
**SocketFactory.java**: start socket connection between server and RPi, and launch visualizations.
**ServerThread.java**: a thread for each RPi. Receive data from RPis and send it both to the live visualization classes and to the Django database.
**Visualization.java | GradientsVisualization.java**: Classes handling live visualization. The first one shows the color of the detected RFID tag next to each sensor on a map, the other the concentration of people inside a room with a sensor placed at its entrance.
**ch.sichh.helpers.\***: some helper classes for the previous listed classes.

###### ~/Documents/immersiveX – Django (python + HTML + javascript)
-----------------
>**ImmersiveExperience/settings.py**: Settings of the webserver.
**hubnet/templates/\***: HTML templates to show webpages with custom content in a browser, where the offline visualizations are defined.
**Hubnet/models.py**: A description of the objects to be saved in the database, with their attributes. 
**Hubnet/urls.py**: Used to associate some URLs to a view, with some optional parameters.
**Hubnet/views.py**: Python functions to elaborate data got via URLs, or data from database, and save it to the database or use it as a parameter to be passed to a template. 

###### IE2.0-UserRegistration - java
----------------------------------------
>Handle the connection between Server and RFID reader, to read some RFID tags. Then associate them to a specific group and store this association to the database.
