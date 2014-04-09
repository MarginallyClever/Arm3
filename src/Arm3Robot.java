import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;


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
	final float BASE_TO_SHOULDER_MINIMUM_LIMIT = 11.0f;
	
	final float HOME_X = (25.0f+WRIST_TO_FINGER);
	final float HOME_Y = 0;
	final float HOME_Z = -1;
	
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
	

	public float getAngle0() {  return angle_0;  }
	public float getAngle1() {  return angle_1;  }
	public float getAngle2() {  return angle_2;  }
	
	
	public Arm3Robot() {
		IK(finger_tip.x,finger_tip.y,finger_tip.z);
	}
	

	protected void CheckAngleLimits() {
		if (angle_0 < -180) angle_0 = -180;
		if (angle_0 >  180) angle_0 =  180;
		if (angle_2 <  -20) angle_2 = -20;
		if (angle_2 >  180) angle_2 = 180;
		if (angle_1 < -135) angle_1 = -135;
		if (angle_1 >   80) angle_1 = 80;
		if (angle_1 < -angle_2+ 10) angle_1 = -angle_2+ 10;
		if (angle_1 > -angle_2+170) angle_1 = -angle_2+170;
	}
	
	
	/**
	 * Convert cartesian XYZ to robot motor steps.
	 * @return 0 if successful, 1 if the IK solution cannot be found.
	 */
	protected int IK(float x, float y,float z) {
	  // if we know the position of the wrist relative to the shoulder
	  // we can use intersection of circles to find the elbow.
	  // once we know the elbow position we can find the angle of each joint.
	  // each angle can be converted to motor steps.
	    
	    // use intersection of circles to find two possible elbow points.
	    // the two circles are the bicep (shoulder-elbow) and the forearm (elbow-wrist)
	    // the distance between circle centers is d  
	    Vector3f arm_plane = new Vector3f(x,y,0);
	    arm_plane.normalise();
	
	    // the finger (attachment point for the tool) is a short distance in "front" of the wrist joint
	    Vector3f finger = new Vector3f(x,y,z);
	    Vector3f s = new Vector3f(arm_plane.x,arm_plane.y,arm_plane.z);
	    s.scale(WRIST_TO_FINGER);
	    Vector3f.sub(finger, s, wrist);
	
	    shoulder.set(arm_plane.x,arm_plane.y,arm_plane.z);
	    shoulder.scale(BASE_TO_SHOULDER_X);
	    shoulder.z = BASE_TO_SHOULDER_Z;
	    
	    Vector3f es = new Vector3f();
	    Vector3f.sub( wrist, shoulder, es);
	    
	    // use intersection of circles to find elbow
	    float d = es.length();
	    float r1=ELBOW_TO_WRIST;  // circle 1 centers on wrist
	    float r0=SHOULDER_TO_ELBOW;  // circle 0 centers on shoulder
	    if( d > ELBOW_TO_WRIST + SHOULDER_TO_ELBOW ) {
	      // The points are impossibly far apart, no solution can be found.
	      return 1;
	    }
	    
	    float a = ( r0 * r0 - r1 * r1 + d*d ) / ( 2.0f*d );
	    // find the midpoint
	    Vector3f mid=new Vector3f();
	    s.set(es.x,es.y,es.z);
	    s.scale(a/d);
	    Vector3f.add(s, shoulder, mid);
	    // with a and r0 we can find h, the distance from midpoint to the intersections.
	    float h=(float)Math.sqrt(r0*r0-a*a);
	    // the distance h on a line orthogonal to n and plane_normal gives us the two intersections.
		Vector3f n = new Vector3f(-arm_plane.y,arm_plane.x,0);
		n.normalise();
		Vector3f r = new Vector3f();
		Vector3f.cross(n,es,r);
		r.normalise();
		s.set(r.x*h,r.y*h,r.z*h);
		Vector3f.sub(mid, s, elbow);
		//Vector3f.add(mid, s, elbow);
		    
		  // find the elbow-shoulder relative to the horizontal
		  Vector3f temp = new Vector3f();
		  Vector3f apn = new Vector3f();
		  Vector3f.sub(elbow, shoulder, temp);
		  temp.normalise();
		  apn.set(arm_plane.x,arm_plane.y,arm_plane.z);
		  apn.normalise();
		  float ax = Vector3f.dot(temp, apn);
		  float ay = temp.z;
		  angle_1 = (float) Math.atan2(ay,ax);
		
		  // find the angle elbow-wrist relative to the horizontal
		  Vector3f.sub(elbow, wrist,temp);
		  temp.normalise();
		  apn.set(arm_plane.x,arm_plane.y,arm_plane.z);
		  apn.normalise();
		  float bx = Vector3f.dot(temp, apn);
		  float by = temp.z;
		  angle_2 = (float) Math.atan2(by,bx);
		  
		  // the easiest part
		  angle_0 = (float) Math.atan2(y,x);
		
		  angle_1 *= -RAD2DEG;
		  angle_2 *= RAD2DEG;
		  angle_0 *= RAD2DEG;
		
		  return 0;
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
		Vector3f.add(temp, shoulder, elbow);
		
		temp.set(arm_plane.x*(float)Math.cos(angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				 arm_plane.y*(float)Math.cos(angle_2/RAD2DEG)*-ELBOW_TO_WRIST,
				             (float)Math.sin(angle_2/RAD2DEG)*-ELBOW_TO_WRIST);
		Vector3f.add(temp, elbow, wrist);
		
		temp.set(arm_plane.x*WRIST_TO_FINGER,
				 arm_plane.y*WRIST_TO_FINGER,
				 0);
		Vector3f.add(temp, wrist, finger_tip);
	}
	
	
	protected void update_fk(int delta) {
		boolean changed=false;
		if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
			angle_1 -= 0.35f * delta;
			angle_2 += 0.35f * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
			angle_1 += 0.35f * delta;
			angle_2 -= 0.35f * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_T)) {
			angle_2 += 0.35f * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_G)) {
			angle_2 -= 0.35f * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_Y)) {
			angle_0 += 0.35f * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_H)) {
			angle_0 -= 0.35f * delta;
			changed=true;
		}

		if(changed==true) {
			CheckAngleLimits();
			FK();
		}
	}

	
	protected void update_ik(int delta) {
		final float vel=0.1f;
		boolean changed=false;
		Vector3f newtip = new Vector3f();
		
		newtip.set(finger_tip);
		
		if (Keyboard.isKeyDown(Keyboard.KEY_U)) {
			newtip.x -= vel * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_J)) {
			newtip.x += vel * delta;
			changed=true;
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_I)) {
			newtip.y += vel * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_K)) {
			newtip.y -= vel * delta;
			changed=true;
		}
		
		if (Keyboard.isKeyDown(Keyboard.KEY_O)) {
			newtip.z += vel * delta;
			changed=true;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_L)) {
			newtip.z -= vel * delta;
			changed=true;
		}

		if(changed==true) {
			// check bounds
			if(newtip.x>60) newtip.x=60;
			if(newtip.x<-60) newtip.x=-60;
			if(newtip.y>60) newtip.y=60;
			if(newtip.y<-60) newtip.y=-60;
			if(newtip.z>30) newtip.z=30;
			if(newtip.z<-50) newtip.z=-50;
			
			Vector3f temp = new Vector3f();
			Vector3f.sub(newtip, shoulder, temp);
			if(temp.length() < 50) {
				temp.z=0;
				if(temp.length() > BASE_TO_SHOULDER_MINIMUM_LIMIT) {
					if(IK(newtip.x,newtip.y,newtip.z)==0) {
						finger_tip.set(newtip);
					}
				}
			}
		}
	}
	
	
	public void update(int delta) {
		update_fk(delta);
		update_ik(delta);
		
		// this will probably do awful things to memory and flood the serial connection
		arduino.DeleteAllQueuedCommands();
		arduino.SendCommand("G0 X"+finger_tip.x+" Y"+finger_tip.y+" Z"+finger_tip.z);
	}
	
	
	public void render() {
		// these two should always match!
		GL11.glPushMatrix();
		GL11.glTranslatef(base.x, base.y, base.z);

		//GL11.glColor3d(1,1,1);
		//PrimitiveSolids.drawStar(finger_tip,5);
		//PrimitiveSolids.drawStar(elbow,10);
		//PrimitiveSolids.drawStar(shoulder,15);
		
		drawFK();
		//drawIK();
		GL11.glPopMatrix();
	}
	
	
	protected void drawIK() {
		// finger tip
		GL11.glBegin(GL11.GL_LINES);

		GL11.glVertex3f(0,0,0);
		GL11.glColor3f(1,0,0);
		GL11.glVertex3f(shoulder.x,shoulder.y,shoulder.z);

		GL11.glVertex3f(shoulder.x,shoulder.y,shoulder.z);
		GL11.glColor3f(1,0,1);
		GL11.glVertex3f(elbow.x,elbow.y,elbow.z);

		GL11.glVertex3f(elbow.x,elbow.y,elbow.z);
		GL11.glColor3f(1,1,0);
		GL11.glVertex3f(wrist.x,wrist.y,wrist.z);

		GL11.glVertex3f(wrist.x,wrist.y,wrist.z);
		GL11.glColor3f(1,0.8f,0.2f);
		GL11.glVertex3f(finger_tip.x,finger_tip.y,finger_tip.z);
		
		GL11.glEnd();
	}
	
	
	protected void drawFK() {
		Vector3f a0 = new Vector3f(BASE_TO_SHOULDER_X,0,BASE_TO_SHOULDER_Z);
		Vector3f a1 = new Vector3f(0,0,SHOULDER_TO_ELBOW);
		Vector3f a2 = new Vector3f(0,0,ELBOW_TO_WRIST);
		Vector3f a3 = new Vector3f(0,0,WRIST_TO_FINGER);
		
		GL11.glPushMatrix();
		
		// base to shoulder
		GL11.glColor3f(1,1,1);
		GL11.glRotatef(angle_0,0,0,1);
		GL11.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(4,BASE_TO_SHOULDER_X*2,BASE_TO_SHOULDER_Z);
		GL11.glTranslatef(a0.x,a0.y,a0.z);

		// shoulder to elbow
		GL11.glRotatef(90+angle_1,0,1,0);
		GL11.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(3.1f,3.1f);
		GL11.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(3,3,SHOULDER_TO_ELBOW);

		// elbow to wrist
		GL11.glTranslatef(a1.x,a1.y,a1.z);
		GL11.glRotatef(180-angle_2-angle_1,0,1,0);
		GL11.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(2.1f,2.1f);
		GL11.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(2,2,ELBOW_TO_WRIST);

		// wrist to tool
		GL11.glTranslatef(a2.x,a2.y,a2.z);
		GL11.glRotatef(-180+angle_2,0,1,0);
		GL11.glColor3f(0,1,0);
		PrimitiveSolids.drawCylinder(1.1f,1.1f);
		GL11.glColor3f(0,0,1);
		PrimitiveSolids.drawBox(1,1,WRIST_TO_FINGER);
		GL11.glTranslatef(a3.x,a3.y,a3.z);	
		GL11.glPopMatrix();
	}
}
