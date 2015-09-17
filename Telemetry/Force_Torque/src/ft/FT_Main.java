package ft;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.BadLocationException;

import com.atiia.automation.sensors.NetFTRDTPacket;
import com.atiia.automation.sensors.NetFTSensor;

import RTADiscoveryProtocol.DiscoveryClient;
import RTADiscoveryProtocol.RTADevice;

@SuppressWarnings("serial")
public class FT_Main extends JFrame{

	private JLabel label_devicelist;
	private JComboBox<String> combobox_devicelist;
	private JLabel label_ipaddress;
	private JLabel label_port;
	private JLabel label_frequency;
	private JLabel label_speed;
	private JButton button_refresh;
	private JButton button_connect;
	private JButton button_close;
	private JTextField textfield_ipaddress1;
	private JTextField textfield_ipaddress2;
	private JTextField textfield_ipaddress3;
	private JTextField textfield_ipaddress4;
	private JTextField textfield_port;
	private JTextField textfield_frequency;
	private JCheckBox checkbox_speed;
	private JTextArea textarea_status;
	private JScrollPane scrollpane_status;
	private JFileChooser fileChooser;
	private DiscoveryClient discoveryClient;
	private RTADevice[] rtaDevices;
	private NetFTSensor sensor;
	private NetFTRDTPacket[] packets;
	private NetFTRDTPacket packet;
	private DatagramSocket slowDataSocket;
	private DatagramSocket fastDataSocket;
	private Thread threadLowSpeed,threadHighSpeed;
	private StringBuilder ipaddress;
	private FileWriter fileWriter;
	private File file;
	private int m_iRDTSampleRate; 
	private double[] m_daftCountsPerUnit = {1, 1, 1, 1, 1, 1}; //Counts for Force and Torque 3*2
	
	public FT_Main() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		initComponents();
		getContentPane().add(label_devicelist);
		getContentPane().add(combobox_devicelist);
		getContentPane().add(button_refresh);
		getContentPane().add(label_ipaddress);
		getContentPane().add(label_speed);
		getContentPane().add(checkbox_speed);
		getContentPane().add(textfield_ipaddress1);
		getContentPane().add(textfield_ipaddress2);
		getContentPane().add(textfield_ipaddress3);
		getContentPane().add(textfield_ipaddress4);
		getContentPane().add(label_port);
		getContentPane().add(label_frequency);
		getContentPane().add(textfield_port);
		getContentPane().add(textfield_frequency);
		getContentPane().add(button_connect);
		getContentPane().add(button_close);
		button_connect.setEnabled(false);
		button_close.setEnabled(false);
		this.setSize(470, 350);
		getContentPane().setLayout(null);
		textarea_status = new JTextArea();
		getContentPane().add(textarea_status);
		textarea_status.setBounds(23,182,410,118);
		textarea_status.setLineWrap(true);
		getContentPane().add(scrollpane_status);
		scrollpane_status = new JScrollPane();
		scrollpane_status.setBounds(23,182,405,120);
		this.setTitle("Force / Torque Sensor - ATI");
		Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
	    int x = (int) ((dimension.getWidth() - this.getWidth()) / 2);
	    int y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
	    this.setLocation(x, y);
		this.setVisible(true);
	}
	public static void main(String args[])
	{
		FT_Main ftmain;
		try 
		{
			ftmain = new FT_Main();
		} 
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (InstantiationException e) 
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e) 
		{
			e.printStackTrace();
		} 
		catch (UnsupportedLookAndFeelException e) 
		{
			e.printStackTrace();
		}
	}
	private void initComponents()
	{
		label_devicelist = new JLabel("List of Devices");
		label_devicelist.setBounds(23,29,150,10);
		combobox_devicelist = new JComboBox<String>();
		combobox_devicelist.setBounds(33, 50, 300, 20);
		combobox_devicelist.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					String ipaddress = rtaDevices[combobox_devicelist.getSelectedIndex()].toString().substring(3,rtaDevices[combobox_devicelist.getSelectedIndex()].toString().indexOf(" "));
					String[] ip = ipaddress.split(".");
					textfield_ipaddress1.setText(ip[0]);
					textfield_ipaddress2.setText(ip[1]);
					textfield_ipaddress3.setText(ip[2]);
					textfield_ipaddress4.setText(ip[3]);
				}
				catch(Exception ex)
				{
					textarea_status.append("Cannot Parse IP Address...\n");
				}
			}
		});
		button_refresh = new JButton("Refresh...");
		button_refresh.setBounds(343,50,90,20);
		button_refresh.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) 
			{
				button_refresh.setEnabled(false);
				refreshDevices();	
			}
		});
		label_ipaddress = new JLabel("Enter IP Address");
		label_ipaddress.setBounds(23,80,150,10);
		label_speed = new JLabel("Speed");
		label_speed.setBounds(224,80,100,10);
		checkbox_speed = new JCheckBox("High Speed");
		checkbox_speed.setBounds(234, 100, 79, 20);
		label_port = new JLabel("Enter Port");
		label_port.setBounds(23,130,60,10);
		label_frequency = new JLabel("Enter Frequency");
		label_frequency.setBounds(130,131,90,10);
		textfield_ipaddress1 = new JTextField();
		textfield_ipaddress1.setBounds(50,100,30,20);
		textfield_ipaddress1.setText("192");
		textfield_ipaddress1.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent arg0) {
				
				if(textfield_ipaddress1.getText().trim().equals(""))
				{
					textfield_ipaddress1.setText("192");
				}
				else if(Integer.parseInt(textfield_ipaddress1.getText().trim())>255)
				{
					textfield_ipaddress1.setText("192");
				}
			}
			
			public void focusGained(FocusEvent arg0) {
				
					textfield_ipaddress1.setText("");
			}
		});
		textfield_ipaddress2 = new JTextField();
		textfield_ipaddress2.setBounds(90,100,30,20);
		textfield_ipaddress2.setText("168");
		textfield_ipaddress2.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent arg0) {
				
				if(textfield_ipaddress2.getText().trim().equals(""))
				{
					textfield_ipaddress2.setText("168");
				}
				else if(Integer.parseInt(textfield_ipaddress2.getText().trim())>255)
				{
					textfield_ipaddress2.setText("168");
				}
			}
			
			public void focusGained(FocusEvent arg0) {
				
					textfield_ipaddress2.setText("");
			}
		});
		textfield_ipaddress3 = new JTextField();
		textfield_ipaddress3.setBounds(130,100,30,20);
		textfield_ipaddress3.setText("1");
		textfield_ipaddress3.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent arg0) {
				
				if(textfield_ipaddress3.getText().trim().equals(""))
				{
					textfield_ipaddress3.setText("1");
				}
				else if(Integer.parseInt(textfield_ipaddress3.getText().trim())>255)
				{
					textfield_ipaddress3.setText("1");
				}
			}
			
			public void focusGained(FocusEvent arg0) {
				
					textfield_ipaddress3.setText("");
			}
		});
		textfield_ipaddress4 = new JTextField();
		textfield_ipaddress4.setBounds(170,100,30,20);
		textfield_ipaddress4.setText("1");
		textfield_ipaddress4.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent arg0) {
				
				if(textfield_ipaddress4.getText().trim().equals(""))
				{
					textfield_ipaddress4.setText("1");
				}
				else if(Integer.parseInt(textfield_ipaddress4.getText().trim())>255)
				{
					textfield_ipaddress4.setText("1");
				}
			}
			
			public void focusGained(FocusEvent arg0) {
				
					textfield_ipaddress4.setText("");
			}
		});
		textfield_port = new JTextField();
		textfield_port.setBounds(50,151,60,20);
		textfield_port.setText("49152");
		textfield_port.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent arg0) {
				
				if(textfield_port.getText().trim().equals(""))
				{
					textfield_port.setText("49152");
				}
				
			}
			
			public void focusGained(FocusEvent arg0) {
					
					textfield_port.setText("");
				
			}
		});
		textfield_frequency = new JTextField();
		textfield_frequency.setBounds(160,152,60,20);
		textfield_frequency.setText("1");
		textfield_frequency.addFocusListener(new FocusListener() {
			
			public void focusLost(FocusEvent arg0) {
				
				if(textfield_frequency.getText().trim().equals(""))
				{
					textfield_frequency.setText("1");
				}
				
			}
			
			public void focusGained(FocusEvent arg0) {
					
					textfield_frequency.setText("");
				
			}
		});
		button_connect = new JButton("Connect");
		button_connect.setBounds(243,151,90,20);
		button_connect.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				try
				{
					if(!textfield_ipaddress1.getText().trim().equals("")&&!textfield_ipaddress2.getText().trim().equals("")&&!textfield_ipaddress3.getText().trim().equals("")&&!textfield_ipaddress4.getText().trim().equals(""))
					{
						fileChooser = new JFileChooser();
						fileChooser.setDialogTitle("Enter File Name to Save...");
						fileChooser.setSelectedFile(new File("output.txt"));
						int status = fileChooser.showSaveDialog(FT_Main.this);
						if(status == JFileChooser.APPROVE_OPTION)
						{
								final String output = fileChooser.getSelectedFile().getAbsolutePath();
								ipaddress = new StringBuilder();
								ipaddress.append(textfield_ipaddress1.getText().trim());
								ipaddress.append(".");
								ipaddress.append(textfield_ipaddress2.getText().trim());
								ipaddress.append(".");
								ipaddress.append(textfield_ipaddress3.getText().trim());
								ipaddress.append(".");
								ipaddress.append(textfield_ipaddress4.getText().trim());
								int port = Integer.parseInt(textfield_port.getText().trim());
								final int frequency = Integer.parseInt(textfield_frequency.getText().trim());
								
								//To read configuration details about sensor
								if(!readConfigurationInfo(ipaddress.toString()))
								{
									return;
								}
								if(checkbox_speed.isSelected()==false)
								{
										//For Low Speed
										textarea_status.removeAll();
										textarea_status.append("Initiating Read with Low Speed...\n");
										sensor = new NetFTSensor(InetAddress.getByName(ipaddress.toString()),port);
										if(sensor != null)
										{
											textarea_status.append("Sensor Object Initiated...\n");
										}
										slowDataSocket = sensor.initLowSpeedData();
										if(slowDataSocket!=null)
										{
											textarea_status.append("Datagram Socket Created...\n");
										}
										textarea_status.append("Starting Data Read...\n");
										threadLowSpeed = new Thread() {
											
											@Override
											public void run() {
												
												int i = 0;
												int fileName = 0;
												while(true)
												{
													try
													{
														file = new File(output);
														fileWriter = new FileWriter(file,true);
														packet = sensor.readLowSpeedData(slowDataSocket);
														//packet = new NetFTRDTPacket(i,i,i,i,i,i,i,i,i);
														if(packet!=null)
														{
															fileWriter.write(packet.toString(m_daftCountsPerUnit));
															fileWriter.write(System.getProperty( "line.separator"));
															textarea_status.replaceRange("", 0, textarea_status.getLineEndOffset(0));
															textarea_status.append(packet.toString()+"\n");
														}
													}
													catch(Exception ex)
													{
														try 
														{
															textarea_status.replaceRange("", 0, textarea_status.getLineEndOffset(0));
															textarea_status.append("Cannot Read Data from Sensor...\n");
														} 
														catch (BadLocationException e) 
														{
															e.printStackTrace();
														}
													}
													finally
													{
														if(fileWriter!=null)
														{
															try 
															{
																fileWriter.close();
															} 
															catch (IOException e) 
															{
																e.printStackTrace();
															}
														}
													}
													try 
													{
														this.sleep(frequency*1000);
													} 
													catch (InterruptedException e) 
													{
														e.printStackTrace();
													}
													i++;
												}
											}
										};
										threadLowSpeed.start();
								}
								else
								{
									//For High Speed
									textarea_status.removeAll();
									textarea_status.append("Initiating Read with High Speed...\n");
									sensor = new NetFTSensor(InetAddress.getByName(ipaddress.toString()),port);
									if(sensor != null)
									{
										textarea_status.append("Sensor Object Initiated...\n");
									}
									fastDataSocket = sensor.startHighSpeedDataCollection(0);
									if(fastDataSocket!=null)
									{
										textarea_status.append("Datagram Socket Created...\n");
									}
									textarea_status.append("Starting Data Read...\n");
									threadHighSpeed = new Thread() {
										
										@Override
										public void run() {
											
											int fileName = 0;
											while(true)
											{
												try
												{
													int count = Math.max(m_iRDTSampleRate / 10, 1);
													file = new File(output);
													fileWriter = new FileWriter(file,true);
													packets = new NetFTRDTPacket[count];
													packets = sensor.readHighSpeedData(fastDataSocket,count);
													for(int i=0;i<count;i++)
													{
														fileWriter.write(packets[i].toString(m_daftCountsPerUnit));
														//fileWriter.write(System.getProperty( "line.separator"));
														textarea_status.replaceRange("", 0, textarea_status.getLineEndOffset(0));
														textarea_status.append(packet.toString()+"\n");
													}
												}
												catch(Exception ex)
												{
													try 
													{
														textarea_status.replaceRange("", 0, textarea_status.getLineEndOffset(0));
														textarea_status.append("Cannot Read Data from Sensor...\n");
													} 
													catch (BadLocationException e) 
													{
														e.printStackTrace();
													}
												}
												finally
												{
													if(fileWriter!=null)
													{
														try 
														{
															fileWriter.close();
														} 
														catch (IOException e) 
														{
															e.printStackTrace();
														}
													}
												}
												try 
												{
													this.sleep(frequency*1000);
												} 
												catch (InterruptedException e) 
												{
													e.printStackTrace();
												}
											}
										}
									};
									threadLowSpeed.start();
								}
									}
						}
						else
						{
							textarea_status.append("IP Address, Port Number or Frequency Should Not Be Empty"+"\n");
							return;
						}
					}
					catch(Exception ex)
					{
						textarea_status.append(ex.toString()+"\n");
					}
				}
			});
		button_close = new JButton("Close");
		button_close.setBounds(343,151,90,20);
		button_close.addActionListener(new ActionListener() 
		{
			@SuppressWarnings("deprecation")
			public void actionPerformed(ActionEvent arg0) 
			{
				try
				{
					if(!checkbox_speed.isSelected())
					{
						if(slowDataSocket!=null && !slowDataSocket.isClosed())
						{
							sensor.stopDataCollection(slowDataSocket);
							textarea_status.append("Socket Closed...\n");
						}
						if(fileWriter!=null)
						{
							fileWriter.close();
						}
						if(threadLowSpeed!=null)
						{
							threadLowSpeed.interrupt();
						}
					}
					else
					{
						if(fastDataSocket!=null && !fastDataSocket.isClosed())
						{
							sensor.stopDataCollection(fastDataSocket);
							textarea_status.append("Socket Closed...\n");
						}
						if(fileWriter!=null)
						{
							fileWriter.close();
						}
						if(threadHighSpeed!=null)
						{
							threadHighSpeed.interrupt();
						}
					}
					
					
				}
				catch(Exception ex)
				{
					textarea_status.append(ex.toString()+"\n");
					ex.printStackTrace();
				}
			}
		});
	}
	
	private String readWebPageText( String strUrlSuffix , String m_strSensorAddress) throws 
    MalformedURLException, IOException
	{
		  /*Reads the HTML from the web server.*/
		  BufferedReader cBufferedReader;
		  /*The url of the configuration page.*/
		  String strURL = "http://" + m_strSensorAddress + "/" +
		          strUrlSuffix;
		  cBufferedReader = new BufferedReader ( new InputStreamReader ( new
		          URL(strURL).openConnection().getInputStream()));        
		  /*The text of the page.*/
		  String strPageText = "";
		  /*The last line read from the web stream.*/
		  String strCurLine;
		  /*Precondition: cBufferedReader is at the beginning of the page.
		   *Postcondition: cBufferedReader is finished, strPageText =
		   *the text of the page, strCurLine = last line read from the 
		   *page.
		   */
		   while ( null != ( strCurLine = cBufferedReader.readLine() ) ) {            
		      strPageText += strCurLine;
		   }     
		   return strPageText;
}
	
	//Read NetFT Cal API
	private String readNetFTCalAPI(int index, String ipAddress)
    {
        try{
        String strXML = readWebPageText("netftcalapi.xml?index="+index,ipAddress);
        return strXML;
        }catch(Exception e)
        {
            return "";
        }
    }
	//Read NetFT API
	private String readNetFTAPI(int index,String ipAddress)
    {
        try{
        String strXML = readWebPageText("netftapi2.xml?index="+index,ipAddress);
        return strXML;
        }catch(Exception e)
        {
            return "";
        }
    }
	
	private int findActiveCFG(String xmlText)
    {
       String[] strret = xmlText.split("<setcfgsel>");
       String[] strret2 = strret[1].split("</setcfgsel>");
       int activeConfig = Integer.parseInt(strret2[0]);
       return activeConfig;       
    }
	
	private void setCountsPerForce( double counts )
    {
        double dCountsPerForce = counts;
        if ( 0 == dCountsPerForce ){
            dCountsPerForce = 1;
        }
        int i;
        for ( i = 0; i < 3; i++ )
        {
            m_daftCountsPerUnit[i] = dCountsPerForce;
        }
    }
    
    private void setCountsPerTorque( double counts )
    {
        double dCountsPerTorque = counts;
        if ( 0 == dCountsPerTorque ) {
            dCountsPerTorque = 1;
        }
        int i;
        for ( i = 0; i < 3; i++ )
        {
            m_daftCountsPerUnit[i+3] = dCountsPerTorque;
        }
    }
	//Pre Function Calls to Read Configuration Info
	private boolean readConfigurationInfo(String ipAddress)
    { 
        try
        {
        String mDoc = readNetFTAPI(0,ipAddress);
        int activeConfig = findActiveCFG(mDoc);
        mDoc = readNetFTAPI(activeConfig,ipAddress);
        //m_strCfgIndex = "" + activeConfig;
        String[] parseStep1 = mDoc.split("<cfgcalsel>");
        String[] parseStep2 = parseStep1[1].split("</cfgcalsel>");
        String mCal = readNetFTCalAPI(Integer.parseInt(parseStep2[0]),ipAddress);
        //m_strCalIndex = parseStep2[0];
        //parseStep1 = mCal.split("<calsn>");
        //parseStep2 = parseStep1[1].split("</calsn>");
        //m_strCalSN = parseStep2[0];
        mDoc = readNetFTAPI(activeConfig,ipAddress);
        //parseStep1 = mDoc.split("<cfgnam>");
        //parseStep2 = parseStep1[1].split("</cfgnam>");
        //m_strCfgName = parseStep2[0];        
        parseStep1 = mDoc.split("<cfgcpf>");
        parseStep2 = parseStep1[1].split("</cfgcpf>");        
        setCountsPerForce(Double.parseDouble(parseStep2[0]));
        parseStep1 = mDoc.split("<cfgcpt>");
        parseStep2 = parseStep1[1].split("</cfgcpt>");       
        setCountsPerTorque(Double.parseDouble(parseStep2[0]));
        parseStep1 = mDoc.split("<comrdtrate>");
        parseStep2 = parseStep1[1].split("</comrdtrate>");  
        m_iRDTSampleRate = (Integer.parseInt(parseStep2[0]));
        //parseStep1 = mDoc.split("<scfgfu>");
        //parseStep2 = parseStep1[1].split("</scfgfu>"); 
        //m_strForceUnits = parseStep2[0];
        //parseStep1 = mDoc.split("<scfgtu>");
        //parseStep2 = parseStep1[1].split("</scfgtu>"); 
        //m_strTorqueUnits = parseStep2[0];
        //parseStep1 = mDoc.split("<cfgmr>");
        //parseStep2 = parseStep1[1].split("</cfgmr>");
        //String[] asRatings = parseStep2[0].split(";");
          //for ( int i = 0; i < asRatings.length; i++ )
          //{
              //m_daftMaxes[i] = Double.parseDouble(asRatings[i]);
              //if ( 0 == m_daftMaxes[i])
              //{
                   //m_daftMaxes[i] = 32768; /* Default maximum rating. */
              //}
           //}
           //m_ftvc.setMaxForce(m_daftMaxes[2]); /* Use Fz rating as maximum. */
           //m_ftvc.setMaxTorque(m_daftMaxes[5]); /* use Tz rating as maximum. */
        }catch(Exception e)
        {
            return false;            
        }
        return true;
    }
	private void refreshDevices()
	{
		try
		{
			textarea_status.append("Refresh Clicked...\n");
			rtaDevices = null;
			EventQueue.invokeLater(new Runnable() {
				
				public void run() {
					
					try 
					{
						rtaDevices = DiscoveryClient.discoverRTADevicesLocalBroadcast(null);
						if(rtaDevices.length>0)
						{
							for(int i=0;i<rtaDevices.length;i++)
							{
								combobox_devicelist.addItem(rtaDevices[i].toString());
							}
							combobox_devicelist.setSelectedIndex(0);
							button_refresh.setEnabled(true);
							button_connect.setEnabled(true);
						}
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
						return;
					}
					finally
					{
						
					}
					if(rtaDevices.length == 0)
					{
						textarea_status.append("No Devices Found...\n");
						button_refresh.setEnabled(true);
						return;
					}
				}
			});
		}
		catch(Exception ex)
		{
			
		}
	}
}
