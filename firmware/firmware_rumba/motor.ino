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
  motors[0].step_pin=MOTOR_0_STEP_PIN;
  motors[0].dir_pin=MOTOR_0_DIR_PIN;
  motors[0].enable_pin=MOTOR_0_ENABLE_PIN;
  motors[0].limit_switch_pin=MOTOR_0_LIMIT_PIN;
  motors[0].flip=-1;

  motors[1].step_pin=MOTOR_1_STEP_PIN;
  motors[1].dir_pin=MOTOR_1_DIR_PIN;
  motors[1].enable_pin=MOTOR_1_ENABLE_PIN;
  motors[1].limit_switch_pin=MOTOR_1_LIMIT_PIN;
  motors[1].flip=-1;

  motors[2].step_pin=MOTOR_2_STEP_PIN;
  motors[2].dir_pin=MOTOR_2_DIR_PIN;
  motors[2].enable_pin=MOTOR_2_ENABLE_PIN;
  motors[2].limit_switch_pin=MOTOR_2_LIMIT_PIN;
  motors[2].flip=1;

  motors[3].step_pin=MOTOR_3_STEP_PIN;
  motors[3].dir_pin=MOTOR_3_DIR_PIN;
  motors[3].enable_pin=MOTOR_3_ENABLE_PIN;
  motors[3].limit_switch_pin=MOTOR_3_LIMIT_PIN;
  motors[3].flip=1;
#if NUM_AXIES > 3
  motors[4].step_pin=MOTOR_4_STEP_PIN;
  motors[4].dir_pin=MOTOR_4_DIR_PIN;
  motors[4].enable_pin=MOTOR_4_ENABLE_PIN;
  motors[4].limit_switch_pin=MOTOR_4_LIMIT_PIN;
  motors[4].flip=1;
#endif
#if NUM_AXIES > 4
  motors[5].step_pin=MOTOR_5_STEP_PIN;
  motors[5].dir_pin=MOTOR_5_DIR_PIN;
  motors[5].enable_pin=MOTOR_5_ENABLE_PIN;
  motors[5].limit_switch_pin=MOTOR_5_LIMIT_PIN;
  motors[5].flip=1;
#endif
  
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
 * Uses bresenham's line algorithm to move both motors
 * @input newx the destination x position
 * @input newy the destination y position
 **/
void line(float newx,float newy,float newz) {
  a[0].delta = newx-px;
  a[1].delta = newy-py;
  a[2].delta = newz-pz;
  
  long i,j,steps_total=0;
  long delta[NUM_AXIES];
  long over[NUM_AXIES];

  for(i=0;i<NUM_AXIES;++i) {
    delta[i] = abs(a[i].delta);
    a[i].dir = ( a[i].delta * motors[i].flip ) > 0 ? LOW : HIGH;
    if( steps_total < delta[i] ) steps_total = delta[i];
    over[i]=0;
  }
  

#if VERBOSE > 1
  Serial.print("Total steps=");
  Serial.println(steps_total);
  Serial.println(F("Start >"));
#endif

  digitalWrite( MOTOR_0_DIR_PIN, a[0].dir );
  digitalWrite( MOTOR_1_DIR_PIN, a[1].dir );
  digitalWrite( MOTOR_2_DIR_PIN, a[2].dir );
  digitalWrite( MOTOR_3_DIR_PIN, a[3].dir );
#if NUM_AXIES >= 4
  digitalWrite( MOTOR_4_DIR_PIN, a[4].dir );
#endif
#if NUM_AXIES >= 5
  digitalWrite( MOTOR_5_DIR_PIN, a[5].dir );
#endif
  
#if VERBOSE > 3
  Serial.print("\t");  Serial.print(g_step_count);
  Serial.print("\t");  Serial.print(step_delay_us);
  Serial.print("\n");
#endif
  
  for( i=0; i<steps_total; ++i ) {
    // M0
    over[0] += delta[0];
    if(over[0] >= steps_total) {
      digitalWrite(MOTOR_0_STEP_PIN,LOW);
      over[0] -= steps_total;
      digitalWrite(MOTOR_0_STEP_PIN,HIGH);
    }
    // M1
    over[1] += delta[1];
    if(over[1] >= steps_total) {
      digitalWrite(MOTOR_1_STEP_PIN,LOW);
      over[1] -= steps_total;
      digitalWrite(MOTOR_1_STEP_PIN,HIGH);
    }
    // M2
    over[2] += delta[2];
    if(over[2] >= steps_total) {
      digitalWrite(MOTOR_2_STEP_PIN,LOW);
      over[2] -= steps_total;
      digitalWrite(MOTOR_2_STEP_PIN,HIGH);
    }
    // M3
    over[3] += delta[3];
    if(over[3] >= steps_total) {
      digitalWrite(MOTOR_3_STEP_PIN,LOW);
      over[3] -= steps_total;
      digitalWrite(MOTOR_3_STEP_PIN,HIGH);
    }
#if NUM_AXIES >= 4
    // M4
    over[4] += delta[4];
    if(over[4] >= steps_total) {
      digitalWrite(MOTOR_4_STEP_PIN,LOW);
      over[4] -= steps_total;
      digitalWrite(MOTOR_4_STEP_PIN,HIGH);
    }
#endif
#if NUM_AXIES >= 5
    // M5
    over[5] += delta[5];
    if(over[5] >= steps_total) {
      digitalWrite(MOTOR_5_STEP_PIN,LOW);
      over[5] -= steps_total;
      digitalWrite(MOTOR_5_STEP_PIN,HIGH);
    }
#endif
    
    ++g_step_count;
    if(g_step_count<g_accel_until) {
      step_delay_us -= DEFAULT_ACCEL;
    }
    if(g_step_count>=g_decel_after) {
      step_delay_us += DEFAULT_ACCEL;
    }
    pause_efficient();
    
#if VERBOSE > 2
    Serial.println(i);
#endif
  }

  px=newx;
  py=newy;
  pz=newz;

#if VERBOSE > 1
  Serial.println(F("< Done."));
#endif
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
