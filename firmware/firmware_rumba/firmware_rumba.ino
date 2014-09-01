//------------------------------------------------------------------------------
// Arm3 - Three Axis Robot Arm based on code from 6 Axis CNC Demo v2
// dan@marginallycelver.com 2014-03-23
//------------------------------------------------------------------------------
// Copyright at end of file.
// please see http://www.marginallyclever.com/ for more information.

//------------------------------------------------------------------------------
// INCLUDES
//------------------------------------------------------------------------------
#include "configure.h"


//------------------------------------------------------------------------------
// GLOBALS
//------------------------------------------------------------------------------
Axis a[NUM_AXIES];  // for line()
Axis atemp;  // for line()


Motor motors[NUM_AXIES];
char *motor_names="XYZABC";


char buffer[MAX_BUF];  // where we store the message until we get a ';'
int sofar;  // how much is in the buffer

long line_number=0;

float px, py, pz;  // step count
float ox, oy, oz;  // reported position

// speeds
float feed_rate=DEFAULT_FEEDRATE;  // how fast the EE moves in cm/s
float acceleration=DEFAULT_ACCELERATION;

// settings
char mode_abs=1;  // absolute mode?

Vector3 tool_offset[MAX_TOOLS];
int current_tool=0;


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


// force this thread to do nothing until all the queued segments are processed.
void wait_for_segment_buffer_to_empty() {
  while( current_segment != last_segment );
}


/**
 * Set the feedrate (speed motors will move)
 * @input nfr the new speed in steps/second
 */
float feedrate(float nfr) {
  if(feed_rate==nfr) return nfr;  // same as last time?  quit now.

  if(nfr>MAX_FEEDRATE) {
    Serial.print(F("Feedrate set to maximum ("));
    Serial.print(MAX_FEEDRATE);
    Serial.println(F("steps/s)"));
    nfr=MAX_FEEDRATE;
  }
  if(nfr<MIN_FEEDRATE) {  // don't allow crazy feed rates
    Serial.print(F("Feedrate set to minimum ("));
    Serial.print(MIN_FEEDRATE);
    Serial.println(F("steps/s)"));
    nfr=MIN_FEEDRATE;
  }
  feed_rate=nfr;
  
  return feed_rate;
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
  IK(ox,oy,oz,px,py,pz);
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




void arm_line(float x,float y,float z,float feed_rate) {
  float a,b,c;
  IK(x,y,z,a,b,c);
  set_position(x,y,z);
  motor_prepare_segment(a,b,c,feed_rate);
}


void line_safe(float x,float y,float z,float feed_rate) {
  x -= tool_offset[current_tool].x;
  y -= tool_offset[current_tool].y;
  z -= tool_offset[current_tool].z;
  
  // split up long lines to make them straighter?
  float dx=x-ox;
  float dy=y-oy;
  float dz=z-oz;

  float len=sqrt(dx*dx+dy*dy+dz*dz);
  long pieces=floor(len * CM_PER_SEGMENT);

  float x0=ox;
  float y0=oy;
  float z0=oz;
  float a;
  long j;
  
  for(j=1;j<pieces;++j) {
    a=(float)j/(float)pieces;

    arm_line(dx*a+x0,
             dy*a+y0,
             dz*a+z0,
             feed_rate);
  }
  arm_line(x,y,z,feed_rate);
}


void arc_safe(float x,float y,float z,float cx,float cy,int dir,float feed_rate) {
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
    line_safe(nx,ny,nz,feed_rate);
  }
  
  line_safe(x,y,z,feed_rate);
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
  output("F",feed_rate);
  output("A",acceleration);
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
void parser_processCommand() {
  // blank lines
  if(buffer[0]==';') return;
  
  long cmd;
  
  // is there a line number?
  cmd=parsenumber('N',-1);
  if(cmd!=-1 && buffer[0]=='N') {  // line number must appear first on the line
    if( cmd != line_number ) {
      // wrong line number error
      Serial.print(F("BADLINENUM "));
      Serial.println(line_number);
      return;
    }
  
    // is there a checksum?
    if(strchr(buffer,'*')!=0) {
      // yes.  is it valid?
      char checksum=0;
      int c=0;
      while(buffer[c]!='*') checksum ^= buffer[c++];
      c++; // skip *
      int against = strtod(buffer+c,NULL);
      if( checksum != against ) {
        Serial.print(F("BADCHECKSUM "));
        Serial.println(line_number);
        return;
      } 
    } else {
      Serial.print(F("NOCHECKSUM "));
      Serial.println(line_number);
      return;
    }
    
    line_number++;
  }
  
  cmd = parsenumber('G',-1);
  switch(cmd) {
  case  0: 
  case  1: { // line
    acceleration = min(max(parsenumber('A',acceleration),1),2000);
    Vector3 offset=get_end_plus_offset();
    line_safe( parsenumber('X',(mode_abs?offset.x:0)) + (mode_abs?0:offset.x),
               parsenumber('Y',(mode_abs?offset.y:0)) + (mode_abs?0:offset.y),
               parsenumber('Z',(mode_abs?offset.z:0)) + (mode_abs?0:offset.z),
               feedrate(parsenumber('F',feed_rate)) );
    break;
  }
  case  2:
  case  3: {  // arc
    acceleration = min(max(parsenumber('A',acceleration),1),2000);
    Vector3 offset=get_end_plus_offset();
    arc_safe( parsenumber('X',(mode_abs?offset.x:0)) + (mode_abs?0:offset.x),
              parsenumber('Y',(mode_abs?offset.y:0)) + (mode_abs?0:offset.y),
              parsenumber('Z',(mode_abs?offset.z:0)) + (mode_abs?0:offset.z),
              parsenumber('I',(mode_abs?offset.x:0)) + (mode_abs?0:offset.x),
              parsenumber('J',(mode_abs?offset.y:0)) + (mode_abs?0:offset.y),
              (cmd==2),
              feedrate(parsenumber('F',feed_rate)) );  // direction
    break;
  }
  case  4:  {  // dwell
    wait_for_segment_buffer_to_empty();
    pause(parsenumber('S',0) + parsenumber('P',0)*1000);  
    break;
  }
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
void parser_ready() {
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
  segment_setup();
  tools_setup();
  
  set_position(HOME_X,HOME_Y,HOME_Z);  // set staring position
  help();  // say hello
  parser_ready();
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
    if(c=='\n') {
      buffer[sofar]=0;  // end the buffer so string functions work right
      //Serial.print(F("\r\n"));  // echo a return character for humans
      parser_processCommand();  // do something with the command

#ifdef ONE_COMMAND_AT_A_TIME
      wait_for_segment_buffer_to_empty();
#endif

      parser_ready();
      break;
    }
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
