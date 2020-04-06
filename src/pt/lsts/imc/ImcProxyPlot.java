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


@WebSocket
public class ImcProxyPlot extends ImcClientSocket {
	
	public List<String> toSave = Arrays.asList("Acceleration", "AirSaturation", "AngularVelocity", "Chlorophyll", "Conductivity", "CpuUsage", "Current", "Depth", "DesiredHeading",
												"DesiredSpeed", "DissolvedOrganicMatter", "DissolvedOxygen", "EstimatedFreq", "EstimatedState", "EulerAngles", "GpsFix", "Heartbeat",
												"Heave", "OpticalBackscatter", "Power", "PowerSettings", "Pressure", "RelativeWind", "AbsoluteWind", "Salinity", "SoundSpeed",
												"StorageUsage", "Temperature", "Voltage");
	public int AutoNautL2 = 34819;
	public int AutoNautL3 = 34820;
	public FileOutputStream os = null;
	public SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	public SimpleDateFormat format_x_axis = new SimpleDateFormat("hh.mm");
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");

	public Vector<Double> x = new Vector<Double>(); 
	public Vector<Double> y = new Vector<Double>();
	public Vector<Double> z = new Vector<Double>();
	public Vector<Double> times = new Vector<Double>();

	public Date prev_date = null;

	public ArrayList<ArrayList<Double>> acceleration = new ArrayList<ArrayList<Double>>();

	double[] xData = new double[] { 0.0, 1.0, 2.0 };
    double[] yData = new double[] { 2.0, 1.0, 0.0 };
	
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
		

		if(temp.equals("Acceleration"))
		{
			console("Acceleration received");
			plotAcceleration(message);
		}

		/*for(String s : toSave)
		{
			if(s.contains(message.getAbbrev()))
			{
				console("Save it to local storage");


				String data = "SUKA";					
				try {
					os = new FileOutputStream(new File("/home/autonaut/data.txt"), true);
					os.write(data.getBytes(), 0, data.length());
					os.write('\n');
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					try {
						os.flush();
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}*/
	}

	void plotAcceleration(IMCMessage message){

		Date curr_date = message.getDate();
		if(prev_date == null)
			prev_date = curr_date;

		System.out.println(curr_date);
		System.out.println(prev_date);
		
		boolean is_passed = checkDates(curr_date,prev_date);

		String date_title = format_title.format(message.getDate());
		String date_x_axis = format_x_axis.format(message.getDate());

		if(is_passed)
		{
			System.out.println("Time to save!");
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			
			values = message.getValues();
			boolean first_it = true;
			String key;
			String value;
			Double value_d;
			ArrayList<Double> xyz = new ArrayList<Double>();

			for (Map.Entry<String, Object> entry : values.entrySet()) {
				if(!first_it)
				{
					//System.out.println(entry.getKey() + ":" + entry.getValue().toString());
					key = entry.getKey();
					value = entry.getValue().toString();
					value_d = Double.valueOf(value);
					xyz.add(value_d);
				}
				first_it = false;
			}
			x.add(xyz.get(0));
			y.add(xyz.get(1));
			z.add(xyz.get(2));

			times.add(Double.parseDouble(date_x_axis));

			System.out.println(x.size() + " " + Double.parseDouble(date_x_axis));
			//System.out.println(date);
			prev_date = curr_date;
		} else
			System.out.println("Acceleration discarded!");

		if(x.size()==20)
		{
			System.out.println(x.size() + " " + y.size() + " " + z.size() + " " + times.size());
			// Create Chart
			XYChart chart = new XYChartBuilder().width(600).height(500).title("Acceleration - "+date_title).xAxisTitle("Time").yAxisTitle("A").build();

			// Customize Chart
			chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
			chart.getStyler().setChartTitleVisible(true);
			chart.getStyler().setLegendPosition(LegendPosition.InsideSW);
			//chart.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
			chart.getStyler().setYAxisDecimalPattern("##.##");
			chart.getStyler().setPlotMargin(0);
			chart.getStyler().setPlotContentSize(.95);

			chart.addSeries("x", times, x);
			chart.addSeries("y", times, y);
			chart.addSeries("z", times, z);
			
			// Save it
			try {
				BitmapEncoder.saveBitmap(chart, "/home/autonaut/Acceleration", BitmapFormat.PNG);
			} catch(IOException e) {
			}
		}


		

		/*
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			if(!first_it)
			{
				//System.out.println(entry.getKey() + ":" + entry.getValue().toString());
				key = entry.getKey();
				value = entry.getValue().toString();
				// create custom string for each map element
				toFile = key + "=" + value;
				System.out.println(toFile);
			}
			first_it = false;
		}
		System.out.println(date);
		*/

	}

	boolean checkDates(Date current, Date previous) {
		long diff = current.getTime() - previous.getTime();
		if(diff >= 0)
		{
			long diffSeconds = diff / 1000 % 60;  
			long diffMinutes = diff / (60 * 1000) % 60;
			long diffHours = diff / (60 * 60 * 1000);
			//System.out.println(diffSeconds + " " + diffMinutes + " " + diffHours + "\n");
			
			if(diffSeconds >= 5) //diffMinutes >= 1
				return true;
			else
				return false;

		} else
		{
			System.out.println("Date difference is negative!!");
			return false;
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