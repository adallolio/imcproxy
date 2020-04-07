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

public class PlotEulerAngles {
    static SimpleDateFormat format_title = new SimpleDateFormat("dd-M-yyyy");
	static SimpleDateFormat format_x_axis = new SimpleDateFormat("HH:mm:ss");
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	// Maximum record vector size - moving window.
	static Integer max_size_100 = 100;
	static Vector<Double> phi = new Vector<Double>(); 
	static Vector<Double> theta = new Vector<Double>();
	static Vector<Double> psi = new Vector<Double>();
	static Vector<Double> psim = new Vector<Double>();
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

		//System.out.println(curr_date);
		//System.out.println(angvel_prev_date);
		
		get_record = checkDates(curr_date, prev_date, time_unit[0], frequency[0]);

		String date_title = format_title.format(message.getDate());
		String date_x_axis = format_x_axis.format(message.getDate());

		if(get_record)
		{
			System.out.println("EulerAngles record saved!");
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			
			values = message.getValues();
			boolean first_it = true;
			String key;
			String value;
			Double value_d;
			ArrayList<Double> phi_theta_psi_psim = new ArrayList<Double>();

			for (Map.Entry<String, Object> entry : values.entrySet()) {
				if(!first_it)
				{
					//System.out.println(entry.getKey() + ":" + entry.getValue().toString());
					key = entry.getKey();
					value = entry.getValue().toString();
					value_d = Double.valueOf(value);
					phi_theta_psi_psim.add(value_d);
				}
				first_it = false;
			}
			if(phi.size() == max_size_100)
			{
				for(int i=0;i<10;i++)
				{
					phi.remove(i);
					theta.remove(i);
					psi.remove(i);
					psim.remove(i);
				}
			}
			phi.add(phi_theta_psi_psim.get(0));
			theta.add(phi_theta_psi_psim.get(1));
			psi.add(phi_theta_psi_psim.get(2));
			psim.add(phi_theta_psi_psim.get(3));

			times.add(curr_date);

			System.out.println(phi.size() + " " + curr_date);
			//System.out.println(date);
			prev_date = curr_date;
		} else
			System.out.println("EulerAngles discarded!");

		plot = checkDates(curr_date, prev_date_plot, time_unit[1], frequency[1]);

		if(plot)
		{
			System.out.println("Generating plot!");
			System.out.println(phi.size() + " " + theta.size() + " " + psi.size() + " " + psim.size() + " " + times.size());
			// Create Chart
			XYChart chart = new XYChartBuilder().width(600).height(500).title("Euler Angles - "+date_title+ " (last update "+date_x_axis+")").xAxisTitle("Time").yAxisTitle("rad").build();

			// Customize Chart
			chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);
			chart.getStyler().setChartTitleVisible(true);
			chart.getStyler().setLegendPosition(LegendPosition.InsideSW);
			//chart.getStyler().setYAxisLabelAlignment(Styler.TextAlignment.Right);
			chart.getStyler().setYAxisDecimalPattern("##.##");
			chart.getStyler().setPlotMargin(0);
			chart.getStyler().setPlotContentSize(.95);

			chart.addSeries("phi", times, phi);
			chart.addSeries("theta", times, theta);
			chart.addSeries("psi", times, psi);
			chart.addSeries("psi_m", times, psim);
			
			// Save it
			try {
				BitmapEncoder.saveBitmap(chart, "/home/autonaut/AngularVelocity", BitmapFormat.PNG);
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