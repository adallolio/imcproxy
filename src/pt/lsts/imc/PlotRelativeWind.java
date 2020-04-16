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
import org.knowm.xchart.internal.chartpart.Chart;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Color;

public class PlotRelativeWind {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	// Maximum record vector size - moving window.
	static Integer max_size_1000 = 1000;
	static Vector<String> angle = new Vector<String>(); 
	static Vector<String> speed = new Vector<String>();
	static Vector<String> times = new Vector<String>();
	static Date prev_date = null;
	static Date prev_date_plot = null;
	// Time units for saving a record and for generating a new plot.
	static String[] time_unit = {"seconds","minutes"};
	// Frequency for saving a record and for generating a new plot.
	static Integer[] frequency = {10,1};
	// String for influxdb.
	static String influxdb = "--input /home/autonaut/java_to_influx/relativewind.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname relativewind --fieldcolumns angle,speed";
    
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
		// Get date from server.
		String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());

		if(get_record)
		{
			System.out.println("RelativeWind record saved!");
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			
			values = message.getValues();
			String key;
			String value;
			ArrayList<String> ang_speed = new ArrayList<String>();

			for (Map.Entry<String, Object> entry : values.entrySet()) {
				System.out.println(entry.getKey() + ":" + entry.getValue().toString());
				key = entry.getKey();
				value = entry.getValue().toString();
				ang_speed.add(value);
			}
			if(angle.size() == max_size_1000)
			{
				for(int i=0;i<max_size_1000/10;i++)
				{
					angle.remove(i);
					speed.remove(i);
					times.remove(i);
				}
			}
			angle.add(ang_speed.get(0));
			speed.add(ang_speed.get(1));

			times.add(date_csv);

			System.out.println(angle.size() + " " + date_csv);
			//System.out.println(date);
			prev_date = curr_date;
		} else
			System.out.println("RelativeWind discarded!");

		plot = checkDates(curr_date, prev_date_plot, time_unit[1], frequency[1]);

		if(plot)
		{
			System.out.println("Generating CSV!");
			System.out.println(angle.size() + " " + speed.size() + times.size());

			try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/relativewind.csv"))) {

				StringBuilder sb = new StringBuilder();
				sb.append("timestamp");
				sb.append(',');
				sb.append("angle");
				sb.append(',');
				sb.append("speed");
				sb.append('\n');

				//writer.write(sb.toString());

				for(int i=0; i<angle.size(); i++)
				{
					sb.append(times.get(i));
					sb.append(',');
					sb.append(angle.get(i));
					sb.append(',');
					sb.append(speed.get(i));
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

			/*
			System.out.println("Generating plot!");
			System.out.println(angle.size() + " " + speed.size() + " " + times.size());

			int numCharts = 2;
			String[] titles = {"Relative Wind Angle","Relative Wind Speed"};
			String[] y_axes = {"degrees","m/s"};
			String[] legend = {"angle","speed"};

			Vector<Vector<Double>> total = new Vector<Vector<Double>>();
			total.add(angle);
			total.add(speed);

			List<Chart> charts = new ArrayList<Chart>();
			for (int i = 0; i < numCharts; i++) {
				XYChart chart = new XYChartBuilder().title(titles[i]+" - (last update "+date_title+" at "+date_x_axis+ ")").xAxisTitle("Time").yAxisTitle(y_axes[i]).width(800).height(500).build();
				chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
				chart.getStyler().setChartTitleVisible(true);
				chart.getStyler().setLegendPosition(LegendPosition.InsideSW);
				//chart.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
				chart.getStyler().setYAxisDecimalPattern("##.##");
				chart.getStyler().setPlotMargin(0);
				chart.getStyler().setPlotContentSize(.95);
				XYSeries series = chart.addSeries(legend[i], times, total.get(i));
				//series.setMarker(SeriesMarkers.NONE);
				charts.add(chart);
			}

			try {
				BitmapEncoder.saveBitmap(charts, 2, 1, "/var/www/dokuwiki/data/media/relativewind-rt", BitmapEncoder.BitmapFormat.PNG);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			prev_date_plot = curr_date;*/
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
