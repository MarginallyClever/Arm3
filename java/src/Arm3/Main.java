package Arm3;

public class Main {
	public static MainGUI gui;
	
	public static void main(String[] argv) {
	    //Schedule a job for the event-dispatching thread:
	    //creating and showing this application's GUI.
	    javax.swing.SwingUtilities.invokeLater(new Runnable() {
	        public void run() {
	        	gui = MainGUI.getSingleton();
	        }
	    });
	}
}
