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

public class PlotGpsFix {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	// Maximum record vector size - moving window.
	static Integer max_size_1000 = 1000;
    // Storage Usage.
    static Vector<String> cogs = new Vector<String>();
    static Vector<String> sogs = new Vector<String>();
    static Vector<String> times = new Vector<String>();
	static Date prev_date_plot = null;
	static int AutoNautL2 = 34819;
	static int AutoNautL3 = 34820;
	// Time units for saving a record and for generating a new plot.
	static String[] time_unit = {"seconds","minutes"};
	// Frequency for saving a record and for generating a new plot.
	static Integer[] frequency = {10,1};
	// String for influxdb.
    static String influxdb_loc = "--input /home/autonaut/java_to_influx/gpsfixloc.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname gpsfixloc --fieldcolumns lat,lon,sat";
    static String influxdb_cog = "--input /home/autonaut/java_to_influx/gpsfixcog.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname gpsfixcogsog --fieldcolumns cog,sog";
    
    static void plot(IMCMessage message){

		boolean plot = false;
        Date curr_date = message.getDate();
        // Get date from server.
        String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());

        if(prev_date_plot == null)
            prev_date_plot = curr_date;

        double cog = message.getDouble("cog");
        double sog = message.getDouble("sog");
        String cog_s = Double.toString(cog*(180.0/Math.PI));
        String sog_s = Double.toString(sog);
        cogs.add(cog_s);
        sogs.add(sog_s);

        double lat = message.getDouble("lat");
        double lon = message.getDouble("lon");
        Integer sat = message.getInteger("satellites");
        String lat_s = Double.toString(lat*(180.0/Math.PI));
        String lon_s = Double.toString(lon*(180.0/Math.PI));
        String sat_s = Integer.toString(sat);

        times.add(date_csv);

        plot = checkDates(curr_date, prev_date_plot, time_unit[1], frequency[1]);

        if(plot)
        {
            System.out.println("GpsFix from L2 saved!");

            if(cogs.size() == max_size_1000)
		    {
                for(int i=0;i<max_size_1000/10;i++)
                {
                    cogs.remove(i);
                    sogs.remove(i);
                    times.remove(i);
                }
            }

            System.out.println("Generating CSV!");

            if(sat != 0)
            {
                // Insert location table.
                try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/gpsfixloc.csv"))) {

                    StringBuilder sb = new StringBuilder();
                    sb.append("timestamp");
                    sb.append(',');
                    sb.append("lat");
                    sb.append(',');
                    sb.append("lon");
                    sb.append(',');
                    sb.append("sat");
                    sb.append('\n');

                    sb.append(date_csv);
                    sb.append(',');
                    sb.append(lat_s);
                    sb.append(',');
                    sb.append(lon_s);
                    sb.append(',');
                    sb.append(sat_s);
                    sb.append('\n');

                    writer.write(sb.toString());

                    System.out.println("done!");

                    try {
                        Process p = Runtime.getRuntime().exec("python /home/autonaut/java_to_influx/csv-to.py "+influxdb_loc);
                        System.out.println("Writing to AutoNaut InfluxDB!");
                    } catch(IOException f) {
                    }


                } catch (FileNotFoundException e) {
                    System.out.println(e.getMessage());
                }
            }

            System.out.println("And again!");

            // Insert cog-sog table.
            try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/gpsfixcog.csv"))) {

                StringBuilder sb = new StringBuilder();
                sb.append("timestamp");
                sb.append(',');
                sb.append("cog");
                sb.append(',');
                sb.append("sog");
                sb.append('\n');

                for(int i=0; i<sogs.size(); i++)
                {
                    sb.append(times.get(i));
                    sb.append(',');
                    sb.append(cogs.get(i));
                    sb.append(',');
                    sb.append(sogs.get(i));
                    sb.append('\n');
                }

                writer.write(sb.toString());

                System.out.println("done!");

                try {
                    Process p = Runtime.getRuntime().exec("python /home/autonaut/java_to_influx/csv-to.py "+influxdb_cog);
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
