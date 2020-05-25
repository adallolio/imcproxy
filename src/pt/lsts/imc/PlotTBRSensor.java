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

public class PlotTBRSensor {
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
    // String for influxdb.
	static String influxdb = "--input /home/autonaut/java_to_influx/tbrsensor.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname tbrsensor --fieldcolumns serial_no,unix_timestamp,temp,avg_noise,peak_noise,rcv_freq,rcv_mem_addr";
	
    static void plot(IMCMessage message){
        // Get date from server.
        String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());
        System.out.println("TBRSensor record saved!");

        String serial_no = message.getString("serial_no");
        String unix_tmst = message.getString("unix_timestamp");
        String temp = message.getString("temperature");
        String avg_noise_lev = message.getString("avg_noise_level");
        String peak_noise_lev = message.getString("peak_noise_level");
        String rcv_freq = message.getString("recv_listen_freq");
        String mem = message.getString("recv_mem_addr");
        System.out.println("TBRSensor CSV!");

        try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/tbrsensor.csv"))) {

            StringBuilder sb = new StringBuilder();
            sb.append("timestamp");
            sb.append(',');
            sb.append("serial_no");
            sb.append(',');
            sb.append("unix_timestamp");
            sb.append(',');
            sb.append("temp");
            sb.append(',');
            sb.append("avg_noise");
            sb.append(',');
            sb.append("peak_noise");
            sb.append(',');
            sb.append("rcv_freq");
            sb.append(',');
            sb.append("rcv_mem_addr");
            sb.append('\n');

            //writer.write(sb.toString());

            sb.append(date_csv);
            sb.append(',');
            sb.append(serial_no);
            sb.append(',');
            sb.append(unix_tmst);
            sb.append(',');
            sb.append(temp);
            sb.append(',');
            sb.append(avg_noise_lev);
            sb.append(',');
            sb.append(peak_noise_lev);
            sb.append(',');
            sb.append(rcv_freq);
            sb.append(',');
            sb.append(mem);
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
    }
}
