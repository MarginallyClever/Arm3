
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPipelineFactory;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.opengl.util.Animator;



public class RobotTrainer 
implements ActionListener, GLEventListener
{
	World world;
	
	/** menus */
	static JMenuBar mainMenu;
	static JMenuItem buttonQuit;
	
	/* window management */
    final JFrame frame; 
    /* animation system */
    final Animator animator = new Animator();
    
    /* timing for animations */
    long start_time;
    long last_time;
    
    static RobotTrainer __singleton;
    
	
	public static void main(String[] argv) {
		GetInstance();
	}
	
	
	static public RobotTrainer GetInstance() {
		if(__singleton==null) __singleton = new RobotTrainer();
		return __singleton;
	}
	
	protected RobotTrainer() {
        frame = new JFrame( "RobotTrainer" ); 
        frame.setSize( 800, 600 );
        frame.setLayout(new java.awt.BorderLayout());

        // custom to this program start
		world = new World();
        mainMenu = new JMenuBar();
        updateMenu();
        frame.setJMenuBar(mainMenu);
        // custom to this program end

        
        final Animator animator = new Animator();
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
              // Run this on another thread than the AWT event queue to
              // make sure the call to Animator.stop() completes before
              // exiting
              new Thread(new Runnable() {
                  public void run() {
                    animator.stop();
                    System.exit(0);
                  }
                }).start();
            }
          });

        final GLCanvas glcanvas = new GLCanvas();
        animator.add(glcanvas);
        glcanvas.addGLEventListener(this);
        frame.add( glcanvas, BorderLayout.CENTER );
        frame.validate();
        frame.setVisible(true);
        animator.start();
        
        last_time = start_time = System.currentTimeMillis();
    }
	
	
	public void updateMenu() {
		mainMenu.removeAll();
		
        JMenu menu = new JMenu("RobotTrainer");
	        
	        buttonQuit = new JMenuItem("Quit");
	        buttonQuit.addActionListener(this);
	        menu.add(buttonQuit);
        
        mainMenu.add(menu);
		
        mainMenu.add(world.updateMenu());
	}
	
	
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		
		if(o==buttonQuit) {
			System.exit(0);
			return;
		}
	}
    

    @Override
    public void reshape( GLAutoDrawable glautodrawable, int x, int y, int width, int height ) {
    	GL2 gl2 = glautodrawable.getGL().getGL2();
        gl2.setSwapInterval(1);

		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glLoadIdentity();
		//gl2.glOrtho(0, screen_width, 0, screen_height, 1, -1);
		GLU glu = new GLU();
        glu.gluPerspective(45, (float)width/(float)height, 1.0f, 1000.0f);
        gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glLoadIdentity();
		
        world.setup( gl2 );
    }
    
    @Override
    public void init( GLAutoDrawable drawable ) {
    	// Use debug pipeline
    	boolean glDebug=true;
    	boolean glTrace=false;
    	
        GL gl = drawable.getGL();

        if(glDebug) {
            try {
                // Debug ..
                gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", null, gl, null) );
            } catch (Exception e) {e.printStackTrace();}
        }

        if(glTrace) {
            try {
                // Trace ..
                gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", null, gl, new Object[] { System.err } ) );
            } catch (Exception e) {e.printStackTrace();}
        }


		// MouseListener gearsMouse = new TraceMouseAdapter(new GearsMouseAdapter());
		MouseAdapter gearsMouse = new GearsMouseAdapter();
		KeyAdapter gearsKeys = new GearsKeyAdapter();
		
		if (drawable instanceof Window) {
			Window window = (Window) drawable;
			window.addMouseListener((MouseListener)gearsMouse);
			window.addKeyListener((KeyListener)gearsKeys);
		} else if (GLProfile.isAWTAvailable() && drawable instanceof java.awt.Component) {
			java.awt.Component comp = (java.awt.Component) drawable;
			new AWTMouseAdapter(gearsMouse).addTo(comp);
			new AWTKeyAdapter(gearsKeys).addTo(comp);
		}
    }
    
    
    @Override
    public void dispose( GLAutoDrawable glautodrawable ) {
    }
    
    
    @Override
    public void display( GLAutoDrawable glautodrawable ) {
        long now_time = System.currentTimeMillis();
        float dt = (now_time - last_time)*0.001f;
    	last_time = now_time;
    	//System.out.println(dt);
    	
		// Clear The Screen And The Depth Buffer
    	GL2 gl2 = glautodrawable.getGL().getGL2();

    	gl2.glClearColor(0.8f,0.8f,1, 0f);
    	
        // Special handling for the case where the GLJPanel is translucent
        // and wants to be composited with other Java 2D content
        if (GLProfile.isAWTAvailable() &&
            (glautodrawable instanceof javax.media.opengl.awt.GLJPanel) &&
            !((javax.media.opengl.awt.GLJPanel) glautodrawable).isOpaque() &&
            ((javax.media.opengl.awt.GLJPanel) glautodrawable).shouldPreserveColorBufferIfTranslucent()) {
          gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT);
        } else {
          gl2.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        }
        
        // draw the world
        world.render( gl2, dt );
    }
	
    
	  class GearsKeyAdapter extends KeyAdapter {
	    public void keyPressed(KeyEvent e) {
	        world.keyPressed(e);
	    }
	    public void keyReleased(KeyEvent e) {
	    	world.keyReleased(e);
	    }
	  }
	  
	  
	  class GearsMouseAdapter extends MouseAdapter {
	      public void mousePressed(MouseEvent e) {
		        world.mousePressed(e);
	      }
	        
	      public void mouseReleased(MouseEvent e) {
	    	  world.mouseReleased(e);
	      }
	        
	      public void mouseDragged(MouseEvent e) {
	    	  world.mouseDragged(e);
	      }
	  }
}
