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

public class PlotStorageUsage {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	// Maximum record vector size - moving window.
	static Integer max_size_100 = 100;
    // Storage Usage.
	static Double available;
	static Double used; 
	static Date time = new Date();
	static Date prev_date_plot = null;
	// Time units for saving a record and for generating a new plot.
	static String[] time_unit = {"seconds","minutes"};
	// Frequency for saving a record and for generating a new plot.
    static Integer[] frequency = {10,1};
    
    static void plot(IMCMessage message){

		boolean plot = false;
		Date curr_date = message.getDate();

		if(prev_date_plot == null)
			prev_date_plot = curr_date;

		plot = checkDates(curr_date, prev_date_plot, time_unit[1], frequency[1]);

		if(plot)
		{
			String date_title = format_title.format(message.getDate());
			String date_x_axis = format_x_axis.format(message.getDate());
			System.out.println("StorageUsage record saved!");
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			
			values = message.getValues();
			//boolean first_it = true;
			String key;
			String value;
			Double value_d;
			ArrayList<Double> storage = new ArrayList<Double>();

			for (Map.Entry<String, Object> entry : values.entrySet()) {
				System.out.println(entry.getKey() + ":" + entry.getValue().toString());
				key = entry.getKey();
				value = entry.getValue().toString();
				value_d = Double.valueOf(value);
				storage.add(value_d);
			}

			available = Math.round(storage.get(0)/1000 * 100.0) / 100.0;
			used = storage.get(1);

			time = curr_date;

			System.out.println(available+"\n");

			System.out.println("Generating plot!");
			// Create Chart
			PieChart chart = new PieChartBuilder().width(800).height(600).title("Disc Storage - "+date_title+ " (last update "+date_x_axis+")").build();

			// Customize Chart
			Color[] sliceColors = new Color[] { new Color(224, 68, 14), new Color(246, 199, 182) };
    		chart.getStyler().setSeriesColors(sliceColors);

			chart.addSeries("Used Storage", used);
			chart.addSeries("Available ("+available+"Gb)", 100.0-used);
			
			// Save it
			try {
				BitmapEncoder.saveBitmap(chart, "/home/autonaut/StorageUsage", BitmapFormat.PNG);
			} catch(IOException e) {
			}
			prev_date_plot = curr_date;
		}
    }
    
    static boolean checkDates(Date current, Date previous, String unit, Integer frequency) {
		long diff = current.getTime() - previous.getTime();
		if(diff >= 0)
		{
			long diffSeconds = diff / 1000 % 60;  
			long diffMinutes = diff / (60 * 1000) % 60;
			long diffHours = diff / (60 * 60 * 1000);
			//System.out.println(diffSeconds + " " + diffMinutes + " " + diffHours + "\n");

			if(unit.equals("hours") && diffHours == frequency)
				return true;
			else if(unit.equals("minutes") && diffMinutes == frequency)
				return true;
			else if(unit.equals("seconds") && diffSeconds == frequency)
				return true;
			else
				return false;

		} else
		{
			System.out.println("Date difference is negative!!");
			return false;
		}
	}
}