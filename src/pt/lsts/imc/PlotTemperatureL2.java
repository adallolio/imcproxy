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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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

public class PlotTemperatureL2 {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	// Maximum record vector size - moving window.
	static Integer max_size_1000 = 1000;
	static Vector<String> temperaturel2 = new Vector<String>(); 
	static Vector<String> temperaturel3 = new Vector<String>(); 
	static Vector<String> times = new Vector<String>();
	static Vector<String> entities_vec = new Vector<String>();
	static Date prev_date_plot = null;
	// Time units for saving a record and for generating a new plot.
	static String time_unit = "minutes";
	// Frequency for saving a record and for generating a new plot.
	static Integer frequency = 1;
	// String for influxdb.
	static String influxdbl2 = "--input /home/autonaut/java_to_influx/temperaturel2.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname temperaturel2 --fieldcolumns value,entity";
	
    static void plot(IMCMessage message, LinkedHashMap<String, String> entities){
		String int_ent = entities.get("STB - ADIS");
		Integer int_ent_int = Integer.parseInt(int_ent);
		String ext_ent = entities.get("Weather Station Temperature");
		Integer ext_ent_int = Integer.parseInt(ext_ent);

		short entity = message.getSrcEnt();
		String entity_to_csv = "";
		if(entity == int_ent_int)
		{
			System.out.println("Internal emperature!");
			entity_to_csv = entity_to_csv+"int";
		} else if(entity == ext_ent_int)
		{
			System.out.println("Weather Station Temperature!");
			entity_to_csv = entity_to_csv+"ext";
		}

		boolean plot = false;

		Date curr_date = message.getDate();

		if(prev_date_plot == null)
			prev_date_plot = curr_date;

		// Get date from server.
		String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());

		//System.out.println("Temperature record saved!");
		
		if(temperaturel2.size() == max_size_1000)
		{
			for(int i=0;i<max_size_1000/10;i++)
			{
				temperaturel2.remove(i);
				times.remove(i);
				entities_vec.remove(i);
			}
		}

		String temp_string = message.getString("value");
		temperaturel2.add(temp_string.substring(0, temp_string.length() - 4));
		times.add(date_csv);
		entities_vec.add(entity_to_csv);

		System.out.println(temperaturel2.size() + " " + date_csv);

		plot = checkDates(curr_date, prev_date_plot, time_unit, frequency);

		if(plot)
		{
			System.out.println("L2 Temperature CSV!");
			System.out.println(temperaturel2.size() + " " + times.size());

			try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/temperaturel2.csv"))) {

				StringBuilder sb = new StringBuilder();
				sb.append("timestamp");
				sb.append(',');
				sb.append("value");
				sb.append(',');
				sb.append("entity");
				sb.append('\n');

				//writer.write(sb.toString());

				for(int i=0; i<temperaturel2.size(); i++)
				{
					sb.append(times.get(i));
					sb.append(',');
					sb.append(temperaturel2.get(i));
					sb.append(',');
					sb.append(entities_vec.get(i));
					sb.append('\n');
				}

				writer.write(sb.toString());

				System.out.println("done!");

				try {
					Process p = Runtime.getRuntime().exec("python /home/autonaut/java_to_influx/csv-to.py "+influxdbl2);
					System.out.println("Writing to AutoNaut InfluxDB!");
				} catch(IOException f) {
				}


			} catch (FileNotFoundException e) {
				System.out.println(e.getMessage());
			}
			prev_date_plot = curr_date;
			temperaturel2.clear();
			times.clear();
			entities_vec.clear();
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
