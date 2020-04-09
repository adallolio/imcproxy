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

public class PlotPowerSettings {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	// Maximum record vector size - moving window.
	static Integer max_size_100 = 100;
	static Vector<Integer> l2 = new Vector<Integer>(); 
	static Vector<Integer> l3 = new Vector<Integer>();
    static Vector<Integer> iridium = new Vector<Integer>();
    static Vector<Integer> modem = new Vector<Integer>();
    static Vector<Integer> pumps = new Vector<Integer>();
    static Vector<Integer> vhf = new Vector<Integer>();
	static Vector<Date> times = new Vector<Date>();
	static Date prev_date = null;
	static Date prev_date_plot = null;
	// Time units for saving a record and for generating a new plot.
	static String[] time_unit = {"seconds","minutes"};
	// Frequency for saving a record and for generating a new plot.
    static Integer[] frequency = {10,1};
    
    static void plot(IMCMessage message){

		boolean get_record = false;
		boolean plot = false;

		Date curr_date = message.getDate();
		if(prev_date == null)
			prev_date = curr_date;
		if(prev_date_plot == null)
			prev_date_plot = curr_date;
		
		get_record = checkDates(curr_date, prev_date, time_unit[0], frequency[0]);

		String date_title = format_title.format(message.getDate());
		String date_x_axis = format_x_axis.format(message.getDate());

		if(get_record)
		{
			System.out.println("PowerSettings record saved!");
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			
			values = message.getValues();
			boolean first_it = true;
			String key;
			String value;
            Integer value_i;
			ArrayList<Integer> settings = new ArrayList<Integer>();

			for (Map.Entry<String, Object> entry : values.entrySet()) {
				if(!first_it)
				{
					System.out.println(entry.getKey() + ":" + entry.getValue().toString());
					key = entry.getKey();
					value = entry.getValue().toString();
                    value_i = Integer.parseInt(value);
					settings.add(value_i);
				}
				first_it = false;
			}
			if(l2.size() == max_size_100)
			{
				for(int i=0;i<10;i++)
				{
					l2.remove(i);
					l3.remove(i);
                    iridium.remove(i);
                    modem.remove(i);
                    pumps.remove(i);
                    vhf.remove(i);
			times.remove(i);
				}
			}
			l2.add(settings.get(0));
			l3.add(settings.get(1));
            iridium.add(settings.get(2));
            modem.add(settings.get(3));
			pumps.add(settings.get(4));
			vhf.add(settings.get(5));

			times.add(curr_date);

			System.out.println(l2.size() + " " + curr_date);
			//System.out.println(date);
			prev_date = curr_date;
		} else
			System.out.println("PowerSettings discarded!");

		plot = checkDates(curr_date, prev_date_plot, time_unit[1], frequency[1]);

		if(plot)
		{
			System.out.println("Generating plot!");
			System.out.println(l2.size() + " " + l3.size() + " " + iridium.size() + " " + modem.size() + " " + pumps.size() + " " + vhf.size() + " " + times.size());
			
			
			// Save it
			/*try {
				BitmapEncoder.saveBitmap(chart, "/home/autonaut/PowerSettings", BitmapFormat.PNG);
			} catch(IOException e) {
			}*/
            
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
