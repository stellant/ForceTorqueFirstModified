package com.sensor.barcode;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;

import com.atiia.automation.sensors.NetFTRDTPacket;
import com.atiia.automation.sensors.NetFTSensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;

@Path("/readdata")
public class ReadData	 
{
	private DatagramSocket slowDataSocket = null;
	private NetFTRDTPacket packet = null;
	private String result = null;
	private int m_iRDTSampleRate; 
	private double[] m_daftCountsPerUnit = {1, 1, 1, 1, 1, 1};
	
	@GET
	@Produces(MediaType.TEXT_XML)
	public String readData(@QueryParam("ip") String ip, @QueryParam("port") String port)
	{
		try 
		{
			if(readConfigurationInfo(ip))
			{
				NetFTSensor sensor = new NetFTSensor(InetAddress.getByName(ip), Integer.parseInt(port));
				if(sensor!=null)
				{
					slowDataSocket = sensor.initLowSpeedData();
					if(slowDataSocket!=null)
					{
						packet = sensor.readLowSpeedData(slowDataSocket);
						//packet = new NetFTRDTPacket(35,28,45,78,98,96,65,65,47);
						if(packet != null)
						{
							result = getXML(packet);
						}
						else
						{
							result = getEmptyXML("No Data Found");
						}
					}
				}
			}
			else
			{
				throw new Exception();
			}
		}
		catch (UnknownHostException e) 
		{
			result = getEmptyXML(e.toString());
		}
		catch(IOException e)
		{
			result = getEmptyXML(e.toString());
		}
		catch(Exception e)
		{
			result = getEmptyXML("Cannot Read Sensor Configuration");
		}
		return result;
	}
	private String getXML(NetFTRDTPacket packet)
	{
		StringBuilder s = new StringBuilder();
		s.append("<?xml version = \"1.0\"?>");
		s.append("<ForceTorque>");
		s.append("<RDTSequence>"+packet.getRDTSequence()+"</RDTSequence>");
		s.append("<FTSequence>"+packet.getFTSequence()+"</FTSequence>");
		s.append("<Status>"+packet.getStatus()+"</Status>");
		s.append("<Fx>"+packet.getFx()/m_daftCountsPerUnit[0]+"</Fx>");
		s.append("<Fy>"+packet.getFy()/m_daftCountsPerUnit[1]+"</Fy>");
		s.append("<Fz>"+packet.getFz()/m_daftCountsPerUnit[2]+"</Fz>");
		s.append("<Tx>"+packet.getTx()/m_daftCountsPerUnit[3]+"</Tx>");
		s.append("<Ty>"+packet.getTy()/m_daftCountsPerUnit[4]+"</Ty>");
		s.append("<Tz>"+packet.getTz()/m_daftCountsPerUnit[5]+"</Tz>");
		s.append("<DateTime>"+new Date()+"</DateTime>");
		s.append("</ForceTorque>");
		return s.toString();
	}
	private String getEmptyXML(String status)
	{
		StringBuilder s = new StringBuilder();
		s.append("<?xml version = \"1.0\"?>");
		s.append("<ForceTorque>");
		s.append(status);
		s.append("</ForceTorque>");
		return s.toString();
	}
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
    private boolean readConfigurationInfo(String ipAddress)
    { 
        try
        {
	        String mDoc = readNetFTAPI(0,ipAddress);
	        int activeConfig = findActiveCFG(mDoc);
	        mDoc = readNetFTAPI(activeConfig,ipAddress);
	        String[] parseStep1 = mDoc.split("<cfgcalsel>");
	        String[] parseStep2 = parseStep1[1].split("</cfgcalsel>");
	        String mCal = readNetFTCalAPI(Integer.parseInt(parseStep2[0]),ipAddress);
	        mDoc = readNetFTAPI(activeConfig,ipAddress);
	        parseStep1 = mDoc.split("<cfgcpf>");
	        parseStep2 = parseStep1[1].split("</cfgcpf>");        
	        setCountsPerForce(Double.parseDouble(parseStep2[0]));
	        parseStep1 = mDoc.split("<cfgcpt>");
	        parseStep2 = parseStep1[1].split("</cfgcpt>");       
	        setCountsPerTorque(Double.parseDouble(parseStep2[0]));
	        parseStep1 = mDoc.split("<comrdtrate>");
	        parseStep2 = parseStep1[1].split("</comrdtrate>");  
	        m_iRDTSampleRate = (Integer.parseInt(parseStep2[0]));
        }
        catch(Exception e)
        {
            return false;            
        }
        return true;
    }
}
