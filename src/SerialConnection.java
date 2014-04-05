


import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class SerialConnection
implements SerialPortEventListener, ActionListener {
	private static String cue=">";
	private static String NL="\n";
	
	private String[] portsDetected;
	
	public static int BAUD_RATE = 57600;
	public CommPortIdentifier portIdentifier;
	public CommPort commPort;
	public SerialPort serialPort;
	public InputStream in;
	public OutputStream out;
	public boolean portOpened=false;
	public boolean portConfirmed=false;
	public String portName;
	public boolean waitingForCue=true;
	
	// settings
	private Preferences prefs;
	
	// menus & GUIs
	JTextArea log = new JTextArea();
	JScrollPane logPane;
    private JMenuItem [] buttonPorts;
    
    // communications
    String line3;
    ArrayList<String> commandQueue = new ArrayList<String>();

    // Listeners which should be notified of a change to the percentage.
    private ArrayList<SerialConnectionReadyListener> listeners = new ArrayList<SerialConnectionReadyListener>();

	
	public SerialConnection(String name) {
		prefs = Preferences.userRoot().node("SerialConnection").node(name);
		
		DetectSerialPorts();

		OpenPort(GetLastPort());
	}
	
	public void finalize() {
		ClosePort();
		//super.finalize();
	}
	
	private String GetLastPort(){
		return prefs.get("last port","");
	}
	
	private void SetLastPort(String portName) {
		prefs.put("last port", portName);
	}
	
	public void Log(String msg) {
		log.append(msg);
		log.setCaretPosition(log.getText().length());
	}

	// override this method to check that the software is connected to the right type of robot.
	public boolean ConfirmPort() {
		if(!portOpened) return false;
		
		portConfirmed=true;
		return true;
	}
	
	
	@Override
	public void serialEvent(SerialPortEvent events) {
        switch (events.getEventType()) {
        case SerialPortEvent.DATA_AVAILABLE:
        	if(!portOpened) break;
            try {
            	final byte[] buffer = new byte[1024];
				int len = in.read(buffer);
				if( len>0 ) {
					String line2 = new String(buffer,0,len);
					Log(line2);
					line3+=line2;
					// wait for the cue to send another command
					if(line3.lastIndexOf(cue)!=-1) {
						waitingForCue=false;
						if(ConfirmPort()) {
							line3="";
							SendQueuedCommand();
						}
					}
				}
            } catch (IOException e) {}
            break;
        }
	}
	
	protected void SendQueuedCommand() {
		if(!portOpened || waitingForCue) return;
		
		if(commandQueue.size()==0) {
		      notifyListeners();
		      return;
		}
		
		String command;
		try {
			command=commandQueue.remove(0)+";";
			Log(command+NL);
			out.write(command.getBytes());
			waitingForCue=true;
		}
		catch(IndexOutOfBoundsException e1) {}
		catch(IOException e2) {}
	}
	
	public void SendCommand(String command) {
		if(!portOpened) return;
		
		commandQueue.add(command);
		if(portConfirmed) SendQueuedCommand();
	}
	
	// find all available serial ports for the settings->ports menu.
	public void DetectSerialPorts() {
		@SuppressWarnings("unchecked")
	    Enumeration<CommPortIdentifier> ports = (Enumeration<CommPortIdentifier>)CommPortIdentifier.getPortIdentifiers();
	    ArrayList<String> portList = new ArrayList<String>();
	    while (ports.hasMoreElements()) {
	        CommPortIdentifier port = (CommPortIdentifier) ports.nextElement();
	        if (port.getPortType() == CommPortIdentifier.PORT_SERIAL) {
	        	portList.add(port.getName());
	        }
	    }
	    portsDetected = (String[]) portList.toArray(new String[0]);
	}
	
	public boolean PortExists(String portName) {
		if(portName==null || portName.equals("")) return false;

		int i;
		for(i=0;i<portsDetected.length;++i) {
			if(portName.equals(portsDetected[i])) {
				return true;
			}
		}
		
		return false;
	}
	
	public void ClosePort() {
		if(!portOpened) return;
		
	    if (serialPort != null) {
	        try {
	            // Close the I/O streams.
	            out.close();
	            in.close();
		        // Close the port.
		        serialPort.removeEventListener();
		        serialPort.close();
	        } catch (IOException e) {
	            // Don't care
	        }
	    }

		portOpened=false;
		portConfirmed=false;
	}
	
	// open a serial connection to a device.  We won't know it's the robot until  
	public int OpenPort(String portName) {
		if(portOpened && portName.equals(GetLastPort())) return 0;
		if(PortExists(portName) == false) return 0;
		
		ClosePort();
		
		Log("Connecting to "+portName+"..."+NL);
		
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		}
		catch(Exception e) {
			Log("Ports could not be identified:"+e.getMessage()+NL);
			e.printStackTrace();
			return 1;
		}

		if ( portIdentifier.isCurrentlyOwned() ) {
    	    Log("Error: Another program is currently using this port."+NL);
			return 2;
		}

		// open the port
		try {
		    commPort = portIdentifier.open("DrawbotGUI",2000);
		}
		catch(Exception e) {
			Log("Port could not be opened:"+e.getMessage()+NL);
			e.printStackTrace();
			return 3;
		}

	    if( ( commPort instanceof SerialPort ) == false ) {
			Log("Error: Only serial ports are handled."+NL);
			return 4;
		}

		// set the port parameters (like baud rate)
		serialPort = (SerialPort)commPort;
		try {
			serialPort.setSerialPortParams(BAUD_RATE,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		}
		catch(Exception e) {
			Log("Port could not be configured:"+e.getMessage()+NL);
			return 5;
		}

		try {
			in = serialPort.getInputStream();
			out = serialPort.getOutputStream();
		}
		catch(Exception e) {
			Log("Streams could not be opened:"+e.getMessage()+NL);
			return 6;
		}
		
		try {
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		}
		catch(TooManyListenersException e) {
			Log("Streams could not be opened:"+e.getMessage()+NL);
			return 7;
		}

		portOpened=true;
		SetLastPort(portName);

		return 0;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		Object subject = e.getSource();
		
		int i;
		for(i=0;i<portsDetected.length;++i) {
			if(subject == buttonPorts[i]) {
				OpenPort(portsDetected[i]);
				return;
			}
		}
	}

    // Adds a listener that should be notified.
    public void addListener(SerialConnectionReadyListener listener) {
      listeners.add(listener);
    }

    // Notifies all the listeners
    private void notifyListeners() {
      for (SerialConnectionReadyListener listener : listeners) {
        listener.SerialConnectionReady(this);
      }
    }

	public JMenu getMenu() {
		JMenu subMenu = new JMenu();
	    ButtonGroup group = new ButtonGroup();
	    buttonPorts = new JRadioButtonMenuItem[portsDetected.length];
	    
	    String lastPort=GetLastPort();
	    
		int i;
	    for(i=0;i<portsDetected.length;++i) {
	    	buttonPorts[i] = new JRadioButtonMenuItem(portsDetected[i]);
	        if(lastPort.equals(portsDetected[i])) buttonPorts[i].setSelected(true);
	        buttonPorts[i].addActionListener(this);
	        group.add(buttonPorts[i]);
	        subMenu.add(buttonPorts[i]);
	    }
	    
	    return subMenu;
	}
	

	public Component getGUI() {
	    // the log panel
	    log.setEditable(false);
	    log.setForeground(Color.GREEN);
	    log.setBackground(Color.BLACK);
	    logPane = new JScrollPane(log);
	    
	    return logPane;
	}
}
