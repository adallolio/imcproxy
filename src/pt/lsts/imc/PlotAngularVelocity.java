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

public class PlotAngularVelocity {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	// Maximum record vector size - moving window.
	static Integer max_size_1000 = 1000;

	// Angular Velocity.
	static Vector<String> x = new Vector<String>(); 
	static Vector<String> y = new Vector<String>();
	static Vector<String> z = new Vector<String>();
	static Vector<String> times = new Vector<String>();
	static Vector<String> entities_vec = new Vector<String>();
	static Date prev_date = null;
	static Date prev_date_plot = null;
	// Time units for saving a record and for generating a new plot.
	static String[] time_unit = {"seconds","minutes"};
	// Frequency for saving a record and for generating a new plot.
	static Integer[] frequency = {10,1};
	// String for influxdb.
	static String influxdb = "--input /home/autonaut/java_to_influx/angularvelocity.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname angularvelocity --fieldcolumns x,y,z,entity";
    
    static void plot(IMCMessage message, LinkedHashMap<String, String> entities){
		
		String gps_ent = entities.get("GPS");
		Integer gps_ent_int = Integer.parseInt(gps_ent);
		String adis_ent = entities.get("STB - ADIS");
		Integer adis_ent_int = Integer.parseInt(adis_ent);
		short entity = message.getSrcEnt();
		String entity_to_csv = "";
		if(entity == gps_ent_int)
		{
			System.out.println("ANGULAR VELOCITY FROM GPS!");
			entity_to_csv = entity_to_csv+"GPS";
		}
		else if(entity == adis_ent_int)
		{
			System.out.println("ANGULAR VELOCITY FROM IMU!");
			entity_to_csv = entity_to_csv+"IMU";
		}
			

		boolean get_record = false;
		boolean plot = false;

		Date curr_date = message.getDate();
		if(prev_date == null)
			prev_date = curr_date;
		if(prev_date_plot == null)
			prev_date_plot = curr_date;
		
		get_record = checkDates(curr_date, prev_date, time_unit[0], frequency[0]);

		// Get date from server.
		String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());

		if(get_record)
		{
			System.out.println("AngularVelocity record saved!");
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			
			values = message.getValues();
			boolean first_it = true;
			String key;
			String value;
			ArrayList<String> xyz = new ArrayList<String>();

			for (Map.Entry<String, Object> entry : values.entrySet()) {
				if(!first_it)
				{
					//System.out.println(entry.getKey() + ":" + entry.getValue().toString());
					key = entry.getKey();
					value = entry.getValue().toString();
					xyz.add(value);
				}
				first_it = false;
			}
			if(x.size() == max_size_1000)
			{
				for(int i=0;i<max_size_1000/10;i++)
				{
					x.remove(i);
					y.remove(i);
					y.remove(i);
					times.remove(i);
					entities_vec.remove(i);
				}
			}
			x.add(xyz.get(0));
			y.add(xyz.get(1));
			z.add(xyz.get(2));
			entities_vec.add(entity_to_csv);

			times.add(date_csv);

			System.out.println(x.size() + " " + date_csv);
			//System.out.println(date);
			prev_date = curr_date;
		} else
			System.out.println("AngularVelocity discarded!");

		plot = checkDates(curr_date, prev_date_plot, time_unit[1], frequency[1]);

		if(plot)
		{
			System.out.println("Generating CSV!");
			System.out.println(x.size() + " " + y.size() + " " + z.size() + " " + times.size());

			try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/angularvelocity.csv"))) {

				StringBuilder sb = new StringBuilder();
				sb.append("timestamp");
				sb.append(',');
				sb.append("x");
				sb.append(',');
				sb.append("y");
				sb.append(',');
				sb.append("z");
				sb.append(',');
				sb.append("entity");
				sb.append('\n');

				//writer.write(sb.toString());

				for(int i=0; i<x.size(); i++)
				{
					sb.append(times.get(i));
					sb.append(',');
					sb.append(x.get(i));
					sb.append(',');
					sb.append(y.get(i));
					sb.append(',');
					sb.append(z.get(i));
					sb.append(',');
					sb.append(entities_vec.get(i));
					sb.append('\n');
				}

				writer.write(sb.toString());

				System.out.println("done!");

				try {
					Process p = Runtime.getRuntime().exec("python /home/autonaut/java_to_influx/csv-to.py "+influxdb);
					System.out.println("Writing to AutoNaut InfluxDB!");
				} catch(IOException f) {
				}


			  } catch (FileNotFoundException e) {
				System.out.println(e.getMessage());
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
