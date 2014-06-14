# Arm3-v1

An open source palletizing robot based on the ABB 460.

For more information please visit [Marginally Clever's official page](http://www.marginallyclever.com/shop/robot-arms)

## starting the program

* in OSX: ./latest_official_release/OSX/start.command
* in windows: ./latest_official_release/start32.bat or ./latest_official_release/start64.bat

## Connecting to the robot

Main menu > Connection(s) > Arm0 > [choose your serial port]

* in windows the port will probably be the last COM port.
* in OSX it will be the first item listed.

Once you connect the real arm and the virtual arm should synchronize and stay that way.
The eventual goal is to be able to train the robot in the simulation without being connected.
Then connect to the robot and run the pre-planned script.  This way scripts can be shared between users.

## controls

click once in the main window.  this way the app can receive keyboard commands.

### Moving the camera
WASD will fly around the scene.  QE will fly up and down.

### Moving the arm
P will switch between forward kinematics and inverse kinematics.

In forward kinematics mode: RF, TG, and YH will turn the three motors of the robot arm.
In inverse kinematics mode: RF, TG, and YH will move the finger tip along the X, Y, and Z axies.


## copyright

This file is part of Arm3-v1.

Arm3 is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Arm3 is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Foobar. If not, see <http://www.gnu.org/licenses/>.