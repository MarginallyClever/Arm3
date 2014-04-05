import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;


public class Camera {
	/** position of camera */
	Vector3f position = new Vector3f(0,0,0);
	float pan=0;
	float tilt=0;
	
	public Camera() {
		position.set(0,0,-100);
	}
	
	void update(int delta) {
		if (Mouse.isInsideWindow() && Mouse.isButtonDown(0)) {
			pan+=Mouse.getDX();
			tilt+=Mouse.getDY();
			if(tilt < 0) tilt= 0;
			if(tilt > 90) tilt=90;
		}
	}
	
	
	void render() {
		// move camera
    	GL11.glTranslatef(position.x,position.y,position.z);
		GL11.glRotatef(tilt, -1, 0, 0);
		GL11.glRotatef(pan,0,0,1);
	}
}
