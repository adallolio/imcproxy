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

import java.io.IOException;
import java.util.ArrayList;
import java.awt.Color;

import pt.lsts.imc.EntityList;
import pt.lsts.imc.EntityList.OP;
import pt.lsts.imc.IMCAddressResolver;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;

import java.net.URL;

@WebSocket
public class ImcProxyPlot extends ImcClientSocket {
	
	public List<String> toSave = Arrays.asList("Acceleration", "AirSaturation", "AngularVelocity", "Chlorophyll", "Conductivity", "CpuUsage", "Current", "Depth", "DesiredHeading",
												"DesiredSpeed", "DissolvedOrganicMatter", "DissolvedOxygen", "EstimatedFreq", "EstimatedState", "EulerAngles", "GpsFix", "Heartbeat",
												"Heave", "OpticalBackscatter", "Power", "PowerSettings", "Pressure", "RelativeWind", "AbsoluteWind", "Salinity", "SoundSpeed",
												"StorageUsage", "Temperature", "Voltage");
	public int AutoNautL2 = 34819;
	public int AutoNautL3 = 34820;
	public LinkedHashMap<String, String> AutoNautL2_entities = new LinkedHashMap<String, String>();
	public LinkedHashMap<String, String> AutoNautL3_entities = new LinkedHashMap<String, String>();
	public boolean L2entities_arrived = false;
	public boolean L3entities_arrived = false;

	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");

	public ImcProxyPlot(String serverHost, int serverPort) throws Exception {
		connect(new URI("ws://"+serverHost+":"+serverPort));
	}
	
	public static void console(String text) {
		System.out.println(format.format(new Date())+text);
	}
	
	@Override
	public void onMessage(IMCMessage message) {
		//System.out.println("got "+message.getAbbrev()+","+message.getClass().getSimpleName()+" from web"); //Leave this here to test if this works

		if(message.getAbbrev().equals("EntityList") && message.getSrc() == AutoNautL2 && !L2entities_arrived)
		{
			console("L2 ENTITY LIST RECEIVED");
			if(message.getInteger("op") == 0) // OP.REPORT
			{
				AutoNautL2_entities = message.getTupleList("list");
				L2entities_arrived = true;
			}
		}
		if(message.getAbbrev().equals("EntityList") && message.getSrc() == AutoNautL3 && !L3entities_arrived)
		{
			console("L3 ENTITY LIST RECEIVED!");
			if(message.getInteger("op") == 0) // OP.REPORT
			{
				AutoNautL3_entities = message.getTupleList("list");
				L3entities_arrived = true;
			}
		}

		// Check if message is coming from AutoNaut L2(0x8803 = 34819) or L3(0x8804 = 34820).
		//console("The message is from " + message.getSrc());
		if(message.getSrc() == AutoNautL2 || message.getSrc() == AutoNautL3)
		{
			//console("The message is from AutoNaut!");
			saveMessage(message);
		}
	}

	public void saveMessage(IMCMessage message) {
		String temp = message.getAbbrev();

		if(temp.equals("Acceleration"))
		{
			console("Acceleration received");
			PlotAcceleration.plot(message);
		} else if(temp.equals("AngularVelocity") && L2entities_arrived)
		{
			console("AngularVelocity received");
			PlotAngularVelocity.plot(message,AutoNautL2_entities);
		} else if(temp.equals("EulerAngles"))
		{
			console("EulerAngles received");
			PlotEulerAngles.plot(message);
		} else if(temp.equals("Voltage"))
		{
			console("Voltage received");
			PlotVoltage.plot(message);
		} else if(temp.equals("StorageUsage"))
		{
			console("StorageUsage received");
			PlotStorageUsage.plot(message);
		} else if(temp.equals("RelativeWind"))
		{
			console("RelativeWind received");
			PlotRelativeWind.plot(message);
		} else if(temp.equals("AirSaturation"))
		{
			console("AirSaturation received");
			PlotAirSaturation.plot(message);
		} else if(temp.equals("DissolvedOxygen"))
		{
			console("DissolvedOxygen received");
			PlotDissolvedOxygen.plot(message);
		} else if(temp.equals("Chlorophyll"))
		{
			console("Chlorophyll received");
			PlotChlorophyll.plot(message);
		} else if(temp.equals("Conductivity"))
		{
			console("Conductivity received");
			PlotConductivity.plot(message);
		} else if(temp.equals("CpuUsage"))
		{
			console("CpuUsage received");
			PlotCpuUsage.plot(message);
		} else if(temp.equals("Current") && L2entities_arrived)
		{
			console("Current received");
			PlotCurrent.plot(message,AutoNautL2_entities);
		} else if(temp.equals("Depth"))
		{
			console("Depth received");
			PlotDepth.plot(message);
		} else if(temp.equals("DissolvedOrganicMatter"))
		{
			console("DissolvedOrganicMatter received");
			PlotDissolvedOrganicMatter.plot(message);
		} else if(temp.equals("EstimatedFreq"))
		{
			console("EstimatedFreq received");
			PlotEstimatedFreq.plot(message);
		} else if(temp.equals("Heave"))
		{
			console("Heave received");
			PlotHeave.plot(message);
		} else if(temp.equals("OpticalBackscatter"))
		{
			console("OpticalBackscatter received");
			PlotOpticalBackscatter.plot(message);
		} else if(temp.equals("Power") && L2entities_arrived)
		{
			console("Power received");
			PlotPower.plot(message,AutoNautL2_entities);
		} else if(temp.equals("PowerSettings"))
		{
			console("PowerSettings received");
			PlotPowerSettings.plot(message);
		} else if(temp.equals("Pressure"))
		{
			console("Pressure received");
			PlotPressure.plot(message);
		} else if(temp.equals("AbsoluteWind"))
		{
			console("AbsoluteWind received");
			PlotAbsoluteWind.plot(message);
		} else if(temp.equals("Salinity"))
		{
			console("Salinity received");
			PlotSalinity.plot(message);
		} else if(temp.equals("SoundSpeed"))
		{
			console("SoundSpeed received");
			PlotSoundSpeed.plot(message);
		} else if(temp.equals("Temperature") && message.getSrc() == AutoNautL2 && L2entities_arrived)
		{
			console("Temperature L2 received");
			PlotTemperature.plot(message,AutoNautL2_entities);
		} else if(temp.equals("Temperature") && message.getSrc() == AutoNautL3 && L3entities_arrived)
		{
			console("Temperature L3 received");
			PlotTemperature.plot(message,AutoNautL3_entities);
		} else if(temp.equals("GpsFix") && message.getSrc() == AutoNautL2)
		{
			console("GpsFix received");
			PlotGpsFix.plot(message);
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
