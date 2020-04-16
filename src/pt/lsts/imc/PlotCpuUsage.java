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

import org.knowm.xchart.*;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYSeries.*;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Color;

public class PlotCpuUsage {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	// Maximum record vector size - moving window.
	static Integer max_size_100 = 100;
	static String cpu; 
	static Date prev_date = null;
	static Date prev_date_plot_l2 = null;
	static Date prev_date_plot_l3 = null;
	static int AutoNautL2 = 34819;
	static int AutoNautL3 = 34820;
	// Time units for saving a record and for generating a new plot.
	static String time_unit = "minutes";
	// Frequency for saving a record and for generating a new plot.
	static Integer frequency = 1;
	// String for influxdb.
	static String influxdb = "--input /home/autonaut/java_to_influx/cpuusage.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname cpuusage --fieldcolumns unit,value";

    static void plot(IMCMessage message){

		boolean plot = false;
		Date curr_date = message.getDate();

		if(message.getSrc() == AutoNautL2)
		{
			if(prev_date_plot_l2 == null)
				prev_date_plot_l2 = curr_date;
			
			plot = checkDates(curr_date, prev_date_plot_l2, time_unit, frequency);

			if(plot)
			{
				String date_title = format_title.format(message.getDate());
				// Get date from server.
				String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());
				
				System.out.println("CpuUsage record saved!");
				Map<String, Object> values = new LinkedHashMap<String, Object>();
				
				values = message.getValues();
				String key;
				String value;

				for (Map.Entry<String, Object> entry : values.entrySet()) {
					System.out.println(entry.getKey() + ":" + entry.getValue().toString());
					key = entry.getKey();
					value = entry.getValue().toString();
					cpu = value;
				}

				System.out.println("Generating CSV!");

				try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/cpuusage.csv"))) {

					StringBuilder sb = new StringBuilder();
					sb.append("timestamp");
					sb.append(',');
					sb.append("unit");
					sb.append(',');
					sb.append("value");
					sb.append('\n');

					sb.append(date_csv);
					sb.append(',');
					sb.append("l2");
					sb.append(',');
					sb.append(cpu);
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
				prev_date_plot_l2 = curr_date;
				
				/*
				System.out.println("Generating plot!");
				// Create Chart
				PieChart chart = new PieChartBuilder().width(800).height(600).title("Level 2 Cpu Usage - "+date_title+ " (last update "+date_x_axis+")").build();

				// Customize Chart
				Color[] sliceColors = new Color[] { new Color(224, 68, 14), new Color(246, 199, 182) };
				chart.getStyler().setSeriesColors(sliceColors);
				chart.getStyler().setLegendPosition(LegendPosition.InsideSW);

				chart.addSeries("Used", cpu);
				chart.addSeries("Available", 100.0-cpu);
				
				// Save it
				try {
					BitmapEncoder.saveBitmap(chart, "/var/www/dokuwiki/data/media/l2cpuusage-rt", BitmapFormat.PNG);
				} catch(IOException e) {
				}
				prev_date_plot_l2 = curr_date;
				*/
			}
		} else if(message.getSrc() == AutoNautL3)
		{
			if(prev_date_plot_l3 == null)
				prev_date_plot_l3 = curr_date;

			plot = checkDates(curr_date, prev_date_plot_l3, time_unit, frequency);

			if(plot)
			{

				String date_title = format_title.format(message.getDate());
				// Get date from server.
				String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());
				
				System.out.println("CpuUsage from L3 saved!");
				Map<String, Object> values = new LinkedHashMap<String, Object>();
				
				values = message.getValues();
				String key;
				String value;

				for (Map.Entry<String, Object> entry : values.entrySet()) {
					System.out.println(entry.getKey() + ":" + entry.getValue().toString());
					key = entry.getKey();
					value = entry.getValue().toString();
					cpu = value;
				}

				System.out.println("Generating CSV!");

				try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/airsaturation.csv"))) {

					StringBuilder sb = new StringBuilder();
					sb.append("timestamp");
					sb.append(',');
					sb.append("unit");
					sb.append(',');
					sb.append("value");
					sb.append('\n');

					sb.append(date_csv);
					sb.append(',');
					sb.append("l3");
					sb.append(',');
					sb.append(cpu);
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
				prev_date_plot_l3 = curr_date;

				/*
				System.out.println("Generating plot!");
				// Create Chart
				PieChart chart = new PieChartBuilder().width(800).height(600).title("Level 3 Cpu Usage - "+date_title+ " (last update "+date_x_axis+")").build();

				// Customize Chart
				Color[] sliceColors = new Color[] { new Color(224, 68, 14), new Color(246, 199, 182) };
				chart.getStyler().setSeriesColors(sliceColors);
				chart.getStyler().setLegendPosition(LegendPosition.InsideSW);
				//chart.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);

				chart.addSeries("Used", cpu);
				chart.addSeries("Available", 100.0-cpu);
				
				// Save it
				try {
					BitmapEncoder.saveBitmap(chart, "/var/www/dokuwiki/data/media/l3cpuusage-rt", BitmapFormat.PNG);
				} catch(IOException e) {
				}
				prev_date_plot_l3 = curr_date;
				*/
			}
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