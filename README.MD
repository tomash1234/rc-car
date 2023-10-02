# RC Car

## Overview
 - Hardware
   - Car
   - Phone 
   - Esp8266 
 - Software
   - Computer Program to controll
   - Android app
   - Board program

## Computer program

 - Shall be able to connect to phone (ip address, port) in local network
 - Shall be able to control car movement (forward / backward [-1.0, 1.0], steering [-1.0, 1.0])
 - Shall be able to receive images from camera
 - Shall be able to record it camera image
 - Shall be able to receive GPS info, accelerometer value (?)
 - Shall be able to receive phone info - battery percent
 - Control usingKeyboard / Gamepad / Wheel ?

## Android App
 - Shall be able to run http server
 - Shall be able to receive command for car movement
 - Shall be able to stream camera to computer program in real time 
 - Shall be able to send GPS and other phone info
 - Shall be able to connect the board and send command (speed, steering)

### API
 - `GET /camera` - return image 
 - `GET /position?timeout=` - return GPS position
 - `POST /go?motor=[speed]&steering[angle]` -  control car


## Board program
 - Shall receive info from phone and set motors and steering according it.
 - Connection using serial port or Wifi (hotspot)