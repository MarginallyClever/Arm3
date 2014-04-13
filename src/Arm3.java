
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import java.text.DecimalFormat;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;


public class Arm3 
implements ActionListener
{
	//GUI gui;
	
	Camera camera = new Camera();

	Arm3Robot robot0 = new Arm3Robot();
	Arm3Robot robot1 = new Arm3Robot();
	int activeRobot=1;

	float screen_width=800;
	float screen_height=600;
	
	/** time at last frame */
	long lastFrame;
	
	/** frames per second */
	int fps;
	/** last fps time */
	long lastFPS;

	/** menus */
	JMenuBar mainMenu;
	JMenuItem buttonQuit;
	
	
	public Arm3() {
		robot1.base.x=50;
	}
	
	
	public void run() {
		createWindow();
		
		initGL(); // init OpenGL
		getDelta(); // call once before loop to initialise lastFrame
		lastFPS = getTime(); // call before loop to initialise fps timer

		while (!Display.isCloseRequested()) {
			int delta = getDelta();
			
			update(delta);
			renderGL();

			//Display.update();
			//Display.sync(60); // cap fps to 60fps
		}

		//Display.destroy();
	}


	public void createWindow() {
		try {
			JFrame frame = new JFrame();
			AWTGLCanvas canvas = new AWTGLCanvas();
			canvas.setSize((int)screen_width,(int)screen_height);
		
	        frame.addWindowListener(new WindowAdapter() {
	        	@Override
	        	public void windowClosing(WindowEvent e) {
	        		System.exit(0);
	        	}
	        });
	
	        mainMenu = new JMenuBar();
	        updateMenu();
	        
			frame.add(canvas,BorderLayout.CENTER);
			frame.setJMenuBar(mainMenu);
			frame.pack();
	        frame.setVisible(true);
/*
			Display.setDisplayMode(new DisplayMode((int)screen_width,(int)screen_height));
			Display.setVSyncEnabled(true);
			Display.create();
/*
	        LWJGLRenderer renderer = new LWJGLRenderer();
	        GameUIDemo gameUI = new GameUIDemo();
	        GUI gui = new GUI(gameUI, renderer);

	        ThemeManager theme = ThemeManager.createThemeManager(getResource("gameui.xml"), renderer);
	        gui.applyTheme(theme);*/
	        
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	
	public void updateMenu() {
		mainMenu.removeAll();
		
        JMenu menu = new JMenu("Arm3");
        buttonQuit = new JMenuItem("Quit");

        menu.add(buttonQuit);
        mainMenu.add(menu);
	}
	
	
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		
		if(o==buttonQuit) {
			System.exit(0);
			return;
		}
	}
	
	public void update(int delta) {
		camera.update(delta);

		if (Keyboard.isKeyDown(Keyboard.KEY_Z) && !Keyboard.isRepeatEvent()) {
			activeRobot=(activeRobot+1)%2;
		}
		
		//gui.update();

		switch(activeRobot) {
		case 0: robot0.update(delta); break;
		case 1: robot1.update(delta); break;
		}
		
		updateFPS(); // update FPS Counter
	}
	
	
	/** 
	 * Calculate how many milliseconds have passed 
	 * since last frame.
	 * 
	 * @return milliseconds passed since last frame 
	 */
	public int getDelta() {
	    long time = getTime();
	    int delta = (int) (time - lastFrame);
	    lastFrame = time;
	 
	    return delta;
	}
	
	/**
	 * Get the accurate system time
	 * 
	 * @return The system time in milliseconds
	 */
	public long getTime() {
	    return (Sys.getTime() * 1000) / Sys.getTimerResolution();
	}
	
	/**
	 * Calculate the FPS and set it in the title bar
	 */
	public void updateFPS() {
		if (getTime() - lastFPS > 250) {
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(2);
			Display.setTitle("FPS: " + fps + /*" "+
					df.format(robot.getAngle0())+","+
					df.format(robot.getAngle1())+","+
					df.format(robot.getAngle2())+" ("+
					df.format(robot.finger_tip.x)+","+
					df.format(robot.finger_tip.y)+","+
					df.format(robot.finger_tip.z)+")"*/" ("+
					df.format(camera.position.x)+","+
					df.format(camera.position.y)+","+
					df.format(camera.position.z)+") "+
					df.format(camera.pan)+","+
					df.format(camera.tilt)
					);
			fps = 0;
			lastFPS += 1000;
		}
		fps++;
	}
	
	
	public void initGL() {
		GL11.glDepthFunc(GL11.GL_LESS);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		//GL11.glOrtho(0, screen_width, 0, screen_height, 1, -1);
		GLU.gluPerspective(45, screen_width/screen_height, 1.0f, 1000.0f);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
	}
	

	public void renderGL() {
		// Clear The Screen And The Depth Buffer
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		//GL11.glEnable(GL11.GL_CULL_FACE);
		
		
		GL11.glPushMatrix();
			camera.render();

			GL11.glDisable(GL11.GL_LIGHTING);
			drawGrid();
			 // Enable lighting
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_LIGHT0);
			GL11.glEnable(GL11.GL_COLOR_MATERIAL);
			/*
			FloatBuffer position = ByteBuffer.allocateDirect(16).asFloatBuffer();
		    position.mark();
		    position.put(new float[] { -10f, 10f, 50f, 0f }); // even values about 10e3 for the first three parameters aren't changing anything
		    position.reset();
			GL11.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, position);

		    FloatBuffer ambient = ByteBuffer.allocateDirect(16).asFloatBuffer();
		    ambient.mark();
		    ambient.put(new float[] { 0.85f, 0.85f, 0.85f, 1f });
		    ambient.reset();
		    GL11.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, ambient);

		    FloatBuffer diffuse = ByteBuffer.allocateDirect(16).asFloatBuffer();
		    diffuse.mark();
		    diffuse.put(new float[] { 1.0f, 1.0f, 1.0f, 1f });
		    diffuse.reset();
		    GL11.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, diffuse);
*/
			robot0.render();
			robot1.render();
			
		GL11.glPopMatrix();
	}


	void drawGrid() {
		final int grid_size=50;
		final int grid_space=1;
		GL11.glColor3f(0.2f,0.2f,0.2f);
		GL11.glNormal3f(0,0,1);
	
		GL11.glBegin(GL11.GL_LINES);
		for(int i=-grid_size;i<=grid_size;i+=grid_space) {
			for(int j=-grid_size;j<=grid_size;j+=grid_space) {
				GL11.glVertex3f(i,-grid_size,0);
				GL11.glVertex3f(i,grid_size,0);					
				GL11.glVertex3f(-grid_size,j,0);
				GL11.glVertex3f(grid_size,j,0);					
			}
		}
	//*/
		/*
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3f(grid_size,-grid_size,0);
		GL11.glVertex3f(-grid_size,-grid_size,0);					
		GL11.glVertex3f(-grid_size,grid_size,0);
		GL11.glVertex3f(grid_size,grid_size,0);					
		GL11.glEnd();*/
	
		GL11.glBegin(GL11.GL_LINES);
		
		// +X line
		GL11.glColor3f(0.5f,0,0);
		GL11.glVertex3f(0,0,0);
		GL11.glVertex3f(grid_size*2,0,0);
		// +Y line
		GL11.glColor3f(0,0.5f,0);
		GL11.glVertex3f(0,0,0);
		GL11.glVertex3f(0,grid_size*2,0);
		// +Z line
		GL11.glColor3f(0,0,0.5f);
		GL11.glVertex3f(0,0,0);
		GL11.glVertex3f(0,0,grid_size*2);
	
		GL11.glEnd();
	}
	
	
	public static void main(String[] argv) {
		Arm3 example = new Arm3();
		example.run();
	}
}
