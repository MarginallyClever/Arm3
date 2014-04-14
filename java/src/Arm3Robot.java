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
	
	// robot arm coordinates.  Relative to base unless otherwise noted.
	public Vector3f finger_tip = new Vector3f(HOME_X,HOME_Y,HOME_Z);
	private Vector3f wrist = new Vector3f();
	private Vector3f elbow = new Vector3f();
	private Vector3f shoulder = new Vector3f();
	public Vector3f base = new Vector3f();  // relative to world

	// angle of rotation
	float angle_0 = 0;
	float angle_1 = 0;
	float angle_2 = 0;

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

	public float getAngle0() {  return angle_0;  }
	public float getAngle1() {  return angle_1;  }
	public float getAngle2() {  return angle_2;  }
	
	public Vector3f getHome() {  return new Vector3f(HOME_X,HOME_Y,HOME_Z);  }
	public void setHome(Vector3f newhome) {
		HOME_X=newhome.x;
		HOME_Y=newhome.y;
		HOME_Z=newhome.z;
	}
	
	
	public boolean movePermitted(Vector3f newtip,Vector3f results) {
		// check floor
		if(newtip.z<0.25f) return false;
		// don't ceiling
		if(newtip.z>30.0f) return false;

		// check far limit
		Vector3f temp = new Vector3f(newtip);
		temp.sub(shoulder);
		if(temp.length() > 50) return false;
		// check near limit
		if(temp.length() < BASE_TO_SHOULDER_MINIMUM_LIMIT) return false;

		if(IK(newtip,results)==false) return false;

		if(CheckAngleLimits(results)==false) return false;

		// OK?
		return true;
	}
	
	
	public Arm3Robot(String name) {
		super(name);
		Vector3f results=new Vector3f();
		IK(finger_tip,results);
		angle_0=results.x;
		angle_1=results.y;
		angle_2=results.z;
	}
	
	
	protected boolean CheckAngleLimits(Vector3f angles) {
		if (angles.x < -180) return false;
		if (angles.x >  180) return false;
		if (angles.z <  -20) return false;
		if (angles.z >  180) return false;
		if (angles.y < -150) return false;
		if (angles.y >   80) return false;
		if (angles.y < -angles.z+ 10) return false;
		if (angles.y > -angles.z+170) return false;
		
		return true;
	}
	
	
	/**
	 * Convert cartesian XYZ to robot motor steps.
	 * @input cartesian coordinates relative to the base
	 * @input results where to put resulting angles after the IK calculation
	 * @return 0 if successful, 1 if the IK solution cannot be found.
	 */
	protected boolean IK(Vector3f cartesian, Vector3f results) {
		float a0,a1,a2;
	  // if we know the position of the wrist relative to the shoulder
	  // we can use intersection of circles to find the elbow.
	  // once we know the elbow position we can find the angle of each joint.
	  // each angle can be converted to motor steps.
	    
	    // use intersection of circles to find two possible elbow points.
	    // the two circles are the bicep (shoulder-elbow) and the forearm (elbow-wrist)
	    // the distance between circle centers is d  
	    Vector3f arm_plane = new Vector3f(cartesian.x,cartesian.y,0);
	    arm_plane.normalize();
	
	    // the finger (attachment point for the tool) is a short distance in "front" of the wrist joint
	    Vector3f finger = new Vector3f(cartesian);
	    Vector3f s = new Vector3f(arm_plane.x,arm_plane.y,arm_plane.z);
	    s.scale(WRIST_TO_FINGER);
	    wrist.set(finger);
	    wrist.sub(s);

	    shoulder.set(arm_plane);
	    shoulder.scale(BASE_TO_SHOULDER_X);
	    shoulder.z = BASE_TO_SHOULDER_Z;
	    
	    // use intersection of circles to find elbow
	    Vector3f es = new Vector3f(wrist);
	    es.sub(shoulder);
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
	    mid.add(shoulder);

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

		elbow.set(mid);
		elbow.sub(s);
		//Vector3f.add(mid, s, elbow);
		    
		  // find the angle between elbow-shoulder and the horizontal
		  Vector3f temp = new Vector3f();
		  temp.set(elbow);
		  temp.sub(shoulder);		  
		  temp.normalize();
		  float ax = temp.dot(arm_plane);
		  float ay = temp.z;
		  a1 = (float) Math.atan2(ay,ax);
		
		  // find the angle between elbow-wrist and the horizontal
		  temp.set(elbow);
		  temp.sub(wrist);
		  temp.normalize();
		  float bx = temp.dot(arm_plane);
		  float by = temp.z;
		  a2 = (float) Math.atan2(by,bx);
		  
		  // the easiest part
		  a0 = (float) Math.atan2(cartesian.y,cartesian.x);
		
		  // all angles are in radians, I want degrees
		  results.x=a0 * RAD2DEG;
		  results.y=a1 * -RAD2DEG;
		  results.z=a2 * RAD2DEG;
		  
		  return true;
	}
	
	
	protected void FK() {
		Vector3f arm_plane = new Vector3f();
		Vector3f temp = new Vector3f();
		
		arm_plane.set((float)Math.cos(angle_0/RAD2DEG),
					  (float)Math.sin(angle_0/RAD2DEG),
					  0);
		shoulder.set(arm_plane.x*BASE_TO_SHOULDER_X,
					 arm_plane.y*BASE_TO_SHOULDER_X,
					 BASE_TO_SHOULDER_Z);
		
		temp.set(arm_plane.x*(float)Math.cos(-angle_1/RAD2DEG)*SHOULDER_TO_ELBOW,
				 arm_plane.y*(float)Math.cos(-angle_1/RAD2DEG)*SHOULDER_TO_ELBOW,
				             (float)Math.sin(-angle_1/RAD2DEG)*SHOULDER_TO_ELBOW);
		elbow.set(temp);
		elbow.add(shoulder);
		
		temp.set(arm_plane.x*(float)Math.cos(angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				 arm_plane.y*(float)Math.cos(angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				             (float)Math.sin(angle_2/RAD2DEG)*-ELBOW_TO_WRIST);
		wrist.set(temp);
		wrist.add(elbow);
		
		temp.set(arm_plane.x*WRIST_TO_FINGER,
				 arm_plane.y*WRIST_TO_FINGER,
				 0);
		finger_tip.set(temp);
		finger_tip.add(wrist);
	}
	
	
	protected void update_fk(float delta) {
		boolean changed=false;
		final float vel=10.0f;
		Vector3f new_angles = new Vector3f(angle_0,angle_1,angle_2);
        
		if (rDown) {
			new_angles.y -= vel * delta;
			new_angles.z += vel * delta;
			changed=true;
		}
		if (fDown) {
			new_angles.y += vel * delta;
			new_angles.z -= vel * delta;
			changed=true;
		}
		if (tDown) {
			new_angles.z += vel * delta;
			changed=true;
		}
		if (gDown) {
			new_angles.z -= vel * delta;
			changed=true;
		}
		if (yDown) {
			new_angles.x += vel * delta;
			changed=true;
		}
		if (hDown) {
			new_angles.x -= vel * delta;
			changed=true;
		}

		if(changed==true) {
			if(CheckAngleLimits(new_angles)) {
				angle_0=new_angles.x;
				angle_1=new_angles.y;
				angle_2=new_angles.z;
				FK();
				arm_moved=true;
			}
		}
	}

	
	protected void update_ik(float delta) {
		boolean changed=false;
		Vector3f newtip = new Vector3f(finger_tip);
		final float vel=5.0f;

		if (uDown) {
			newtip.x -= vel * delta;
			changed=true;
		}
		if (jDown) {
			newtip.x += vel * delta;
			changed=true;
		}
		
		if (iDown) {
			newtip.y += vel * delta;
			changed=true;
		}
		if (kDown) {
			newtip.y -= vel * delta;
			changed=true;
		}
		
		if (oDown) {
			newtip.z += vel * delta;
			changed=true;
		}
		if (lDown) {
			newtip.z -= vel * delta;
			changed=true;
		}

		Vector3f results = new Vector3f();
		if(changed==true && movePermitted(newtip,results)) {
			if(finger_tip.epsilonEquals(newtip,0.1f)) arm_moved=true;
			angle_0=results.x;
			angle_1=results.y;
			angle_2=results.z;
			finger_tip.set(newtip);
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
	
	
	public void update(float delta) {
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
					Vector3f newtip = new Vector3f(HOME_X,HOME_Y,HOME_Z);
					Vector3f results = new Vector3f();
					IK(newtip,results);
					angle_0=results.x;
					angle_1=results.y;
					angle_2=results.z;
					finger_tip.set(newtip);  // Should match config in robot.
					this.SendCommand("G92 X"+HOME_X+" Y"+HOME_Y+" Z"+HOME_Z);
					homing=false;
					homed=true;
					follow_mode=true;
				}
			}
			if(homed && follow_mode && arm_moved==true) {
				arm_moved=false;
				this.DeleteAllQueuedCommands();
				this.SendCommand("G0 X"+finger_tip.x+" Y"+finger_tip.y+" Z"+finger_tip.z);
			}
		}
	}
	
	
	public void render(GL2 gl2,float delta) {
		update(delta);
		
		// these two should always match!
		gl2.glPushMatrix();
		gl2.glTranslatef(base.x, base.y, base.z);
/*
 		// for debugging difference between FK and IK
		gl2.glDisable(GL2.GL_LIGHTING);
		gl2.glColor3d(1,1,1);
		PrimitiveSolids.drawStar(gl2,finger_tip,5);
		PrimitiveSolids.drawStar(gl2,elbow,10);
		PrimitiveSolids.drawStar(gl2,shoulder,15);
		gl2.glEnable(GL2.GL_LIGHTING);
*/
		drawFK(gl2);
		//drawIK(gl2);
		gl2.glPopMatrix();
	}
	
	
	protected void drawIK(GL2 gl2) {
		// finger tip
		gl2.glBegin(GL2.GL_LINES);

		gl2.glVertex3f(0,0,0);
		gl2.glColor3f(1,0,0);
		gl2.glVertex3f(shoulder.x,shoulder.y,shoulder.z);

		gl2.glVertex3f(shoulder.x,shoulder.y,shoulder.z);
		gl2.glColor3f(1,0,1);
		gl2.glVertex3f(elbow.x,elbow.y,elbow.z);

		gl2.glVertex3f(elbow.x,elbow.y,elbow.z);
		gl2.glColor3f(1,1,0);
		gl2.glVertex3f(wrist.x,wrist.y,wrist.z);

		gl2.glVertex3f(wrist.x,wrist.y,wrist.z);
		gl2.glColor3f(1,0.8f,0.2f);
		gl2.glVertex3f(finger_tip.x,finger_tip.y,finger_tip.z);
		
		gl2.glEnd();
	}
	
	
	protected void drawFK(GL2 gl2) {
		Vector3f a0 = new Vector3f(BASE_TO_SHOULDER_X,0,BASE_TO_SHOULDER_Z);
		Vector3f a1 = new Vector3f(0,0,SHOULDER_TO_ELBOW);
		Vector3f a2 = new Vector3f(0,0,ELBOW_TO_WRIST);
		Vector3f a3 = new Vector3f(0,0,WRIST_TO_FINGER);
		
		gl2.glPushMatrix();
		
		// base to shoulder
		gl2.glColor3f(1,1,1);
		gl2.glRotatef(angle_0,0,0,1);
		gl2.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(gl2,4,BASE_TO_SHOULDER_X*2,BASE_TO_SHOULDER_Z);
		gl2.glTranslatef(a0.x,a0.y,a0.z);

		// shoulder to elbow
		gl2.glRotatef(90+angle_1,0,1,0);
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
		gl2.glRotatef(180-angle_2-angle_1,0,1,0);
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
		gl2.glRotatef(-180+angle_2,0,1,0);
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
		
		gl2.glPopMatrix();
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
}
