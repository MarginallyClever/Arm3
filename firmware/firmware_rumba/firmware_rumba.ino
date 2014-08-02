//------------------------------------------------------------------------------
// Arm3 - Three Axis Robot Arm based on code from 6 Axis CNC Demo v2
// dan@marginallycelver.com 2014-03-23
//------------------------------------------------------------------------------
// Copyright at end of file.
// please see http://www.marginallyclever.com/ for more information.


//------------------------------------------------------------------------------
// CONSTANTS
//------------------------------------------------------------------------------
#define VERBOSE              (0)  // increase this number to get more output

// serial comms
#define BAUD                 (57600)  // How fast is the Arduino talking?
#define MAX_BUF              (64)  // What is the longest message Arduino can store?

// speeds & acceleration control
#define MIN_STEP_DELAY       (100) // fastest the motors can move
#define MAX_FEEDRATE         (1000000/MIN_STEP_DELAY)
#define MIN_FEEDRATE         (0.01)
#define DEFAULT_FEEDRATE     (200)
#define DEFAULT_ACCEL        (40)   // how much to accelerate/decelerate

// machine dimensions
#define BASE_TO_SHOULDER_X   (5.37)  // measured in solidworks
#define BASE_TO_SHOULDER_Z   (9.55)  // measured in solidworks
#define SHOULDER_TO_ELBOW    (25.0)
#define ELBOW_TO_WRIST       (25.0)
#define WRIST_TO_FINGER      (4.0)
#define FINGER_TO_FLOOR      (0.5)
#define STEP_MICROSTEPPING   (16.0)  // microstepping
#define MOTOR_STEPS_PER_TURN (400.0)
#define GEAR_RATIO           (5.0)
#define STEPS_PER_TURN       (MOTOR_STEPS_PER_TURN * STEP_MICROSTEPPING * GEAR_RATIO)

// arduino pins for motor control
#define MOTHERBOARD 1  // RUMBA
#define MOTHERBOARD 2  // RAMPS 1.4

#if MOTHERBOARD == 1  // RUMBA
#define NUM_AXIES        (6)
#define MOTOR_0_DIR_PIN  (16)
#define MOTOR_0_STEP_PIN (17)
#define MOTOR_1_DIR_PIN  (47)
#define MOTOR_1_STEP_PIN (54)
#define MOTOR_2_DIR_PIN  (56)
#define MOTOR_2_STEP_PIN (57)
#define MOTOR_3_DIR_PIN  (22)
#define MOTOR_3_STEP_PIN (23)
#define MOTOR_4_DIR_PIN  (25)
#define MOTOR_4_STEP_PIN (26)
#define MOTOR_5_DIR_PIN  (28)
#define MOTOR_5_STEP_PIN (29)
#endif

#if MOTHERBOARD == 2  // RAMPS 1.4
#define NUM_AXIES        (4)
#define MOTOR_0_DIR_PIN  (55)
#define MOTOR_0_STEP_PIN (54)
#define MOTOR_1_DIR_PIN  (61)
#define MOTOR_1_STEP_PIN (60)
#define MOTOR_2_DIR_PIN  (48)
#define MOTOR_2_STEP_PIN (46)
#define MOTOR_3_DIR_PIN  (28)
#define MOTOR_3_STEP_PIN (26)
#endif


// split long lines into pieces to make them more correct.
#define CM_PER_SEGMENT       (0.5)


// math defines
#define TWOPI                (PI*2)
#define RAD2DEG              (180.0/PI)

// EEPROM settings
#define EEPROM_VERSION       (3)  // firmware version
#define ADDR_VERSION         (0)
#define ADDR_UUID            (4)


// calibration settings
//*
//#define HOME_X               (13.3)
#define HOME_Y               (0)
//#define HOME_Z               (23.91+FINGER_TO_FLOOR)
#define HOME_X               (12.850)  // WAS 13.05
#define HOME_Z               (22.2)

//*/
/*
#define HOME_X (25.0+WRIST_TO_FINGER)
#define HOME_Y 0
#define HOME_Z -0.5
//*/

#define MAX_TOOLS            (6)



//------------------------------------------------------------------------------
// INCLUDES
//------------------------------------------------------------------------------
#include "Vector3.h"


//------------------------------------------------------------------------------
// STRUCTS
//------------------------------------------------------------------------------
// for line()
typedef struct {
  long delta;
  long over;
  int dir;
} Axis;


typedef struct {
  int step_pin;
  int dir_pin;
  int enable_pin;
  int limit_switch_pin;
  int limit_switch_state;
  int flip;
} Motor;


typedef struct {
  Axis a[NUM_AXIES];
  long steps;
  long steps_left;
  long feed_rate;
} Segment;



//------------------------------------------------------------------------------
// GLOBALS
//------------------------------------------------------------------------------
Axis a[NUM_AXIES];  // for line()
Axis atemp;  // for line()


Motor motors[NUM_AXIES];
char *motor_names="XYZABC";


char buffer[MAX_BUF];  // where we store the message until we get a ';'
int sofar;  // how much is in the buffer


float px, py, pz;  // step count
float ox, oy, oz;  // reported position

// speeds
float fr=0;  // human version
long step_delay;  // milliseconds
long step_delay_ms;  // milliseconds
long step_delay_us;  // microseconds

// settings
char mode_abs=1;  // absolute mode?

Vector3 tool_offset[MAX_TOOLS];
int current_tool=0;

// for acceleration
long g_accel_until;
long g_decel_after;
long g_step_count;

long g_start_delay;


//------------------------------------------------------------------------------
// METHODS
//------------------------------------------------------------------------------


/**
 * delay for the appropriate number of microseconds
 * @input ms how many milliseconds to wait
 */
void pause(long ms) {
  delay(ms/1000);
  delayMicroseconds(ms%1000);  // delayMicroseconds doesn't work for values > ~16k.
}


void pause_efficient() {
  if(step_delay_ms>0) delay(step_delay_ms);
  delayMicroseconds(step_delay_us);
}


/**
 * Set the feedrate (speed motors will move)
 * @input nfr the new speed in steps/second
 */
void set_feedrate(float nfr) {
  if(fr==nfr) return;  // same as last time?  quit now.

  if(nfr>MAX_FEEDRATE || nfr<MIN_FEEDRATE) {  // don't allow crazy feed rates
    Serial.print(F("New feedrate must be greater than "));
    Serial.print(MIN_FEEDRATE);
    Serial.print(F("steps/s and less than "));
    Serial.print(MAX_FEEDRATE);
    Serial.println(F("steps/s."));
    return;
  }
  
  step_delay = 1000000.0/nfr;
  if( step_delay < 10000 ) {
    step_delay_us = step_delay % 10000;
    step_delay_ms = 0;
  } else {
    step_delay_us = step_delay % 1000;
    step_delay_ms = step_delay / 1000;
  }
  fr=nfr;
}


/**
 * Set the logical position
 * @input npx new position x
 * @input npy new position y
 */
void set_position(float npx,float npy,float npz) {
  // here is a good place to add sanity tests
  ox=npx;
  oy=npy;
  oz=npz;
}


// returns angle of dy/dx as a value from 0...2PI
float atan3(float dy,float dx) {
  float a=atan2(dy,dx);
  if(a<0) a=(PI*2.0)+a;
  return a;
}



/**
 * Convert cartesian XYZ to robot motor steps.
 * @return 0 if successful, 1 if the IK solution cannot be found.
 */
int IK(float x, float y,float z,float &angle_0,float &angle_1,float &angle_2) {
  // if we know the position of the wrist relative to the shoulder
  // we can use intersection of circles to find the elbow.
  // once we know the elbow position we can find the angle of each joint.
  // each angle can be converted to motor steps.
    
  // use intersection of circles to find two possible elbow points.
  // the two circles are the bicep (shoulder-elbow) and the forearm (elbow-wrist)
  // the distance between circle centers is d  
  Vector3 arm_plane(x,y,0);
  arm_plane.Normalize();

  // the finger (attachment point for the tool) is a short distance in "front" of the wrist joint
  Vector3 wrist(x,y,z);
  wrist -= arm_plane * WRIST_TO_FINGER;

  Vector3 shoulder = arm_plane;
  shoulder *= BASE_TO_SHOULDER_X;
  shoulder.z = BASE_TO_SHOULDER_Z;
  
  Vector3 es = wrist - shoulder;
  
  float d = es.Length();
  
  //a = (r0r0 - r1r1 + d*d ) / (2 d) 
  float r1=ELBOW_TO_WRIST;  // circle 1 centers on wrist
  float r0=SHOULDER_TO_ELBOW;  // circle 0 centers on shoulder
  if( d > ELBOW_TO_WRIST + SHOULDER_TO_ELBOW ) {
    // The points are impossibly far apart, no solution can be found.
    return 1;
  }
    
  float a = ( r0 * r0 - r1 * r1 + d*d ) / ( 2.0*d );
  // find the midpoint
  Vector3 mid = es * ( a / d ) + shoulder;
  // with a and r0 we can find h, the distance from midpoint to the intersections.
  float h=sqrt(r0*r0-a*a);
  // the distance h on a line orthogonal to n and plane_normal gives us the two intersections.
  Vector3 n(-arm_plane.y,arm_plane.x,0);
  Vector3 r = es ^ n;
  r.Normalize();
  Vector3 elbow = mid - r * h;
  //Vector3 elbow = mid + r * h;
    
  // find the shoulder angle using atan3(elbow-shoulder)
  Vector3 temp = elbow - shoulder;
  temp.Normalize();
  float ax=temp | arm_plane;
  float ay=temp.z;
  angle_1 = atan2(ay,ax);

  // find the elbow angle
  temp = elbow - wrist;
  temp.Normalize();
  float bx = temp | arm_plane;
  float by = temp.z;
  angle_0 = atan2(by,bx);
  
  // the easiest part
  angle_2 = atan2(y,x);

  // angles are now in radians
  
#if VERBOSE > 2
  Serial.print(x);
  Serial.print("\t");
  Serial.print(y);
  Serial.print("\t");
  Serial.print(z);
  Serial.print("\t=\t");
  Serial.print(angle_0*RAD2DEG);
  Serial.print("\t");
  Serial.print(angle_1*RAD2DEG);
  Serial.print("\t");
  Serial.print(angle_2*RAD2DEG);
  Serial.print("\n");
#endif

  angle_0 *= -STEPS_PER_TURN/TWOPI;
  angle_1 *= STEPS_PER_TURN/TWOPI;
  angle_2 *= -STEPS_PER_TURN/TWOPI;

  return 0;
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


void arm_line(float x,float y,float z) {
  float a,b,c;
  IK(x,y,z,a,b,c);
  set_position(x,y,z);
  line(a,b,c);
}


void line_safe(float x,float y,float z) {
  x -= tool_offset[current_tool].x;
  y -= tool_offset[current_tool].y;
  z -= tool_offset[current_tool].z;
  
  // split up long lines to make them straighter?
  float dx=x-ox;
  float dy=y-oy;
  float dz=z-oz;
/*
  Serial.print("dx ");  Serial.println(dx);
  Serial.print("dy ");  Serial.println(dy);
  Serial.print("dz ");  Serial.println(dz);

  Serial.print("posx ");  Serial.println(posx);
  Serial.print("posy ");  Serial.println(posy);
  Serial.print("posz ");  Serial.println(posz);
*/
  float len=sqrt(dx*dx+dy*dy+dz*dz);
#if VERBOSE > 2
  Serial.print("LEN ");  Serial.println(len);
#endif

  long pieces=floor(len/CM_PER_SEGMENT);
#if VERBOSE > 2
  Serial.print("pieces ");  Serial.println(pieces);
#endif

  long original_delay = step_delay_us;
  step_delay_us = g_start_delay;
  
  float x0=ox;
  float y0=oy;
  float z0=oz;
  float a;
  long j;
  
  float a0=px,b0=py,c0=pz;
  float a1,b1,c1;
  long ta,tb,tc;
  long total_steps=0;

  // count total steps
  for(j=1;j<=pieces;++j) {
    a=(float)j/(float)pieces;
    IK(dx*a+x0,
       dy*a+y0,
       dz*a+z0,
       a1,b1,c1);
    ta=abs(a1-a0);
    tb=abs(b1-b0);
    tc=abs(c1-c0);
    a0=a1;
    b0=b1;
    c0=c1;
    
    if( ta > tb ) {
      if( ta > tc ) total_steps += ta;
      else total_steps += tc;
    } else if( tb > tc ) {
      total_steps += tb;
    } else {
      total_steps += tc;
    }
    
#if VERBOSE > 3
    Serial.print(a1); Serial.print("\t");  
    Serial.print(b1); Serial.print("\t");  
    Serial.print(c1); Serial.print("\t"); 
    Serial.print(ta); Serial.print("\t");  
    Serial.print(tb); Serial.print("\t");  
    Serial.print(tc); Serial.print("\n");  
#endif
  }

  // find steps to accelerate and decelerate
  long max_accel_steps = abs( step_delay_us - original_delay ) / DEFAULT_ACCEL;
  if( max_accel_steps > total_steps/2 ) {
    max_accel_steps = total_steps/2;
  }
  g_accel_until = max_accel_steps;
  g_decel_after = total_steps - max_accel_steps;
  g_step_count=0;
  
#if VERBOSE > 3
  Serial.print("max=");  Serial.println(total_steps);
  Serial.print("total=");  Serial.println(total_steps);
  Serial.print("until=");  Serial.println(g_accel_until);
  Serial.print("after=");  Serial.println(g_decel_after);
  Serial.print("now=");  Serial.println(step_delay_us);
#endif
  
  for(j=1;j<pieces;++j) {
    a=(float)j/(float)pieces;

    arm_line(dx*a+x0,
             dy*a+y0,
             dz*a+z0);  // should accelerate if possible?
  }
  arm_line(x,y,z);
  
#if VERBOSE > 3
  Serial.print("now=");  Serial.println(step_delay_us);
#endif

  // just in case
  step_delay_us = original_delay;
}


void arc_safe(float x,float y,float z,float cx,float cy,int dir) {
    // get radius
  float dx = ox - cx;
  float dy = oy - cy;
  float radius=sqrt(dx*dx+dy*dy);

  // find angle of arc (sweep)
  float angle1=atan3(dy,dx);
  float angle2=atan3(y-cy,x-cx);
  float theta=angle2-angle1;
  
  if(dir>0 && theta<0) angle2+=2*PI;
  else if(dir<0 && theta>0) angle1+=2*PI;
  
  theta=angle2-angle1;
  
  // get length of arc
  // float circ=PI*2.0*radius;
  // float len=theta*circ/(PI*2.0);
  // simplifies to
  float len = abs(theta) * radius;

  int i, segments = floor( len / CM_PER_SEGMENT );
 
  float nx, ny, nz, angle3, scale;

  for(i=0;i<segments;++i) {
    // interpolate around the arc
    scale = ((float)i)/((float)segments);
    
    angle3 = ( theta * scale ) + angle1;
    nx = cx + cos(angle3) * radius;
    ny = cy + sin(angle3) * radius;
    nz = ( z - oz ) * scale + oz;
    // send it to the planner
    line_safe(nx,ny,nz);
  }
  
  line_safe(x,y,z);
}


/**
 * Look for character /code/ in the buffer and read the float that immediately follows it.
 * @return the value found.  If nothing is found, /val/ is returned.
 * @input code the character to look for.
 * @input val the return value if /code/ is not found.
 **/
float parsenumber(char code,float val) {
  char *ptr=buffer;
  while(ptr && *ptr && ptr<buffer+sofar) {
    if(*ptr==code) {
      return atof(ptr+1);
    }
    ptr=strchr(ptr,' ')+1;
  }
  return val;
} 


void tool_set_offset(int axis,float x,float y,float z) {
  tool_offset[axis].x=x;
  tool_offset[axis].y=y;
  tool_offset[axis].z=z;
  Serial.print('X');
  Serial.print(x);
  Serial.print(" Y");
  Serial.print(y);
  Serial.print(" Z");
  Serial.println(z);
}


Vector3 get_end_plus_offset() {
  return Vector3(tool_offset[current_tool].x + ox,
                 tool_offset[current_tool].y + oy,
                 tool_offset[current_tool].z + oz);
}


void tool_change(int tool_id) {
  if(tool_id < 0) tool_id=0;
  if(tool_id > MAX_TOOLS) tool_id=MAX_TOOLS;
  current_tool=tool_id;
}


/**
 * write a string followed by a float to the serial line.  Convenient for debugging.
 * @input code the string.
 * @input val the float.
 */
void output(char *code,float val) {
  Serial.print(code);
  Serial.println(val);
}


/**
 * print the current position, feedrate, and absolute mode.
 */
void where() {
  Vector3 offset=get_end_plus_offset();
  output("X",offset.x);
  output("Y",offset.y);
  output("Z",offset.z);
  output("F",fr);
  Serial.println(mode_abs?"ABS":"REL");
} 


long GetGUID() {
  return EEPROM_readLong(ADDR_UUID);
}


void SetGUID(long newid) {
  EEPROM_writeLong(ADDR_UUID,newid);
}


/**
 * display helpful information
 */
void help() {
  Serial.print(F("Arm3:"));
  Serial.print(EEPROM_GetGUID());
  Serial.print(':');
  Serial.println(EEPROM_GetVersion());
  Serial.println(F("Commands:"));
  Serial.println(F("M18; - disable motors"));
  Serial.println(F("M100; - this help message"));
  Serial.println(F("M114; - report position and feedrate"));
  Serial.println(F("M1000; - set GUID"));
  Serial.println(F("F, G00, G01, G04, G17, G18, G28, G54-G59, G90-G92, M06 as described by http://en.wikipedia.org/wiki/G-code"));
}


/**
 * Read the input buffer and find any recognized commands.  One G or M command per line.
 */
void processCommand() {
  int cmd = parsenumber('G',-1);
  switch(cmd) {
  case  0: 
  case  1: { // line
    set_feedrate(parsenumber('F',fr));
    Vector3 offset=get_end_plus_offset();
    line_safe( parsenumber('X',(mode_abs?offset.x:0)) + (mode_abs?0:offset.x),
               parsenumber('Y',(mode_abs?offset.y:0)) + (mode_abs?0:offset.y),
               parsenumber('Z',(mode_abs?offset.z:0)) + (mode_abs?0:offset.z) );
    break;
  }
  case  2:
  case  3: {  // arc
    set_feedrate(parsenumber('F',fr));
    Vector3 offset=get_end_plus_offset();
    arc_safe( parsenumber('X',(mode_abs?offset.x:0)) + (mode_abs?0:offset.x),
              parsenumber('Y',(mode_abs?offset.y:0)) + (mode_abs?0:offset.y),
              parsenumber('Z',(mode_abs?offset.z:0)) + (mode_abs?0:offset.z),
              parsenumber('I',(mode_abs?offset.x:0)) + (mode_abs?0:offset.x),
              parsenumber('J',(mode_abs?offset.y:0)) + (mode_abs?0:offset.y),
              (cmd==2) );  // direction
    break;
  }
  case  4:  pause(parsenumber('P',0)*1000000);  break;  // dwell
  case 28:  find_home();  break;
  case 54:
  case 55:
  case 56:
  case 57:
  case 58:
  case 59: {  // 54-59 tool offsets
    int tool_id=cmd-54;
    tool_set_offset(tool_id,parsenumber('X',tool_offset[tool_id].x),
                            parsenumber('Y',tool_offset[tool_id].y),
                            parsenumber('Z',tool_offset[tool_id].z));
    break;
  }
  case 90:  mode_abs=1;  break;  // absolute mode
  case 91:  mode_abs=0;  break;  // relative mode
  case 92:  { // set logical position
    Vector3 offset = get_end_plus_offset();
    set_position( parsenumber('X',offset.x),
                  parsenumber('Y',offset.y),
                  parsenumber('Z',offset.z) );
    IK(ox,oy,oz,px,py,pz);
    break;
  }
  default:  break;
  }

  cmd = parsenumber('M',-1);
  switch(cmd) {
  case 6:  tool_change(parsenumber('T',0));  break;
  case 17:  motor_enable();  break;
  case 18:  motor_disable();  break;
  case 100:  help();  break;
  case 114:  where();  break;
  case 1000:  EEPROM_SetGUID(parsenumber('U',0));  break;
  case 1001:  
  case 1002:  
  case 1003:  
  case 1004:  
  case 1005:  
  case 1006:  {
    int id=cmd-1000;
    int dir=parsenumber('D',1)==1?1:-1;
    for(int i=0;i<STEPS_PER_TURN;i++) {
      motor_onestep(id,dir);
      delay(5);
    }
    break;
  }
  default:  break;
  }
}


/**
 * prepares the input buffer to receive a new message and tells the serial connected device it is ready for more.
 */
void ready() {
  sofar=0;  // clear input buffer
  Serial.print(F(">"));  // signal ready to receive input
}


void tools_setup() {
  for(int i=0;i<MAX_TOOLS;++i) {
    tool_offset[i].Set(0,0,0);
  }
}


/**
 * First thing this machine does on startup.  Runs only once.
 */
void setup() {
  EEPROM_Setup();
  
  Serial.begin(BAUD);  // open coms

  motor_setup();
  tools_setup();
  
  set_feedrate(DEFAULT_FEEDRATE);  // set default speed
  g_start_delay = step_delay_us;
  
  set_position(HOME_X,HOME_Y,HOME_Z);  // set staring position
  IK(ox,oy,oz,px,py,pz);
  help();  // say hello
  ready();
}


/**
 * After setup() this machine will repeat loop() forever.
 */
void loop() {
  // listen for serial commands
  while(Serial.available() > 0) {  // if something is available
    char c=Serial.read();  // get it
    Serial.print(c);  // repeat it back so I know you got the message
    if(sofar<MAX_BUF) buffer[sofar++]=c;  // store it
    if(buffer[sofar-1]==';') break;  // entire message received
  }

  if(sofar>0 && buffer[sofar-1]==';') {
    // we got a message and it ends with a semicolon
    buffer[sofar]=0;  // end the buffer so string functions work right
    Serial.print(F("\r\n"));  // echo a return character for humans
    processCommand();  // do something with the command
    ready();
  }
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
