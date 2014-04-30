
import javax.vecmath.Vector3f;
import javax.media.opengl.GL2;

import com.jogamp.newt.event.KeyEvent;


public class Arm3Robot 
extends RobotWithSerialConnection {
	//math constants
	final float RAD2DEG = 180.0f/(float)Math.PI;

	//machine dimensions
	final float BASE_TO_SHOULDER_X   =(5.37f);  // measured in solidworks
	final float BASE_TO_SHOULDER_Z   =(9.55f);  // measured in solidworks
	final float SHOULDER_TO_ELBOW    =(25.0f);
	final float ELBOW_TO_WRIST       =(25.0f);
	final float WRIST_TO_FINGER      =(4.0f);
	final float BASE_TO_SHOULDER_MINIMUM_LIMIT = 7.5f;
	
	float HOME_X = 13.05f;
	float HOME_Y = 0;
	float HOME_Z = 22.2f;
	boolean HOME_AUTOMATICALLY_ON_STARTUP = true;
	Cylinder [] volumes = new Cylinder[6];
	
	class MotionState {
		// angle of rotation
		float angle_0 = 0;
		float angle_1 = 0;
		float angle_2 = 0;

		// robot arm coordinates.  Relative to base unless otherwise noted.
		public Vector3f finger_tip = new Vector3f(HOME_X,HOME_Y,HOME_Z);
		private Vector3f wrist = new Vector3f();
		private Vector3f elbow = new Vector3f();
		private Vector3f shoulder = new Vector3f();
		
		public Vector3f base = new Vector3f();  // relative to world
		// base orientation, affects entire arm
		public Vector3f base_forward = new Vector3f();
		public Vector3f base_up = new Vector3f();
		public Vector3f base_right = new Vector3f();
		
		// rotating entire robot
		float base_pan=0;
		float base_tilt=0;
		
		void set(MotionState other) {
			angle_0 = other.angle_0;
			angle_1 = other.angle_1;
			angle_2 = other.angle_2;
			finger_tip.set(other.finger_tip);
			wrist.set(other.wrist);
			elbow.set(other.elbow);
			shoulder.set(other.shoulder);
			base.set(other.base);
			base_forward.set(other.base_forward);
			base_up.set(other.base_up);
			base_right.set(other.base_right);
			base_pan = other.base_pan;
			base_tilt = other.base_tilt;
		}
	};
	
	protected MotionState motion_now = new MotionState();
	protected MotionState motion_future = new MotionState();
	
	boolean homed = false;
	boolean homing = false;
	boolean follow_mode = false;
	boolean arm_moved = false;
	
	// keyboard history
	boolean rDown=false;
	boolean fDown=false;
	boolean tDown=false;
	boolean gDown=false;
	boolean yDown=false;
	boolean hDown=false;
	boolean uDown=false;
	boolean jDown=false;
	boolean iDown=false;
	boolean kDown=false;
	boolean oDown=false;
	boolean lDown=false;
	
	
	public Vector3f getHome() {  return new Vector3f(HOME_X,HOME_Y,HOME_Z);  }
	
	public void setHome(Vector3f newhome) {
		HOME_X=newhome.x;
		HOME_Y=newhome.y;
		HOME_Z=newhome.z;
		RotateBase(0f,0f);
		MoveBase(new Vector3f(0,0,0));
		FinalizeMove();
	}
	
	
	//TODO check for collisions with http://geomalgorithms.com/a07-_distance.html#dist3D_Segment_to_Segment ?
	public boolean movePermitted(MotionState state) {
		// check floor
		if(state.finger_tip.z<0.25f) return false;
		// don't ceiling
		if(state.finger_tip.z>30.0f) return false;

		// check far limit
		Vector3f temp = new Vector3f(state.finger_tip);
		temp.sub(state.shoulder);
		if(temp.length() > 50) return false;
		// check near limit
		if(temp.length() < BASE_TO_SHOULDER_MINIMUM_LIMIT) return false;

		// seems doable
		if(IK(state)==false) return false;
		// angle are good?
		if(CheckAngleLimits(state)==false) return false;

		// OK
		return true;
	}
	
	
	public Arm3Robot(String name) {
		super(name);
		
		// set up bounding volumes
		for(int i=0;i<volumes.length;++i) {
			volumes[i] = new Cylinder();
		}
		volumes[0].radius=3.2f;
		volumes[1].radius=3.0f*0.575f;
		volumes[2].radius=2.2f;
		volumes[3].radius=1.15f;
		volumes[4].radius=1.2f;
		volumes[5].radius=1.0f*0.575f;
		
		RotateBase(0,0);
		IK(motion_now);
		IK(motion_future);
	}
	
	
	protected boolean CheckAngleLimits(MotionState state) {
		if (state.angle_0 < -180) return false;
		if (state.angle_0 >  180) return false;
		if (state.angle_2 <  -20) return false;
		if (state.angle_2 >  180) return false;
		if (state.angle_1 < -150) return false;
		if (state.angle_1 >   80) return false;
		if (state.angle_1 < -state.angle_2+ 10) return false;
		if (state.angle_1 > -state.angle_2+170) return false;
		
		return true;
	}
	
	
	/**
	 * Convert cartesian XYZ to robot motor steps.
	 * @input cartesian coordinates relative to the base
	 * @input results where to put resulting angles after the IK calculation
	 * @return 0 if successful, 1 if the IK solution cannot be found.
	 */
	protected boolean IK(MotionState state) {
		float a0,a1,a2;
		// if we know the position of the wrist relative to the shoulder
		// we can use intersection of circles to find the elbow.
		// once we know the elbow position we can find the angle of each joint.
		// each angle can be converted to motor steps.
	    
	    // use intersection of circles to find two possible elbow points.
	    // the two circles are the bicep (shoulder-elbow) and the forearm (elbow-wrist)
	    // the distance between circle centers is d  
	    Vector3f arm_plane = new Vector3f(state.finger_tip.x,state.finger_tip.y,0);
	    arm_plane.normalize();
	
	    // the finger (attachment point for the tool) is a short distance in "front" of the wrist joint
	    Vector3f finger = new Vector3f(state.finger_tip);
	    Vector3f s = new Vector3f(arm_plane.x,arm_plane.y,arm_plane.z);
	    s.scale(WRIST_TO_FINGER);
	    state.wrist.set(finger);
	    state.wrist.sub(s);

	    state.shoulder.set(arm_plane);
	    state.shoulder.scale(BASE_TO_SHOULDER_X);
	    state.shoulder.z = BASE_TO_SHOULDER_Z;
	    
	    // use intersection of circles to find elbow
	    Vector3f es = new Vector3f(state.wrist);
	    es.sub(state.shoulder);
	    float d = es.length();
	    float r1=ELBOW_TO_WRIST;  // circle 1 centers on wrist
	    float r0=SHOULDER_TO_ELBOW;  // circle 0 centers on shoulder
	    if( d > ELBOW_TO_WRIST + SHOULDER_TO_ELBOW ) {
	      // The points are impossibly far apart, no solution can be found.
	      return false;  // should this throw an error because it's called from the constructor?
	    }
	    float a = ( r0 * r0 - r1 * r1 + d*d ) / ( 2.0f*d );
	    // find the midpoint
	    Vector3f mid=new Vector3f(es);
	    mid.scale(a/d);
	    mid.add(state.shoulder);

	    // with a and r0 we can find h, the distance from midpoint to the intersections.
	    float h=(float)Math.sqrt(r0*r0-a*a);
	    // the distance h on a line orthogonal to n and plane_normal gives us the two intersections.
		Vector3f n = new Vector3f(-arm_plane.y,arm_plane.x,0);
		n.normalize();
		Vector3f r = new Vector3f();
		r.cross(n, es);  // check this!
		r.normalize();
		s.set(r);
		s.scale(h);

		state.elbow.set(mid);
		state.elbow.sub(s);
		//Vector3f.add(mid, s, elbow);
		    
		  // find the angle between elbow-shoulder and the horizontal
		  Vector3f temp = new Vector3f();
		  temp.set(state.elbow);
		  temp.sub(state.shoulder);		  
		  temp.normalize();
		  float ax = temp.dot(arm_plane);
		  float ay = temp.z;
		  a1 = (float) Math.atan2(ay,ax);
		
		  // find the angle between elbow-wrist and the horizontal
		  temp.set(state.elbow);
		  temp.sub(state.wrist);
		  temp.normalize();
		  float bx = temp.dot(arm_plane);
		  float by = temp.z;
		  a2 = (float) Math.atan2(by,bx);
		  
		  // the easiest part
		  a0 = (float) Math.atan2(state.finger_tip.y,state.finger_tip.x);
		
		  // all angles are in radians, I want degrees
		  state.angle_0=a0 * RAD2DEG;
		  state.angle_1=a1 * -RAD2DEG;
		  state.angle_2=a2 * RAD2DEG;
		  
		  return true;
	}

	
	protected void update_ik(float delta) {
		boolean changed=false;
		motion_future.finger_tip.set(motion_now.finger_tip);
		final float vel=5.0f;
		float dp = vel * delta;

		if (uDown) {
			motion_future.finger_tip.x -= dp;
			changed=true;
		}
		if (jDown) {
			motion_future.finger_tip.x += dp;
			changed=true;
		}
		
		if (iDown) {
			motion_future.finger_tip.y += dp;
			changed=true;
		}
		if (kDown) {
			motion_future.finger_tip.y -= dp;
			changed=true;
		}
		
		if (oDown) {
			motion_future.finger_tip.z += dp;
			changed=true;
		}
		if (lDown) {
			motion_future.finger_tip.z -= dp;
			changed=true;
		}

		if(changed==true && movePermitted(motion_future)) {
			if(motion_now.finger_tip.epsilonEquals(motion_future.finger_tip,0.1f)) {
				arm_moved=true;
			}
		} else {
			motion_future.finger_tip.set(motion_now.finger_tip);
		}
	}
		
	
	protected void FK(MotionState state) {
		Vector3f arm_plane = new Vector3f();
		Vector3f temp = new Vector3f();
		
		arm_plane.set((float)Math.cos(state.angle_0/RAD2DEG),
					  (float)Math.sin(state.angle_0/RAD2DEG),
					  0);
		state.shoulder.set(arm_plane.x*BASE_TO_SHOULDER_X,
						 arm_plane.y*BASE_TO_SHOULDER_X,
						 BASE_TO_SHOULDER_Z);
		
		temp.set(arm_plane.x*(float)Math.cos(-state.angle_1/RAD2DEG)*SHOULDER_TO_ELBOW,
				 arm_plane.y*(float)Math.cos(-state.angle_1/RAD2DEG)*SHOULDER_TO_ELBOW,
				             (float)Math.sin(-state.angle_1/RAD2DEG)*SHOULDER_TO_ELBOW);
		state.elbow.set(temp);
		state.elbow.add(state.shoulder);
		
		temp.set(arm_plane.x*(float)Math.cos(state.angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				 arm_plane.y*(float)Math.cos(state.angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				             (float)Math.sin(state.angle_2/RAD2DEG)*-ELBOW_TO_WRIST);
		state.wrist.set(temp);
		state.wrist.add(state.elbow);
		
		temp.set(arm_plane.x*WRIST_TO_FINGER,
				 arm_plane.y*WRIST_TO_FINGER,
				 0);
		state.finger_tip.set(temp);
		state.finger_tip.add(state.wrist);
	}
	
	
	protected void update_fk(float delta) {
		boolean changed=false;
		final float vel=10.0f;
		motion_future.angle_0 = motion_now.angle_0;
		motion_future.angle_1 = motion_now.angle_1;
		motion_future.angle_2 = motion_now.angle_2;

		if (rDown) {
			motion_future.angle_1 -= vel * delta;
			motion_future.angle_2 += vel * delta;
			changed=true;
		}
		if (fDown) {
			motion_future.angle_1 += vel * delta;
			motion_future.angle_2 -= vel * delta;
			changed=true;
		}
		if (tDown) {
			motion_future.angle_2 += vel * delta;
			changed=true;
		}
		if (gDown) {
			motion_future.angle_2 -= vel * delta;
			changed=true;
		}
		if (yDown) {
			motion_future.angle_0 += vel * delta;
			changed=true;
		}
		if (hDown) {
			motion_future.angle_0 -= vel * delta;
			changed=true;
		}

		if(changed==true) {
			if(CheckAngleLimits(motion_future)) {
				FK(motion_future);
				arm_moved=true;
			}
		}
	}

	
	protected void keyAction(KeyEvent e,boolean state) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_R: rDown=state;  break;
		case KeyEvent.VK_F: fDown=state;  break;
		case KeyEvent.VK_T: tDown=state;  break;
		case KeyEvent.VK_G: gDown=state;  break;
		case KeyEvent.VK_Y: yDown=state;  break;
		case KeyEvent.VK_H: hDown=state;  break;
		case KeyEvent.VK_U: uDown=state;  break;
		case KeyEvent.VK_J: jDown=state;  break;
		case KeyEvent.VK_I: iDown=state;  break;
		case KeyEvent.VK_K: kDown=state;  break;
		case KeyEvent.VK_O: oDown=state;  break;
		case KeyEvent.VK_L: lDown=state;  break;
		}
	}

	
	public void keyPressed(KeyEvent e) {
		keyAction(e,true);
   	}
	
	
	public void keyReleased(KeyEvent e) {
		keyAction(e,false);
	}
	
	
	public void PrepareMove(float delta) {
		update_fk(delta);
		update_ik(delta);
		
		// before the robot is allowed to do anything it has to be homed
		if(this.isConfirmed) {
			if(!homed) {
				if(!homing) {
					// we are not homed and we have not begun to home
					if(HOME_AUTOMATICALLY_ON_STARTUP==true) {
						// this should be sent by a human when they are ready
						this.SendCommand("G28");
						homing = true;
					}
				} else if( this.ReadyForCommands() ) {
					motion_future.finger_tip.set(HOME_X,HOME_Y,HOME_Z);  // HOME_* should match values in robot firmware.
					IK(motion_future);
					FinalizeMove();
					this.SendCommand("G92 X"+HOME_X+" Y"+HOME_Y+" Z"+HOME_Z);
					homing=false;
					homed=true;
					follow_mode=true;
				}
			}
		}
	}
	
	
	public void FinalizeMove() {
		// copy motion_future to motion_now
		motion_now.set(motion_future);
		
		if(arm_moved) {
			if(homed && follow_mode && this.ReadyForCommands() ) {
				arm_moved=false;
				this.DeleteAllQueuedCommands();
				this.SendCommand("G0 X"+motion_now.finger_tip.x+" Y"+motion_now.finger_tip.y+" Z"+motion_now.finger_tip.z);
			}
		}
	}
	
	
	public void render(GL2 gl2) {
		gl2.glPushMatrix();
/*
		gl2.glTranslatef(motion_now.base.x, motion_now.base.y, motion_now.base.z);
		gl2.glRotatef(motion_now.base_pan, motion_now.base_up.x,motion_now.base_up.y,motion_now.base_up.z);
 		// for debugging difference between FK and IK
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glColor3d(1,1,1);
		PrimitiveSolids.drawStar(gl2,motion_now.finger_tip,5);
		PrimitiveSolids.drawStar(gl2,motion_now.elbow,10);
		PrimitiveSolids.drawStar(gl2,motion_now.shoulder,15);
		gl2.glEnable(GL2.GL_LIGHTING);
*/
		//drawBounds(gl2);
		
		// these two should always match!
		drawFK(gl2);
		//drawIK(gl2);
		
		gl2.glPopMatrix();
	}
	
	
	protected void drawIK(GL2 gl2) {
		gl2.glPushMatrix();
		gl2.glTranslatef(motion_now.base.x, motion_now.base.y, motion_now.base.z);
		gl2.glRotatef(motion_now.base_pan, motion_now.base_up.x,motion_now.base_up.y,motion_now.base_up.z);
		
		// finger tip
		gl2.glBegin(GL2.GL_LINES);

		gl2.glVertex3f(0,0,0);
		gl2.glColor3f(1,0,0);
		gl2.glVertex3f(motion_now.shoulder.x,motion_now.shoulder.y,motion_now.shoulder.z);

		gl2.glVertex3f(motion_now.shoulder.x,motion_now.shoulder.y,motion_now.shoulder.z);
		gl2.glColor3f(1,0,1);
		gl2.glVertex3f(motion_now.elbow.x,motion_now.elbow.y,motion_now.elbow.z);

		gl2.glVertex3f(motion_now.elbow.x,motion_now.elbow.y,motion_now.elbow.z);
		gl2.glColor3f(1,1,0);
		gl2.glVertex3f(motion_now.wrist.x,motion_now.wrist.y,motion_now.wrist.z);

		gl2.glVertex3f(motion_now.wrist.x,motion_now.wrist.y,motion_now.wrist.z);
		gl2.glColor3f(1,0.8f,0.2f);
		gl2.glVertex3f(motion_now.finger_tip.x,motion_now.finger_tip.y,motion_now.finger_tip.z);
		
		gl2.glEnd();
		
		gl2.glPopMatrix();
	}
	
	
	protected void drawFK(GL2 gl2) {
		Vector3f a0 = new Vector3f(BASE_TO_SHOULDER_X,0,BASE_TO_SHOULDER_Z);
		Vector3f a1 = new Vector3f(0,0,SHOULDER_TO_ELBOW);
		Vector3f a2 = new Vector3f(0,0,ELBOW_TO_WRIST);
		Vector3f a3 = new Vector3f(0,0,WRIST_TO_FINGER);
		
		gl2.glPushMatrix();
		gl2.glTranslatef(motion_now.base.x, motion_now.base.y, motion_now.base.z);
		gl2.glRotatef(motion_now.base_pan, motion_now.base_up.x,motion_now.base_up.y,motion_now.base_up.z);
		
		// base to shoulder
		gl2.glColor3f(1,1,1);
		gl2.glRotatef(motion_now.angle_0,0,0,1);
		gl2.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(gl2,4,BASE_TO_SHOULDER_X*2,BASE_TO_SHOULDER_Z);
		gl2.glTranslatef(a0.x,a0.y,a0.z);

		// shoulder to elbow
		gl2.glRotatef(90+motion_now.angle_1,0,1,0);
		gl2.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(gl2,3.2f,3.2f);
		gl2.glColor3f(0,0,1);
		//PrimitiveSolids.drawBox(gl2,3,3,SHOULDER_TO_ELBOW);
		gl2.glPushMatrix();
		gl2.glTranslatef(a1.x/2,a1.y/2,a1.z/2);
		gl2.glRotatef(90,1,0,0);
		PrimitiveSolids.drawCylinder(gl2, SHOULDER_TO_ELBOW/2.0f, 3.0f*0.575f);
		gl2.glPopMatrix();

		// elbow to wrist
		gl2.glTranslatef(a1.x,a1.y,a1.z);
		gl2.glRotatef(180-motion_now.angle_2-motion_now.angle_1,0,1,0);
		gl2.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(gl2,2.2f,2.2f);
		gl2.glColor3f(0,0,1);
		//PrimitiveSolids.drawBox(gl2,2,2,ELBOW_TO_WRIST);
		gl2.glPushMatrix();
		gl2.glTranslatef(a2.x/2,a2.y/2,a2.z/2);
		gl2.glRotatef(90,1,0,0);
		PrimitiveSolids.drawCylinder(gl2, ELBOW_TO_WRIST/2.0f, 1.15f);
		gl2.glPopMatrix();

		// wrist to tool
		gl2.glTranslatef(a2.x,a2.y,a2.z);
		gl2.glRotatef(-180+motion_now.angle_2,0,1,0);
		gl2.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(gl2,1.2f,1.2f);
		gl2.glColor3f(0,0,1);
		//PrimitiveSolids.drawBox(gl2,1,1,WRIST_TO_FINGER);

		gl2.glPushMatrix();
		gl2.glTranslatef(a3.x/2,a3.y/3,a3.z/2);
		gl2.glRotatef(90,1,0,0);
		PrimitiveSolids.drawCylinder(gl2, WRIST_TO_FINGER/2.0f, 1.0f*0.575f);
		gl2.glPopMatrix();

		gl2.glTranslatef(a3.x,a3.y,a3.z);	
		// TODO draw tool here
		
		gl2.glPopMatrix();
	}

	
	protected void drawBounds(GL2 gl2) {
		// base
		
		gl2.glPushMatrix();
		gl2.glTranslatef(motion_now.base.x, motion_now.base.y, motion_now.base.z);
		gl2.glRotatef(motion_now.angle_0,0,0,1);
		gl2.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(gl2,4,BASE_TO_SHOULDER_X*2,BASE_TO_SHOULDER_Z);
		gl2.glPopMatrix();
		
		//gl2.glDisable(GL2.GL_LIGHTING);

		gl2.glColor3f(0,1,0);	PrimitiveSolids.drawCylinder(gl2,volumes[0]);  // shoulder
		gl2.glColor3f(0,0,1);	PrimitiveSolids.drawCylinder(gl2,volumes[1]);  // bicep
		gl2.glColor3f(0,1,0);	PrimitiveSolids.drawCylinder(gl2,volumes[2]);  // elbow
		gl2.glColor3f(0,0,1);	PrimitiveSolids.drawCylinder(gl2,volumes[3]);  // forearm
		gl2.glColor3f(0,1,0);	PrimitiveSolids.drawCylinder(gl2,volumes[4]);  // wrist
		gl2.glColor3f(0,0,1);	PrimitiveSolids.drawCylinder(gl2,volumes[5]);  // elbow

		//gl2.glEnable(GL2.GL_LIGHTING);
		
		// TODO draw tool here
		
	}
	
	
	@Override
	// override this method to check that the software is connected to the right type of robot.
	public boolean ConfirmPort(String preamble) {
		if(!portOpened) return false;
		
		if(preamble.contains("Arm3")) {
			portConfirmed=true;			
		}
		return portConfirmed;
	}
	

	public void MoveBase(Vector3f dp) {
		motion_future.base.set(dp);
	}
	
	public void RotateBase(float pan,float tilt) {
		motion_future.base_pan=pan;
		motion_future.base_tilt=tilt;
		
		motion_future.base_forward.y = (float)Math.sin(pan * Math.PI/180.0) * (float)Math.cos(tilt * Math.PI/180.0);
		motion_future.base_forward.x = (float)Math.cos(pan * Math.PI/180.0) * (float)Math.cos(tilt * Math.PI/180.0);
		motion_future.base_forward.z =                                        (float)Math.sin(tilt * Math.PI/180.0);
		motion_future.base_forward.normalize();
		
		motion_future.base_up.set(0,0,1);
	
		motion_future.base_right.cross(motion_future.base_forward, motion_future.base_up);
		motion_future.base_right.normalize();
		motion_future.base_up.cross(motion_future.base_right, motion_future.base_forward);
		motion_future.base_up.normalize();
	}
	
	
	public BoundingVolume [] GetBoundingVolumes() {
		// shoulder joint
		Vector3f t1=new Vector3f(motion_future.base_right);
		t1.scale(volumes[0].radius/2);
		t1.add(motion_future.shoulder);
		Vector3f t2=new Vector3f(motion_future.base_right);
		t2.scale(-volumes[0].radius/2);
		t2.add(motion_future.shoulder);
		volumes[0].SetP1(GetWorldCoordinatesFor(t1));
		volumes[0].SetP2(GetWorldCoordinatesFor(t2));
		// bicep
		volumes[1].SetP1(GetWorldCoordinatesFor(motion_future.shoulder));
		volumes[1].SetP2(GetWorldCoordinatesFor(motion_future.elbow));
		// elbow
		t1.set(motion_future.base_right);
		t1.scale(volumes[0].radius/2);
		t1.add(motion_future.elbow);
		t2.set(motion_future.base_right);
		t2.scale(-volumes[0].radius/2);
		t2.add(motion_future.elbow);
		volumes[2].SetP1(GetWorldCoordinatesFor(t1));
		volumes[2].SetP2(GetWorldCoordinatesFor(t2));
		// forearm
		volumes[3].SetP1(GetWorldCoordinatesFor(motion_future.elbow));
		volumes[3].SetP2(GetWorldCoordinatesFor(motion_future.wrist));
		// wrist
		t1.set(motion_future.base_right);
		t1.scale(volumes[0].radius/2);
		t1.add(motion_future.wrist);
		t2.set(motion_future.base_right);
		t2.scale(-volumes[0].radius/2);
		t2.add(motion_future.wrist);
		volumes[4].SetP1(GetWorldCoordinatesFor(t1));
		volumes[4].SetP2(GetWorldCoordinatesFor(t2));
		// finger
		volumes[5].SetP1(GetWorldCoordinatesFor(motion_future.wrist));
		volumes[5].SetP2(GetWorldCoordinatesFor(motion_future.finger_tip));
		
		return volumes;
	}
	
	
	Vector3f GetWorldCoordinatesFor(Vector3f in) {
		Vector3f out = new Vector3f(motion_future.base);
		
		Vector3f tempx = new Vector3f(motion_future.base_forward);
		tempx.scale(in.x);
		out.add(tempx);

		Vector3f tempy = new Vector3f(motion_future.base_right);
		tempy.scale(-in.y);
		out.add(tempy);

		Vector3f tempz = new Vector3f(motion_future.base_up);
		tempz.scale(in.z);
		out.add(tempz);
				
		return out;
	}
}
