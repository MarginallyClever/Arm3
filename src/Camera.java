import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;


public class Camera {
	/** position of camera */
	Vector3f position = new Vector3f();
	Vector3f forward = new Vector3f();
	Vector3f up = new Vector3f();
	float pan;
	float tilt;
	
	public Camera() {
		position.set(0,100,-20);
		pan=0;
		tilt=80;
	}
	
	void update(int delta) {
		if (Mouse.isInsideWindow() && Mouse.isButtonDown(0)) {
			pan+=Mouse.getDX();
			tilt+=Mouse.getDY();
			if(tilt < 1) tilt=1;
			if(tilt > 179) tilt= 179;
		}

		forward.y = (float)Math.sin((-pan-90) * Math.PI/180.0) * (float)Math.cos((90-tilt) * Math.PI/180.0);
		forward.x = (float)Math.cos((-pan-90) * Math.PI/180.0) * (float)Math.cos((90-tilt) * Math.PI/180.0);
		forward.z =                                             (float)Math.sin((90-tilt) * Math.PI/180.0);
		
		up.set(0,0,1);

		Vector3f right = new Vector3f();
		Vector3f temp = new Vector3f();
		Vector3f.cross(forward, up, right);
		Vector3f.cross(forward, right, up);
		
		boolean changed=false;
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			// forward
			temp.set(forward);
			temp.scale(0.25f*delta);
			Vector3f.add(position, temp, position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			// back
			temp.set(forward);
			temp.scale(0.25f*-delta);
			Vector3f.add(position, temp, position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			// strafe left
			temp.set(right);
			temp.scale(0.25f*-delta);
			Vector3f.add(position, temp, position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			// strafe right
			temp.set(right);
			temp.scale(0.25f*delta);
			Vector3f.add(position, temp, position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
			// strafe left
			temp.set(up);
			temp.scale(0.25f*delta);
			Vector3f.add(position, temp, position);
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
			// strafe right
			temp.set(up);
			temp.scale(0.25f*-delta);
			Vector3f.add(position, temp, position);
		}
	}
	
	
	void render() {
		// move camera
		GL11.glRotatef(tilt, -1, 0, 0);
		GL11.glRotatef(pan,0,0,1);
    	GL11.glTranslatef(position.x,position.y,position.z);
	}
}
