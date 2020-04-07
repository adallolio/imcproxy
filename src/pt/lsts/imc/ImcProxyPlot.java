package pt.lsts.imc;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import pt.lsts.imc.net.UDPTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;

import org.knowm.xchart.*;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYSeries.*;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Color;


@WebSocket
public class ImcProxyPlot extends ImcClientSocket {
	
	public List<String> toSave = Arrays.asList("Acceleration", "AirSaturation", "AngularVelocity", "Chlorophyll", "Conductivity", "CpuUsage", "Current", "Depth", "DesiredHeading",
												"DesiredSpeed", "DissolvedOrganicMatter", "DissolvedOxygen", "EstimatedFreq", "EstimatedState", "EulerAngles", "GpsFix", "Heartbeat",
												"Heave", "OpticalBackscatter", "Power", "PowerSettings", "Pressure", "RelativeWind", "AbsoluteWind", "Salinity", "SoundSpeed",
												"StorageUsage", "Temperature", "Voltage");
	public int AutoNautL2 = 34819;
	public int AutoNautL3 = 34820;
	public SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	public SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	// Maximum record vector size - moving window.
	public Integer max_size_100 = 100;


	


	

	public ImcProxyPlot(String serverHost, int serverPort) throws Exception {
		connect(new URI("ws://"+serverHost+":"+serverPort));
	}
	
	public static void console(String text) {
		System.out.println(format.format(new Date())+text);
	}
	
	@Override
	public void onMessage(IMCMessage message) {
		//System.out.println("got "+message.getAbbrev()+","+message.getClass().getSimpleName()+" from web"); //Leave this here to test if this works

		// Check if message is coming from AutoNaut L2(0x8803 = 34819) or L3(0x8804 = 34820).
		//console("The message is from " + message.getSrc());
		if(message.getSrc() == AutoNautL2 || message.getSrc() == AutoNautL3)
		{
			//console("The message is from AutoNaut!");
			saveMessage(message);
		}


		// Fred Comment:
		// This is where you filter messages either by msg.getMgid() only, or also imcid  - msg.getSrc() - returns imcid as int.
		// After filtering and finding messages that should be plot -> send message to correct plotter (e.g one plotter function for EstimatedState, one for Velocity etc.)
		
	}

	public void saveMessage(IMCMessage message) {

		// dump(OutputStream err)
		// Map<String, Object> getValues()
		// Check what message is arrived.

		String temp = message.getAbbrev();
		
		String entity = message.getEntityName();

		if(temp.equals("Acceleration"))
		{
			console("Acceleration received");
			PlotAcceleration.plot(message);
		} else if(temp.equals("AngularVelocity"))
		{
			//console("AngularVelocity received from " + entity);
			PlotAngularVelocity.plot(message);
		} else if(temp.equals("EulerAngles"))
		{
			//console("EulerAngles received");
			PlotEulerAngles.plot(message);
		} else if(temp.equals("Voltage"))
		{
			//console("Voltage received");
			PlotVoltage.plot(message);
		} else if(temp.equals("StorageUsage"))
		{
			//console("StorageUsage received");
			PlotStorageUsage.plot(message);
		} else if(temp.equals("RelativeWind"))
		{
			console("RelativeWind received");
			PlotRelativeWind.plot(message);
		}

	}
			
	public static void main(String[] args) throws Exception {
		String host = "zpserver.info";
		int port = 9090;
		
		if (args.length == 2) {
			try {
				port = Integer.parseInt(args[1]);
				host = args[0];
			}
			catch (Exception e) {
				System.out.println("Usage: ./imcplot <host> <port>");
				return;
			}
		}
		ImcProxyPlot.console("Connecting to server at "+host+":"+port);
		new ImcProxyPlot(host, port);		
	}	
}