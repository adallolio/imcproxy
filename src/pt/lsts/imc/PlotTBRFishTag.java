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

public class PlotTBRFishTag {
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
    // String for influxdb.
	static String influxdb = "--input /home/autonaut/java_to_influx/tbrtag.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname tbrtag --fieldcolumns serial_no,unix_timestamp,millis,trans_protocol,trans_id,trans_data,snr,trans_freq,recv_mem_addr,lat,lon";
	
    static void plot(IMCMessage message){
        // Get date from server.
        String date_csv = format.format(new Date()); // get date from message: format.format(message.getDate());
        System.out.println("TBRFishTag record saved!");

        String serial_no = message.getString("serial_no");
        String unix_tmst = message.getString("unix_timestamp");
        String millis = message.getString("millis");
        millis = millis.substring(0, millis.length() - 3);
        String trans_protocol = message.getString("trans_protocol");
        String trans_id = message.getString("trans_id");
        String trans_data = message.getString("trans_data");
        String snr = message.getString("SNR");
        snr = snr.substring(0, snr.length() - 3);
        String trans_freq = message.getString("trans_freq");
        String recv_mem_addr = message.getString("recv_mem_addr");
        double lat = message.getDouble("lat");
        double lon = message.getDouble("lon");
        String lat_s = Double.toString(lat*(180.0/Math.PI));
        String lon_s = Double.toString(lon*(180.0/Math.PI));
        System.out.println("TBRFishTag CSV!");

        try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/tbrtag.csv"))) {

            StringBuilder sb = new StringBuilder();
            sb.append("timestamp");
            sb.append(',');
            sb.append("serial_no");
            sb.append(',');
            sb.append("unix_timestamp");
            sb.append(',');
            sb.append("millis");
            sb.append(',');
            sb.append("trans_protocol");
            sb.append(',');
            sb.append("trans_id");
            sb.append(',');
            sb.append("trans_data");
            sb.append(',');
            sb.append("snr");
            sb.append(',');
            sb.append("trans_freq");
            sb.append(',');
            sb.append("recv_mem_addr");
            sb.append(',');
            sb.append("lat");
            sb.append(',');
            sb.append("lon");
            sb.append('\n');

            //writer.write(sb.toString());

            sb.append(date_csv);
            sb.append(',');
            sb.append(serial_no);
            sb.append(',');
            sb.append(unix_tmst);
            sb.append(',');
            sb.append(millis);
            sb.append(',');
            sb.append(trans_protocol);
            sb.append(',');
            sb.append(trans_id);
            sb.append(',');
            sb.append(trans_data);
            sb.append(',');
            sb.append(snr);
            sb.append(',');
            sb.append(trans_freq);
            sb.append(',');
            sb.append(recv_mem_addr);
            sb.append(',');
            sb.append(lat_s);
            sb.append(',');
            sb.append(lon_s);
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
