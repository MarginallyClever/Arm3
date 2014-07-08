package Arm3;

import javax.vecmath.Vector3f;
import javax.media.opengl.GL2;

import com.jogamp.newt.event.KeyEvent;


public class Arm6Robot 
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
	float HOME_A = 0;
	float HOME_B = 0;
	float HOME_C = 0;
	
	float HOME_RIGHT_X = 0;
	float HOME_RIGHT_Y = 0;
	float HOME_RIGHT_Z = -1;

	float HOME_FORWARD_X = 1;
	float HOME_FORWARD_Y = 0;
	float HOME_FORWARD_Z = 0;

	boolean HOME_AUTOMATICALLY_ON_STARTUP = true;
	Cylinder [] volumes = new Cylinder[6];
	
	class MotionState {
		// angle of rotation
		float angle_0 = 0;
		float angle_1 = 0;
		float angle_2 = 0;
		float angle_3 = 0;
		float angle_4 = 0;
		float angle_5 = 0;

		// robot arm coordinates.  Relative to base unless otherwise noted.
		public Vector3f finger_tip = new Vector3f(HOME_X,HOME_Y,HOME_Z);
		public Vector3f finger_forward = new Vector3f(HOME_FORWARD_X,HOME_FORWARD_Y,HOME_FORWARD_Z);
		public Vector3f finger_right = new Vector3f(HOME_RIGHT_X,HOME_RIGHT_Y,HOME_RIGHT_Z);
		public float iku=0;
		public float ikv=0;
		public float ikw=0;
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
			angle_3 = other.angle_3;
			angle_4 = other.angle_4;
			angle_5 = other.angle_5;
			iku=other.iku;
			ikv=other.ikv;
			ikw=other.ikw;
			finger_forward.set(other.finger_forward);
			finger_right.set(other.finger_right);
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
	
	boolean pDown=false;
	boolean pWasOn=false;
	boolean moveMode=false;
	
	boolean has4axis=true;
	boolean has5axis=true;
	boolean has6axis=true;
	
	
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
		if(state.finger_tip.z<0.25f) {
			return false;
		}
		// don't ceiling
		if(state.finger_tip.z>50.0f) {
			return false;
		}

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
	
	
	public Arm6Robot(String name) {
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
	
	
	protected boolean CheckAngleLimits(MotionState state) {/*
		// machine specific limits
		if (state.angle_0 < -180) return false;
		if (state.angle_0 >  180) return false;
		if (state.angle_2 <  -20) return false;
		if (state.angle_2 >  180) return false;
		if (state.angle_1 < -150) return false;
		if (state.angle_1 >   80) return false;
		if (state.angle_1 < -state.angle_2+ 10) return false;
		if (state.angle_1 > -state.angle_2+170) return false;

		if (state.angle_3 < -180) return false;
		if (state.angle_3 >  180) return false;
		if (state.angle_4 < -180) return false;
		if (state.angle_4 >  180) return false;
		if (state.angle_5 < -180) return false;
		if (state.angle_5 >  180) return false;*/
		
		return true;
	}
	
	
	/**
	 * Convert cartesian XYZ to robot motor steps.
	 * @input cartesian coordinates relative to the base
	 * @input results where to put resulting angles after the IK calculation
	 * @return 0 if successful, 1 if the IK solution cannot be found.
	 */
	protected boolean IK(MotionState state) {
		float a0,a1,a2,a3,a4,a5;
		// if we know the position of the wrist relative to the shoulder
		// we can use intersection of circles to find the elbow.
		// once we know the elbow position we can find the angle of each joint.
		// each angle can be converted to motor steps.

	    // the finger (attachment point for the tool) is a short distance in "front" of the wrist joint
	    Vector3f finger = new Vector3f(state.finger_tip);
		state.wrist.set(state.finger_forward);
		state.wrist.scale(-WRIST_TO_FINGER);
		state.wrist.add(finger);
				
	    // use intersection of circles to find two possible elbow points.
	    // the two circles are the bicep (shoulder-elbow) and the forearm (elbow-wrist)
	    // the distance between circle centers is d  
	    Vector3f arm_plane = new Vector3f(state.wrist.x,state.wrist.y,0);
	    arm_plane.normalize();
	
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
		r.scale(h);

		state.elbow.set(mid);
		state.elbow.sub(r);
		//Vector3f.add(mid, s, elbow);

		
		// find the angle between elbow-shoulder and the horizontal
		Vector3f bicep_forward = new Vector3f(state.elbow);
		bicep_forward.sub(state.shoulder);		  
		bicep_forward.normalize();
		float ax = bicep_forward.dot(arm_plane);
		float ay = bicep_forward.z;
		a1 = (float) -Math.atan2(ay,ax);

		// find the angle between elbow-wrist and the horizontal
		Vector3f forearm_forward = new Vector3f(state.elbow);
		forearm_forward.sub(state.wrist);
		forearm_forward.normalize();
		float bx = forearm_forward.dot(arm_plane);
		float by = forearm_forward.z;
		a2 = (float) Math.atan2(by,bx);

		// find the angle of the base
		a0 = (float) Math.atan2(state.wrist.y,state.wrist.x);

		
		// find the angle of the ulna rotation
		//if(Math.abs(forearm_forward.dot(state.finger_forward))<0.99999) {
			Vector3f arm_plane_normal = new Vector3f();
			Vector3f up = new Vector3f(0,0,1);
			arm_plane_normal.cross(arm_plane,up);
			float cx = state.finger_forward.z;
			float cy = state.finger_forward.dot(arm_plane_normal);
			a4 = 0;//(float) Math.atan2(cy,cx);
		//	float sign_check = state.angle_4 / RAD2DEG;
		//} else {
		//	a4 = state.angle_4 / RAD2DEG; 
		//}
		
		
		// find the angle of the wrist bend
		//if(Math.abs(forearm_forward.dot(state.finger_forward))<0.99999) {
			Vector3f ffn = new Vector3f(forearm_forward);
			float s = forearm_forward.dot(motion_now.finger_forward);
			ffn.scale(s);
			Vector3f temp = new Vector3f(motion_now.finger_forward);
			temp.sub(ffn);
			temp.normalize();
			
			float dx = -state.finger_forward.dot(forearm_forward);
			float dy = state.finger_forward.dot(temp);
			a3=(float) Math.atan2(dy,dx);
		//} else {
		//	a3 = state.angle_3 / RAD2DEG;
		//}
		
		// find the angle of the finger rotation
		a5=0;
		
		// all angles are in radians, I want degrees
		state.angle_0=a0 * RAD2DEG;
		state.angle_1=a1 * RAD2DEG;
		state.angle_2=a2 * RAD2DEG;
		state.angle_3=a3 * RAD2DEG;
		state.angle_4=a4 * RAD2DEG;
		state.angle_5=a5 * RAD2DEG;

		return true;
	}

	
	protected void update_ik(float delta) {
		boolean changed=false;
		motion_future.finger_tip.set(motion_now.finger_tip);
		final float vel=5.0f;
		float dp = vel * delta;

		if (rDown) {
			motion_future.finger_tip.x -= dp;
			changed=true;
		}
		if (fDown) {
			motion_future.finger_tip.x += dp;
			changed=true;
		}
		
		if (tDown) {
			motion_future.finger_tip.y += dp;
			changed=true;
		}
		if (gDown) {
			motion_future.finger_tip.y -= dp;
			changed=true;
		}
		
		if (yDown) {
			motion_future.finger_tip.z += dp;
			changed=true;
		}
		if (hDown) {
			motion_future.finger_tip.z -= dp;
			changed=true;
		}

		// rotations
		float ru=0,rv=0,rw=0;
		if(uDown) rw= 0.1f;
		if(jDown) rw=-0.1f;
		if(iDown) rv=0.1f;
		if(kDown) rv=-0.1f;
		if(oDown) ru=0.1f;
		if(lDown) ru=-0.1f;

		if(rw!=0 || rv!=0 || ru!=0 ) {
			// On a 3-axis robot when homed the forward axis of the finger tip is pointing downward.
			// More complex arms start from the same assumption.
			Vector3f forward = new Vector3f(HOME_FORWARD_X,HOME_FORWARD_Y,HOME_FORWARD_Z);
			Vector3f right = new Vector3f(HOME_RIGHT_X,HOME_RIGHT_Y,HOME_RIGHT_Z);
			Vector3f up = new Vector3f();
			
			up.cross(forward,right);
			
			Vector3f of = new Vector3f(forward);
			Vector3f or = new Vector3f(right);
			Vector3f ou = new Vector3f(up);
			
			motion_future.iku+=ru*dp;
			motion_future.ikv+=rv*dp;
			motion_future.ikw+=rw*dp;
			
			Vector3f result;

			result = RotateAroundAxis(forward,of,motion_future.iku);
			result = RotateAroundAxis(result,or,motion_future.ikv);
			result = RotateAroundAxis(result,ou,motion_future.ikw);
			motion_future.finger_forward.set(result);

			result = RotateAroundAxis(right,of,motion_future.iku);
			result = RotateAroundAxis(result,or,motion_future.ikv);
			result = RotateAroundAxis(result,ou,motion_future.ikw);
			motion_future.finger_right.set(result);
			
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
		
	/**
	 * Rotate the point xyz around the line passing through abc with direction uvw
	 * http://inside.mines.edu/~gmurray/ArbitraryAxisRotation/ArbitraryAxisRotation.html
	 * Special case where abc=0
	 * @param vec
	 * @param axis
	 * @param angle
	 * @return
	 */
	protected Vector3f RotateAroundAxis(Vector3f vec,Vector3f axis,float angle) {
		float C = (float)Math.cos(angle);
		float S = (float)Math.sin(angle);
		float x = vec.x;
		float y = vec.y;
		float z = vec.z;
		float u = axis.x;
		float v = axis.y;
		float w = axis.z;
		
		// (a*( v*v + w*w) - u*(b*v + c*w - u*x - v*y - w*z))(1.0-C)+x*C+(-c*v + b*w - w*y + v*z)*S
		// (b*( u*u + w*w) - v*(a*v + c*w - u*x - v*y - w*z))(1.0-C)+y*C+( c*u - a*w + w*x - u*z)*S
		// (c*( u*u + v*v) - w*(a*v + b*v - u*x - v*y - w*z))(1.0-C)+z*C+(-b*u + a*v - v*x + u*y)*S
		// but a=b=c=0 so
		// x' = ( -u*(- u*x - v*y - w*z)) * (1.0-C) + x*C + ( - w*y + v*z)*S
		// y' = ( -v*(- u*x - v*y - w*z)) * (1.0-C) + y*C + ( + w*x - u*z)*S
		// z' = ( -w*(- u*x - v*y - w*z)) * (1.0-C) + z*C + ( - v*x + u*y)*S
		
		float a = (-u*x - v*y - w*z);

		return new Vector3f( (-u*a) * (1.0f-C) + x*C + ( -w*y + v*z)*S,
							 (-v*a) * (1.0f-C) + y*C + (  w*x - u*z)*S,
							 (-w*a) * (1.0f-C) + z*C + ( -v*x + u*y)*S);
	}
	
	
	protected void FK(MotionState state) {
		Vector3f arm_plane = new Vector3f((float)Math.cos(state.angle_0/RAD2DEG),
					  					  (float)Math.sin(state.angle_0/RAD2DEG),
					  					  0);
		state.shoulder.set(arm_plane.x*BASE_TO_SHOULDER_X,
						   arm_plane.y*BASE_TO_SHOULDER_X,
						               BASE_TO_SHOULDER_Z);
		
		state.elbow.set(arm_plane.x*(float)Math.cos(-state.angle_1/RAD2DEG)*SHOULDER_TO_ELBOW,
						arm_plane.y*(float)Math.cos(-state.angle_1/RAD2DEG)*SHOULDER_TO_ELBOW,
									(float)Math.sin(-state.angle_1/RAD2DEG)*SHOULDER_TO_ELBOW);
		state.elbow.add(state.shoulder);

		state.wrist.set(arm_plane.x*(float)Math.cos(state.angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				 		arm_plane.y*(float)Math.cos(state.angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				 					(float)Math.sin(state.angle_2/RAD2DEG)*-ELBOW_TO_WRIST);
		state.wrist.add(state.elbow);
		
		// build the axies around which we will rotate the tip
		Vector3f fn = new Vector3f();
		Vector3f up = new Vector3f(0,0,1);
		fn.cross(arm_plane,up);
		Vector3f axis = new Vector3f(state.wrist);
		axis.sub(state.elbow);
		axis.normalize();
		fn = RotateAroundAxis(fn, axis, -state.angle_3/RAD2DEG);
		up = RotateAroundAxis(up, axis, -state.angle_3/RAD2DEG);

		state.finger_tip.set(arm_plane);
		state.finger_tip = RotateAroundAxis(state.finger_tip, axis,-state.angle_3/RAD2DEG); 
		state.finger_tip = RotateAroundAxis(state.finger_tip, fn,-state.angle_4/RAD2DEG);
		state.finger_tip.scale(WRIST_TO_FINGER);
		state.finger_tip.add(state.wrist);

		state.finger_forward.set(state.finger_tip);
		state.finger_forward.sub(state.wrist);
		state.finger_forward.normalize();
		
		state.finger_right.set(up); 
		state.finger_right.scale(-1);
		//state.finger_right = RotateAroundAxis(state.finger_right, axis,-state.angle_3/RAD2DEG);
		state.finger_right = RotateAroundAxis(state.finger_right, fn,-state.angle_4/RAD2DEG); 
		state.finger_right = RotateAroundAxis(state.finger_right, state.finger_forward,state.angle_5/RAD2DEG);
	}
	
	
	protected void update_fk(float delta) {
		boolean changed=false;
		final float vel=10.0f;
		motion_future.angle_0 = motion_now.angle_0;
		motion_future.angle_1 = motion_now.angle_1;
		motion_future.angle_2 = motion_now.angle_2;

		if (rDown) {
			motion_future.angle_1 -= vel * delta;
			//motion_future.angle_2 += vel * delta;
			changed=true;
		}
		if (fDown) {
			motion_future.angle_1 += vel * delta;
			//motion_future.angle_2 -= vel * delta;
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
		if(has6axis && uDown) {
			motion_future.angle_3 += vel * delta;
			changed=true;
		}
		if(has6axis && jDown) {
			motion_future.angle_3 -= vel * delta;
			changed=true;
		}
		if(has5axis && iDown) {
			motion_future.angle_4 += vel * delta;
			changed=true;
		}
		if(has5axis && kDown) {
			motion_future.angle_4 -= vel * delta;
			changed=true;
		}
		
		if(has4axis && oDown) {
			motion_future.angle_5 += vel * delta;
			changed=true;
		}
		if(has4axis && lDown) {
			motion_future.angle_5 -= vel * delta;
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
		case KeyEvent.VK_P: pDown=state;  break;
		}
	}

	
	public void keyPressed(KeyEvent e) {
		keyAction(e,true);
   	}
	
	
	public void keyReleased(KeyEvent e) {
		keyAction(e,false);
	}
	
	
	public void PrepareMove(float delta) {
		if(pDown) pWasOn=true;
		if(!pDown && pWasOn) {
			pWasOn=false;
			moveMode=!moveMode;
		}
		if(moveMode) update_ik(delta);
		else		 update_fk(delta);
		
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
		drawIK(gl2);
		
		gl2.glPopMatrix();
	}
	
	
	protected void drawIK(GL2 gl2) {
		gl2.glPushMatrix();
		gl2.glTranslatef(motion_now.base.x, motion_now.base.y, motion_now.base.z);
		gl2.glRotatef(motion_now.base_pan, motion_now.base_up.x,motion_now.base_up.y,motion_now.base_up.z);
		
		gl2.glDisable(GL2.GL_DEPTH_TEST);
		gl2.glDisable(GL2.GL_LIGHTING);
		
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

		gl2.glColor3f(0,1,1);
		gl2.glVertex3f(motion_now.finger_tip.x,motion_now.finger_tip.y,motion_now.finger_tip.z);
		gl2.glVertex3f(motion_now.finger_tip.x+motion_now.finger_right.x,
					   motion_now.finger_tip.y+motion_now.finger_right.y,
					   motion_now.finger_tip.z+motion_now.finger_right.z);
		
		gl2.glColor3f(1,1,0);
		gl2.glVertex3f(motion_now.finger_tip.x,motion_now.finger_tip.y,motion_now.finger_tip.z);
		gl2.glVertex3f(motion_now.finger_tip.x+motion_now.finger_forward.x,
					   motion_now.finger_tip.y+motion_now.finger_forward.y,
					   motion_now.finger_tip.z+motion_now.finger_forward.z);
		/*
		// DEBUG START
		Vector3f forearm_forward = new Vector3f(motion_now.elbow);
		forearm_forward.sub(motion_now.wrist);
		forearm_forward.normalize();
		
		Vector3f ffn = new Vector3f(forearm_forward);
		float s = forearm_forward.dot(motion_now.finger_forward);
		ffn.scale(s);
		Vector3f temp = new Vector3f(motion_now.finger_forward);
		temp.sub(ffn);
		temp.normalize();

		gl2.glColor3f(1,0,0);
		gl2.glVertex3f(motion_now.wrist.x,motion_now.wrist.y,motion_now.wrist.z);
		gl2.glVertex3f(motion_now.wrist.x+forearm_forward.x,
						motion_now.wrist.y+forearm_forward.y,
						motion_now.wrist.z+forearm_forward.z);
		gl2.glColor3f(0,0,1);
		gl2.glVertex3f(motion_now.wrist.x,motion_now.wrist.y,motion_now.wrist.z);
		gl2.glVertex3f(motion_now.wrist.x+ffn.x,
						motion_now.wrist.y+ffn.y,
						motion_now.wrist.z+ffn.z);
		gl2.glColor3f(0,1,1);
		gl2.glVertex3f(motion_now.wrist.x,motion_now.wrist.y,motion_now.wrist.z);
		gl2.glVertex3f(motion_now.wrist.x+temp.x,
						motion_now.wrist.y+temp.y,
						motion_now.wrist.z+temp.z);
		// DEBUG END
		*/
		// DEBUG 2 START
		Vector3f arm_plane = new Vector3f((float)Math.cos(motion_now.angle_0/RAD2DEG),
										  (float)Math.sin(motion_now.angle_0/RAD2DEG),
										  0);
		Vector3f fn = new Vector3f();
		Vector3f up = new Vector3f(0,0,1);
		fn.cross(arm_plane,up);
		Vector3f axis = new Vector3f(motion_now.wrist);
		axis.sub(motion_now.elbow);
		axis.normalize();
		fn = RotateAroundAxis(fn, axis, -motion_now.angle_3/RAD2DEG);
		up = RotateAroundAxis(up, axis, -motion_now.angle_3/RAD2DEG);
		// DEBUG 2 END

		gl2.glColor3f(0,0,1);
		gl2.glVertex3f(motion_now.wrist.x,motion_now.wrist.y,motion_now.wrist.z);
		gl2.glVertex3f(motion_now.wrist.x+fn.x,
						motion_now.wrist.y+fn.y,
						motion_now.wrist.z+fn.z);
		gl2.glColor3f(1,0,0);
		gl2.glVertex3f(motion_now.wrist.x,motion_now.wrist.y,motion_now.wrist.z);
		gl2.glVertex3f(motion_now.wrist.x+up.x,
						motion_now.wrist.y+up.y,
						motion_now.wrist.z+up.z);
		
		gl2.glEnd();


		gl2.glEnable(GL2.GL_DEPTH_TEST);
		gl2.glEnable(GL2.GL_LIGHTING);
		
		gl2.glPopMatrix();
	}
	
	
	protected void drawFK(GL2 gl2) {
		Vector3f a0 = new Vector3f(BASE_TO_SHOULDER_X,0,BASE_TO_SHOULDER_Z);
		Vector3f a1 = new Vector3f(0,0,SHOULDER_TO_ELBOW);
		Vector3f a2 = new Vector3f(0,0,ELBOW_TO_WRIST);
		Vector3f a3 = new Vector3f(0,0,WRIST_TO_FINGER);

		boolean draw_simple=true;
		if(draw_simple) {
			gl2.glDisable(GL2.GL_LIGHTING);

			// base 
			gl2.glPushMatrix();
			gl2.glTranslatef(motion_now.base.x, motion_now.base.y, motion_now.base.z);
			gl2.glRotatef(motion_now.base_pan, motion_now.base_up.x,motion_now.base_up.y,motion_now.base_up.z);
			
			gl2.glColor3f(1,1,1);
			gl2.glRotatef(motion_now.angle_0,0,0,1);

			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a0.x,a0.y,a0.z);
			gl2.glEnd();

			// shoulder
			gl2.glTranslatef(a0.x,a0.y,a0.z);
			gl2.glRotatef(90+motion_now.angle_1,0,1,0);
			
			// bicep
			gl2.glColor3f(0,0,1);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a1.x,a1.y,a1.z);
			gl2.glEnd();
	
			// elbow
			gl2.glTranslatef(a1.x,a1.y,a1.z);
			gl2.glRotatef(180-motion_now.angle_2-motion_now.angle_1,0,1,0);
	
			// forearm
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a2.x,a2.y,a2.z);
			gl2.glEnd();
	
			// wrist
			gl2.glTranslatef(a2.x,a2.y,a2.z);
			gl2.glRotatef(-motion_now.angle_3,a2.x,a2.y,a2.z);
			gl2.glRotatef( motion_now.angle_4,0,1,0);
			gl2.glRotatef(-180+motion_now.angle_2,0,1,0);
			
			// finger tip
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(a3.x,a3.y,a3.z);
			gl2.glEnd();
			
			gl2.glTranslatef(a3.x,a3.y,a3.z);	

			// draw finger tip orientation
			gl2.glRotatef(motion_now.angle_5,a3.x,a3.y,a3.z);
			gl2.glRotatef(-90,0,1,0);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glColor3f(1,0,1);  // magenta
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(HOME_RIGHT_X,HOME_RIGHT_Y,HOME_RIGHT_Z);
			gl2.glColor3f(0,1,0);  // green
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(HOME_FORWARD_X,HOME_FORWARD_Y,HOME_FORWARD_Z);
			gl2.glEnd();
			
			// TODO draw tool here
			
			gl2.glPopMatrix();

			gl2.glEnable(GL2.GL_LIGHTING);
		} else {
			// base 
			gl2.glPushMatrix();
			gl2.glTranslatef(motion_now.base.x, motion_now.base.y, motion_now.base.z);
			gl2.glRotatef(motion_now.base_pan, motion_now.base_up.x,motion_now.base_up.y,motion_now.base_up.z);
			
			gl2.glColor3f(1,1,1);
			gl2.glRotatef(motion_now.angle_0,0,0,1);
			gl2.glColor3f(0,0,1);
			PrimitiveSolids.drawBox(gl2,4,BASE_TO_SHOULDER_X*2,BASE_TO_SHOULDER_Z);
	
			// shoulder
			gl2.glTranslatef(a0.x,a0.y,a0.z);
			gl2.glRotatef(90+motion_now.angle_1,0,1,0);
			gl2.glColor3f(0,1,0);
			PrimitiveSolids.drawCylinder(gl2,3.2f,3.2f);
			
			// bicep
			gl2.glColor3f(0,0,1);
			//PrimitiveSolids.drawBox(gl2,3,3,SHOULDER_TO_ELBOW);
			gl2.glPushMatrix();
			gl2.glTranslatef(a1.x/2,a1.y/2,a1.z/2);
			gl2.glRotatef(90,1,0,0);
			PrimitiveSolids.drawCylinder(gl2, SHOULDER_TO_ELBOW/2.0f, 3.0f*0.575f);
			gl2.glPopMatrix();
	
			// elbow
			gl2.glTranslatef(a1.x,a1.y,a1.z);
			gl2.glRotatef(180-motion_now.angle_2-motion_now.angle_1,0,1,0);
			gl2.glColor3f(0,1,0);
			PrimitiveSolids.drawCylinder(gl2,2.2f,2.2f);
			gl2.glColor3f(0,0,1);
			//PrimitiveSolids.drawBox(gl2,2,2,ELBOW_TO_WRIST);
	
			// forearm
			gl2.glPushMatrix();
			gl2.glTranslatef(a2.x/2,a2.y/2,a2.z/2);
			gl2.glRotatef(90,1,0,0);
			PrimitiveSolids.drawCylinder(gl2, ELBOW_TO_WRIST/2.0f, 1.15f);
			gl2.glPopMatrix();
	
			// wrist
			gl2.glTranslatef(a2.x,a2.y,a2.z);
			gl2.glRotatef(-motion_now.angle_3,a2.x,a2.y,a2.z);
			gl2.glRotatef(motion_now.angle_4,0,1,0);
			gl2.glRotatef(-180+motion_now.angle_2,0,1,0);
			gl2.glColor3f(0,1,0);
			PrimitiveSolids.drawCylinder(gl2,1.2f,1.2f);
			gl2.glColor3f(0,0,1);
			//PrimitiveSolids.drawBox(gl2,1,1,WRIST_TO_FINGER);
	
			// finger tip
			gl2.glPushMatrix();
			gl2.glTranslatef(a3.x/2,a3.y/3,a3.z/2);
			gl2.glRotatef(90,1,0,0);
			PrimitiveSolids.drawCylinder(gl2, WRIST_TO_FINGER/2.0f, 1.0f*0.575f);
			gl2.glPopMatrix();
	
			gl2.glTranslatef(a3.x,a3.y,a3.z);	
			gl2.glRotatef(motion_now.angle_5,a3.x,a3.y,a3.z);
			//gl2.glRotatef(-motion_now.angle_3,a2.x,a2.y,a2.z);
			//gl2.glRotatef(motion_now.angle_4,0,1,0);
			//gl2.glRotatef(-180+motion_now.angle_2,0,1,0);
			gl2.glRotatef(-90,0,1,0);
			// draw finger tip orientation
			gl2.glDisable(GL2.GL_LIGHTING);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glColor3f(1,0,1);  // magenta
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(HOME_RIGHT_X,HOME_RIGHT_Y,HOME_RIGHT_Z);
			gl2.glColor3f(0,1,0);  // green
			gl2.glVertex3f(0,0,0);
			gl2.glVertex3f(HOME_FORWARD_X,HOME_FORWARD_Y,HOME_FORWARD_Z);
			gl2.glEnd();
			gl2.glEnable(GL2.GL_LIGHTING);
			// TODO draw tool here
			gl2.glPopMatrix();
		}
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
