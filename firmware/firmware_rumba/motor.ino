//------------------------------------------------------------------------------
// Arm3 - Three Axis Robot Arm based on code from 6 Axis CNC Demo v2
// dan@marginallycelver.com 2014-03-23
//------------------------------------------------------------------------------
// Copyright at end of file.
// please see http://www.marginallyclever.com/ for more information.



//------------------------------------------------------------------------------
// METHODS
//------------------------------------------------------------------------------
/**
 * Supports movement with both styles of Motor Shield
 * @input newx the destination x position
 * @input newy the destination y position
 **/
void motor_onestep(int motor_id,int dir) {
#if VERBOSE > 2
  Serial.print(motor_names[motor_id]);
#endif
  Motor &a = motors[motor_id];

  digitalWrite(a.dir_pin,dir*a.flip>0?LOW:HIGH);
  digitalWrite(a.step_pin,HIGH);
  digitalWrite(a.step_pin,LOW);
}


void motor_setup() {
  // set up the pins
  motors[0].step_pin=17;
  motors[0].dir_pin=16;
  motors[0].enable_pin=48;
  motors[0].limit_switch_pin=37;
  motors[0].flip=-1;

  motors[1].step_pin=54;
  motors[1].dir_pin=47;
  motors[1].enable_pin=55;
  motors[1].limit_switch_pin=36;
  motors[1].flip=-1;

  motors[2].step_pin=57;
  motors[2].dir_pin=56;
  motors[2].enable_pin=62;
  motors[2].limit_switch_pin=35;
  motors[2].flip=1;

  motors[3].step_pin=23;
  motors[3].dir_pin=22;
  motors[3].enable_pin=27;
  motors[3].limit_switch_pin=34;
  motors[3].flip=1;

  motors[4].step_pin=26;
  motors[4].dir_pin=25;
  motors[4].enable_pin=24;
  motors[4].limit_switch_pin=33;
  motors[4].flip=1;

  motors[5].step_pin=29;
  motors[5].dir_pin=28;
  motors[5].enable_pin=39;
  motors[5].limit_switch_pin=32;
  motors[5].flip=1;
  
  for(int i=0;i<NUM_AXIES;++i) {  
    // set the motor pin & scale
    pinMode(motors[i].step_pin,OUTPUT);
    pinMode(motors[i].dir_pin,OUTPUT);
    pinMode(motors[i].enable_pin,OUTPUT);
    // set the switch pin
    motors[i].limit_switch_state=HIGH;
    pinMode(motors[i].limit_switch_pin,INPUT);
    digitalWrite(motors[i].limit_switch_pin,HIGH);
    motors[i].limit_switch_state = digitalRead(motors[i].limit_switch_pin);
  }
  motor_enable();
}


/**
 * Grips the power on the motors
 **/
void motor_enable() {
  int i;
  for(i=0;i<NUM_AXIES;++i) {
    digitalWrite(motors[i].enable_pin,LOW);
  }
}


/**
 * Releases the power on the motors
 **/
void motor_disable() {
  int i;
  for(i=0;i<NUM_AXIES;++i) {
    digitalWrite(motors[i].enable_pin,HIGH);
  }
}


void find_home() {
  // hit switch
  while(digitalRead(motors[1].limit_switch_pin)==HIGH) {
    motor_onestep(1,1);
    pause(500);
  }
  // Back off switch
  while(digitalRead(motors[1].limit_switch_pin)==LOW) {
    motor_onestep(1,-1);
    delay(1);
  }
#if VERBOSE > 1
  Serial.println(F("Found 1"));
#endif

  // hit switch
  while(digitalRead(motors[0].limit_switch_pin)==HIGH) {
    motor_onestep(0,-1);
    pause(500);
  }
  // Back off switch
  while(digitalRead(motors[0].limit_switch_pin)==LOW) {
    motor_onestep(0,1);
    delay(1);
  }
#if VERBOSE > 1
  Serial.println(F("Found 0"));
#endif

  // hit switch
  while(digitalRead(motors[2].limit_switch_pin)==HIGH) {
    motor_onestep(2,-1);
    pause(500);
  }
  // Back off switch
  while(digitalRead(motors[2].limit_switch_pin)==LOW) {
    motor_onestep(2,1);
    delay(1);
  }
#if VERBOSE > 1
  Serial.println(F("Found 2"));
#endif

  set_position(HOME_X,HOME_Y,HOME_Z);  // set staring position
  IK(ox,oy,oz,px,py,pz);

  Serial.println(F("Found home."));
}


/**
* This file is part of Arm3.
*
* Arm3 is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Arm3 is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Foobar. If not, see <http://www.gnu.org/licenses/>.
*/
