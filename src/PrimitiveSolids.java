import org.lwjgl.opengl.GL11;


public class PrimitiveSolids {
	static void drawCylinder(float thickness,float diameter) {
		//thickness/=2;
		float radius=diameter;///2;
		int i;
		int c=10;
		
		// left
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glNormal3f(0,1,0);
		for(i=0;i<=c;++i) {
			float ratio= (float)Math.PI * 2.0f * (float)i/(float)c;
			GL11.glVertex3f((float)Math.sin(ratio)*radius,
							thickness,
							(float)Math.cos(ratio)*radius);
		}
		GL11.glEnd();
		// right
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glNormal3f(0,-1,0);
		for(i=0;i<=c;++i) {
			float ratio= (float)Math.PI * 2.0f * (float)i/(float)c;
			GL11.glVertex3f((float)Math.sin(ratio)*radius,
							-thickness,
							(float)Math.cos(ratio)*radius);
		}
		GL11.glEnd();

		// edge
		GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
		for(i=0;i<=c;++i) {
			float ratio= (float)Math.PI * 2.0f * (float)i/(float)c;
			float a=(float)Math.sin(ratio)*radius;
			float b=thickness;
			float d=(float)Math.cos(ratio)*radius;
			GL11.glNormal3f(a,0,d);
			GL11.glVertex3f(a,b,d);
			GL11.glVertex3f(a,-b,d);
		}
		GL11.glEnd();
	}
	
	static void drawBox(float depth,float width,float height) {
		width/=2;
		depth/=2;

		GL11.glPushMatrix();
		GL11.glBegin(GL11.GL_QUADS);
		// bottom
		GL11.glNormal3f( 0, 0,-1);
		GL11.glVertex3f(-width,-depth,0);
		GL11.glVertex3f( width,-depth,0);
		GL11.glVertex3f( width, depth,0);
		GL11.glVertex3f(-width, depth,0);

		// top
		GL11.glNormal3f( 0, 0, 1);
		GL11.glVertex3f( width, depth,height);
		GL11.glVertex3f(-width, depth,height);
		GL11.glVertex3f(-width,-depth,height);
		GL11.glVertex3f( width,-depth,height);

		
		// side
		GL11.glNormal3f( 0, 1, 0);
		GL11.glVertex3f(-width, depth,height);
		GL11.glVertex3f( width, depth,height);
		GL11.glVertex3f( width, depth,0);
		GL11.glVertex3f(-width, depth,0);
		
		GL11.glNormal3f( 0,-1, 0);
		GL11.glVertex3f( width,-depth,height);
		GL11.glVertex3f(-width,-depth,height);
		GL11.glVertex3f(-width,-depth,0);
		GL11.glVertex3f( width,-depth,0);

		GL11.glNormal3f( 1, 0, 0);
		GL11.glVertex3f( width,-depth,0);
		GL11.glVertex3f( width,-depth,height);
		GL11.glVertex3f( width, depth,height);
		GL11.glVertex3f( width, depth,0);
	
		GL11.glNormal3f(-1, 0, 0);
		GL11.glVertex3f(-width, depth,height);
		GL11.glVertex3f(-width,-depth,height);
		GL11.glVertex3f(-width,-depth,0);
		GL11.glVertex3f(-width, depth,0);

		GL11.glEnd();
		
		GL11.glPopMatrix();
	}
}
