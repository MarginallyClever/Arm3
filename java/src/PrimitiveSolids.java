import javax.vecmath.Vector3f;
import javax.media.opengl.GL2;


public class PrimitiveSolids {
	static void drawCylinder(GL2 gl2,float thickness,float diameter) {
		//thickness/=2;
		float radius=diameter;///2;
		int i;
		int c=36;
		
		// left
		gl2.glBegin(GL2.GL_TRIANGLE_FAN);
		gl2.glNormal3f(0,1,0);
		for(i=0;i<=c;++i) {
			float ratio= (float)Math.PI * 2.0f * (float)i/(float)c;
			gl2.glVertex3f((float)Math.sin(ratio)*radius,
							thickness,
							(float)Math.cos(ratio)*radius);
		}
		gl2.glEnd();
		// right
		gl2.glBegin(GL2.GL_TRIANGLE_FAN);
		gl2.glNormal3f(0,-1,0);
		for(i=0;i<=c;++i) {
			float ratio= (float)Math.PI * 2.0f * (float)i/(float)c;
			gl2.glVertex3f((float)Math.sin(ratio)*radius,
							-thickness,
							(float)Math.cos(ratio)*radius);
		}
		gl2.glEnd();

		// edge
		gl2.glBegin(GL2.GL_TRIANGLE_STRIP);
		for(i=0;i<=c;++i) {
			float ratio= (float)Math.PI * 2.0f * (float)i/(float)c;
			float a=(float)Math.sin(ratio)*radius;
			float b=thickness;
			float d=(float)Math.cos(ratio)*radius;
			gl2.glNormal3f(a,0,d);
			gl2.glVertex3f(a,b,d);
			gl2.glVertex3f(a,-b,d);
		}
		gl2.glEnd();
	}
	
	static void drawBox(GL2 gl2,float depth,float width,float height) {
		width/=2;
		depth/=2;

		gl2.glPushMatrix();
		gl2.glBegin(GL2.GL_QUADS);
		// bottom
		gl2.glNormal3f( 0, 0,-1);
		gl2.glVertex3f(-width,-depth,0);
		gl2.glVertex3f( width,-depth,0);
		gl2.glVertex3f( width, depth,0);
		gl2.glVertex3f(-width, depth,0);

		// top
		gl2.glNormal3f( 0, 0, 1);
		gl2.glVertex3f( width, depth,height);
		gl2.glVertex3f(-width, depth,height);
		gl2.glVertex3f(-width,-depth,height);
		gl2.glVertex3f( width,-depth,height);

		
		// side
		gl2.glNormal3f( 0, 1, 0);
		gl2.glVertex3f(-width, depth,height);
		gl2.glVertex3f( width, depth,height);
		gl2.glVertex3f( width, depth,0);
		gl2.glVertex3f(-width, depth,0);
		
		gl2.glNormal3f( 0,-1, 0);
		gl2.glVertex3f( width,-depth,height);
		gl2.glVertex3f(-width,-depth,height);
		gl2.glVertex3f(-width,-depth,0);
		gl2.glVertex3f( width,-depth,0);

		gl2.glNormal3f( 1, 0, 0);
		gl2.glVertex3f( width,-depth,0);
		gl2.glVertex3f( width,-depth,height);
		gl2.glVertex3f( width, depth,height);
		gl2.glVertex3f( width, depth,0);
	
		gl2.glNormal3f(-1, 0, 0);
		gl2.glVertex3f(-width, depth,height);
		gl2.glVertex3f(-width,-depth,height);
		gl2.glVertex3f(-width,-depth,0);
		gl2.glVertex3f(-width, depth,0);

		gl2.glEnd();
		
		gl2.glPopMatrix();
	}
	

	static void drawStar(GL2 gl2,Vector3f p) {
		drawStar(gl2,p,1.0f);
	}
	
	
	static void drawStar(GL2 gl2,Vector3f p,float size) {
		size/=2.0f;
		gl2.glPushMatrix();
		gl2.glTranslatef(p.x, p.y, p.z);
		gl2.glBegin(GL2.GL_LINES);
		gl2.glVertex3f(-size, 0, 0);
		gl2.glVertex3f(size, 0, 0);
		gl2.glVertex3f(0, -size, 0);
		gl2.glVertex3f(0, size, 0);
		gl2.glVertex3f(0, 0, -size);
		gl2.glVertex3f(0, 0, size);
		gl2.glEnd();
		gl2.glPopMatrix();
	}
}
