import java.util.ArrayList;


public class RobotWithSerialConnection extends SerialConnection
implements SerialConnectionReadyListener {
	//comms	
	boolean arduinoReady=false;
	boolean isConfirmed=false;

	// sending file to the robot
	private boolean running=false;
	private boolean paused=true;
    private long linesTotal=0;
	private long linesProcessed=0;
	private boolean fileOpened=false;
	private ArrayList<String> gcode;

	
	public RobotWithSerialConnection(String name) {
		super(name);
		addListener(this);
	}
	
	
	public void SerialConnectionReady(SerialConnection arg0) {
		if(arg0==this) arduinoReady=true;
		
		if(arduinoReady) {
			if(!isConfirmed) {
				isConfirmed=true;
				//UpdateMenuBar();
			}
			arduinoReady=false;
			SendFileCommand();
		}
	}

	
	/**
	 * Take the next line from the file and send it to the robot, if permitted. 
	 */
	public void SendFileCommand() {
		if(running==false || paused==true || fileOpened==false || isConfirmed==false || linesProcessed>=linesTotal) return;
		
		String line;
		do {			
			// are there any more commands?
			line=gcode.get((int)linesProcessed++).trim();
			//previewPane.setLinesProcessed(linesProcessed);
			//statusBar.SetProgress(linesProcessed, linesTotal);
			// loop until we find a line that gets sent to the robot, at which point we'll
			// pause for the robot to respond.  Also stop at end of file.
		} while(!SendLineToRobot(line) && linesProcessed<linesTotal);
		
		if(linesProcessed==linesTotal) {
			// end of file
			Halt();
		}
	}

	
	/**
	 * stop sending commands to the robot.
	 * @todo add an e-stop command?
	 */
	public void Halt() {
		running=false;
		paused=false;
	    linesProcessed=0;
	}

	
	/**
	 * Processes a single instruction meant for the robot.
	 * @param line
	 * @return true if the command is sent to the robot.
	 */
	public boolean SendLineToRobot(String line) {
		// contains a comment?  if so remove it
		int index=line.indexOf('(');
		if(index!=-1) {
			//String comment=line.substring(index+1,line.lastIndexOf(')'));
			//Log("* "+comment+NL);
			line=line.substring(0,index).trim();
			if(line.length()==0) {
				// entire line was a comment.
				return false;  // still ready to send
			}
		}

		// send relevant part of line to the robot
		SendCommand(line);
		
		return true;
	}
}
