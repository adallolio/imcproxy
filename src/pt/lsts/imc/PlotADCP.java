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

public class PlotADCP {
	protected static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
    static Vector<String> depths_str = new Vector<String>();
    static Vector<String> vels_str = new Vector<String>();
    static Vector<String> dirs_str = new Vector<String>();
    // String for influxdb.
	static String influxdb = "--input /home/autonaut/java_to_influx/adcp.csv --user autonaut --password ntnu_autonaut --dbname AUTONAUT --metricname adcp --fieldcolumns lat,lon,depth,vel,dir";
	
    static void plot(IMCMessage message){
        // Get date from server.
        String date_csv = format.format(message.getDate()); // new Date() get date from message: format.format(message.getDate());
        System.out.println("ADCP record saved!");

        double lat = message.getDouble("lat");
        double lon = message.getDouble("lon");
        String lat_s = Double.toString(lat*(180.0/Math.PI));
        String lon_s = Double.toString(lon*(180.0/Math.PI));
        
        LinkedHashMap<String, String> depth_tuple = message.getTupleList("depth");
        LinkedHashMap<String, String> vel_tuple = message.getTupleList("vel");
        LinkedHashMap<String, String> dir_tuple = message.getTupleList("dir");

        String depth;
        Set<String> keys = depth_tuple.keySet();
        for(String k:keys){
            depth = depth_tuple.get(k); //.toString()
            depths_str.add(depth);
            System.out.println(k+" -- "+depth_tuple.get(k));
        }
        String vel;
        Set<String> keys1 = vel_tuple.keySet();
        for(String k:keys){
            vel = vel_tuple.get(k); //.toString()
            vels_str.add(vel);
            System.out.println(k+" -- "+vel_tuple.get(k));
        }
        String dir;
        Set<String> keys2 = dir_tuple.keySet();
        for(String k:keys){
            dir = dir_tuple.get(k); //.toString()
            dirs_str.add(dir);
            System.out.println(k+" -- "+dir_tuple.get(k));
        }
        
        /*Vector<String> depths = depth_tuple.get("depth");
        Vector<String> vels = vel_tuple.get("vel");
        Vector<String> dirs = dir_tuple.get("dir");

        try (PrintWriter writer = new PrintWriter(new File("/home/autonaut/java_to_influx/tbrsensor.csv"))) {

            StringBuilder sb = new StringBuilder();
            sb.append("timestamp");
            sb.append(',');
            sb.append("lat");
            sb.append(',');
            sb.append("lon");
            sb.append(',');
            sb.append("depth");
            sb.append(',');
            sb.append("vel");
            sb.append(',');
            sb.append("dir");
            sb.append('\n');

            //writer.write(sb.toString());

            for(int i=0;i<depths.size();i++)
			{
                sb.append(date_csv);
                sb.append(',');
                sb.append(lat);
                sb.append(',');
                sb.append(lon);
                sb.append(',');
                sb.append(depths(i));
                sb.append(',');
                sb.append(vels(i));
                sb.append(',');
                sb.append(dirs(i));
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
        }*/
    }
}
