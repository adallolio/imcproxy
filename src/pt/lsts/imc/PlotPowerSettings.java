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

public class PlotPowerSettings {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	static Date prev_date = null;
	// Time units for saving a record and for generating a new plot.
	static String[] time_unit = {"seconds","minutes"};
	// Frequency for saving a record and for generating a new plot.
	static Integer[] frequency = {10,1};
	// String for influxdb.
	static String influxdb = "--input /home/autonaut/java_to_influx/powersettings.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname powersettings --fieldcolumns l2,l3,iridium,modem,pumps,vhf";
    
    static void plot(IMCMessage message){

		boolean plot = false;

		Date curr_date = message.getDate();
		if(prev_date == null)
			prev_date = curr_date;
		
		// Get date from server.
		String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());

		System.out.println("PowerSettings record saved!");
		
		String l2 = message.getString("l2"); 
		String l3 = message.getString("l3");
		String iridium = message.getString("iridium");
		String modem = message.getString("modem");
		String pumps = message.getString("pumps");
		String vhf = message.getString("vhf");

		plot = checkDates(curr_date, prev_date, time_unit[1], frequency[1]);

		if(plot)
		{
			System.out.println("Generating CSV!");

			try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/powersettings.csv"))) {

				StringBuilder sb = new StringBuilder();
				sb.append("timestamp");
				sb.append(',');
				sb.append("l2");
				sb.append(',');
				sb.append("l3");
				sb.append(',');
				sb.append("iridium");
				sb.append(',');
				sb.append("modem");
				sb.append(',');
				sb.append("pumps");
				sb.append(',');
				sb.append("vhf");
				sb.append('\n');

				//writer.write(sb.toString());

				sb.append(date_csv);
				sb.append(',');
				sb.append(l2);
				sb.append(',');
				sb.append(l3);
				sb.append(',');
				sb.append(iridium);
				sb.append(',');
				sb.append(modem);
				sb.append(',');
				sb.append(pumps);
				sb.append(',');
				sb.append(vhf);
				sb.append('\n');


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
            
            prev_date = curr_date;
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
