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
import org.knowm.xchart.internal.chartpart.Chart;
import org.knowm.xchart.style.Styler.LegendPosition;
import org.knowm.xchart.style.markers.SeriesMarkers;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Color;

public class PlotAcceleration {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	// Maximum record vector size - moving window.
	static Integer max_size_1000 = 1000;
	static Vector<Double> x = new Vector<Double>(); 
	static Vector<Double> y = new Vector<Double>();
	static Vector<Double> z = new Vector<Double>();
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
			System.out.println("Acceleration record saved!");
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
			if(x.size() == max_size_1000)
			{
				for(int i=0;i<max_size_1000/10;i++)
				{
					x.remove(i);
					y.remove(i);
					y.remove(i);
					times.remove(i);
				}
			}
			x.add(xyz.get(0));
			y.add(xyz.get(1));
			z.add(xyz.get(2));

			times.add(curr_date);

			System.out.println(x.size() + " " + curr_date);
			//System.out.println(date);
			prev_date = curr_date;
		} else
			System.out.println("Acceleration discarded!");

		plot = checkDates(curr_date, prev_date_plot, time_unit[1], frequency[1]);

		if(plot)
		{
			System.out.println("Generating plot!");
			System.out.println(x.size() + " " + y.size() + " " + z.size() + " " + times.size());

			int numCharts = 3;
			String[] titles = {"Acceleration - x axis","Acceleration - y axis", "Acceleration - z axis"};
			String y_axes = "m/s/s";
			String legend = "acceleration";

			Vector<Vector<Double>> total = new Vector<Vector<Double>>();
			total.add(x);
			total.add(y);
			total.add(z);
			
			List<Chart> charts = new ArrayList<Chart>();
			for (int i = 0; i < numCharts; i++) {
				XYChart chart = new XYChartBuilder().title(titles[i]+" (last update "+date_title+" at "+date_x_axis+ ")").xAxisTitle("Time").yAxisTitle(y_axes).width(800).height(500).build();
				chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
				chart.getStyler().setChartTitleVisible(true);
				chart.getStyler().setLegendPosition(LegendPosition.InsideSW);
				//chart.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
				chart.getStyler().setYAxisDecimalPattern("##.##");
				chart.getStyler().setPlotMargin(0);
				chart.getStyler().setPlotContentSize(.95);
				XYSeries series = chart.addSeries(legend, times, total.get(i));
				//series.setMarker(SeriesMarkers.NONE);
				charts.add(chart);
			}

			try {
				BitmapEncoder.saveBitmap(charts, 3, 1, "/var/www/dokuwiki/data/media/acceleration-rt", BitmapEncoder.BitmapFormat.PNG);
			} catch (IOException e) {
				e.printStackTrace();
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
