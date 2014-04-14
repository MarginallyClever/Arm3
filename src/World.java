import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.KeyEvent;

import javax.media.opengl.GL2;
import javax.swing.JMenu;
import javax.swing.JMenuItem;


public class World 
implements ActionListener {
	/* menus */
	JMenuItem buttonRescan, buttonDisconnect;
	
	/* world contents */
	Camera camera = new Camera();
	Arm3Robot robot0 = new Arm3Robot("0");
	Arm3Robot robot1 = new Arm3Robot("1");
	final int NUM_ROBOTS = 2;
	int activeRobot=0;

	
	public World() {
		robot1.base.x=50;
	}
	
	
    protected void setup( GL2 gl2 ) {
		gl2.glDepthFunc(GL2.GL_LESS);
		gl2.glEnable(GL2.GL_DEPTH_TEST);
		gl2.glDepthMask(true);
    }
    

    protected void render( GL2 gl2, float dt ) {
		//gl2.glEnable(GL2.GL_CULL_FACE);
		
		
		gl2.glPushMatrix();
			camera.render(gl2);

			gl2.glDisable(GL2.GL_LIGHTING);
			drawGrid(gl2);
			 // Enable lighting
			gl2.glEnable(GL2.GL_LIGHTING);
			gl2.glEnable(GL2.GL_LIGHT0);
			gl2.glEnable(GL2.GL_COLOR_MATERIAL);
			/*
			FloatBuffer position = ByteBuffer.allocateDirect(16).asFloatBuffer();
		    position.mark();
		    position.put(new float[] { -10f, 10f, 50f, 0f }); // even values about 10e3 for the first three parameters aren't changing anything
		    position.reset();
			gl2.glLight(GL2.GL_LIGHT0, GL2.GL_POSITION, position);

		    FloatBuffer ambient = ByteBuffer.allocateDirect(16).asFloatBuffer();
		    ambient.mark();
		    ambient.put(new float[] { 0.85f, 0.85f, 0.85f, 1f });
		    ambient.reset();
		    gl2.glLight(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambient);

		    FloatBuffer diffuse = ByteBuffer.allocateDirect(16).asFloatBuffer();
		    diffuse.mark();
		    diffuse.put(new float[] { 1.0f, 1.0f, 1.0f, 1f });
		    diffuse.reset();
		    gl2.glLight(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuse);
*/
			robot0.render(gl2,dt);
			robot1.render(gl2,dt);
			
		gl2.glPopMatrix();
    }


    public void mouseClicked(MouseEvent e) {
    	
    }
    public void mouseDragged(MouseEvent e) {
    	camera.mouseDragged(e);
    }
    public void mouseEntered(MouseEvent e) {
    	
    }
    public void mouseExited(MouseEvent e) {
    	
    }
    public void mouseMoved(MouseEvent e) {

    }
    public void mousePressed(MouseEvent e) {
    	camera.mousePressed(e);
    	
    }
    public void mouseReleased(MouseEvent e) {
    	camera.mouseReleased(e);
    	
    }
    public void mouseWheelMoved(MouseEvent e) {
    	
    }
    
    public void keyPressed(KeyEvent e) {
    	if(e.getKeyCode() == KeyEvent.VK_SPACE) {
    		activeRobot=(activeRobot+1)%NUM_ROBOTS;
    	}
    	camera.keyPressed(e);
    	switch(activeRobot) {
    	case 0:    	robot0.keyPressed(e); break;
    	case 1:    	robot1.keyPressed(e); break;
    	}
    }
    
    public void keyReleased(KeyEvent e) {
    	camera.keyReleased(e);
    	switch(activeRobot) {
    	case 0:    	robot0.keyReleased(e); break;
    	case 1:    	robot1.keyReleased(e); break;
    	}
    }
    

	public void actionPerformed(ActionEvent e) {
		Object subject = e.getSource();
		if(subject==buttonRescan) {
			robot0.DetectSerialPorts();
			//robot1.arduino.DetectSerialPorts();
			//TODO tell RobotTrainer to update all menus
			RobotTrainer.GetInstance().updateMenu();
			return;
		}
		if(subject==buttonDisconnect) {
			robot0.ClosePort();
			//robot1.arduino.ClosePort();
			RobotTrainer.GetInstance().updateMenu();
			return;
		}
	}
	
    public JMenu updateMenu() {
    	JMenu menu, subMenu;
        
        // connection menu
        menu = new JMenu("Connection(s)");
        menu.setMnemonic(KeyEvent.VK_T);
        menu.getAccessibleContext().setAccessibleDescription("Connection settings.");
        
    	subMenu=robot0.getMenu();
        subMenu.setText("Arm 0");
        menu.add(subMenu);

     	subMenu=robot1.getMenu();
        subMenu.setText("Arm 1");
        menu.add(subMenu);

        buttonRescan = new JMenuItem("Rescan Ports",KeyEvent.VK_R);
        buttonRescan.getAccessibleContext().setAccessibleDescription("Rescan the available ports.");
        buttonRescan.addActionListener(this);
        menu.add(buttonRescan);

        menu.addSeparator();
        
        buttonDisconnect = new JMenuItem("Disconnect",KeyEvent.VK_D);
        buttonDisconnect.addActionListener(this);
        menu.add(buttonDisconnect);
        
        return menu;
    }

	protected void drawGrid(GL2 gl2) {
		final int grid_size=50;
		final int grid_space=1;
		gl2.glColor3f(0.2f,0.2f,0.2f);
		gl2.glNormal3f(0,0,1);
	
		gl2.glBegin(GL2.GL_LINES);
		for(int i=-grid_size;i<=grid_size;i+=grid_space) {
			for(int j=-grid_size;j<=grid_size;j+=grid_space) {
				gl2.glVertex3f(i,-grid_size,0);
				gl2.glVertex3f(i,grid_size,0);					
				gl2.glVertex3f(-grid_size,j,0);
				gl2.glVertex3f(grid_size,j,0);					
			}
		}
		gl2.glEnd();
	//*/
		/*
		gl2.glBegin(GL2.GL_QUADS);
		gl2.glVertex3f(grid_size,-grid_size,0);
		gl2.glVertex3f(-grid_size,-grid_size,0);					
		gl2.glVertex3f(-grid_size,grid_size,0);
		gl2.glVertex3f(grid_size,grid_size,0);					
		gl2.glEnd();*/
	
		gl2.glBegin(GL2.GL_LINES);
		
		// +X line
		gl2.glColor3f(0.5f,0,0);
		gl2.glVertex3f(0,0,0);
		gl2.glVertex3f(grid_size*2,0,0);
		// +Y line
		gl2.glColor3f(0,0.5f,0);
		gl2.glVertex3f(0,0,0);
		gl2.glVertex3f(0,grid_size*2,0);
		// +Z line
		gl2.glColor3f(0,0,0.5f);
		gl2.glVertex3f(0,0,0);
		gl2.glVertex3f(0,0,grid_size*2);
	
		gl2.glEnd();
	}
}
