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

import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import pt.lsts.imc.net.UDPTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;


@WebSocket
public class ImcProxyClient extends ImcClientSocket {
	
	protected LinkedHashMap<Integer, Integer> imcToPort = new LinkedHashMap<>();
	protected LinkedHashMap<Integer, Integer> portToImc = new LinkedHashMap<>();
	protected LinkedHashMap<Integer, InetSocketAddress> localImcHosts = new LinkedHashMap<>();
	protected LinkedHashMap<Integer, UDPTransport> remoteImcHosts = new LinkedHashMap<>();
	protected UDPTransport discovery;
	protected static SimpleDateFormat format = new SimpleDateFormat("[YYYY-MM-dd, HH:mm:ss] ");
	public UDPTransport createImcUdpHost(int startPort) {
		int port = startPort;
		UDPTransport transport; 
        while (true) {
            transport = new UDPTransport(port, 1);
            if (transport.isOnBindError())
                port++;                            
            else
                break;
            
        }
        
        final int bind_port = port;
        
        transport.addMessageListener(new MessageListener<MessageInfo, IMCMessage>() {
        	@Override
        	public void onMessage(MessageInfo info, IMCMessage msg) {
        		if (msg instanceof Announce) {
        			if (info.getPublisher().equals("127.0.0.1")) // ignore announces from loopback
        				return;	
        			String[] services = ((Announce)msg).getServices().split(";");
        			
        			for (String s : services) {
        				if (s.startsWith("imc+udp://")) {
        					String[] parts = s.replaceAll("/", "").split(":");
        					try {
        						localImcHosts.put(msg.getSrc(), new InetSocketAddress(parts[1], Integer.parseInt(parts[2])));
        					}
        					catch (Exception e) {
        						
        					}
        				}
        			}
        			
        			try {
        				sendMessage(msg);
        			}
        			catch (Exception e) {
        				e.printStackTrace();
        			}
        		}
        		else
        			udpMessage(msg, bind_port);
        	}
		});
        return transport;
	}
	
	public ImcProxyClient(String serverHost, int serverPort) throws Exception {
		connect(new URI("ws://"+serverHost+":"+serverPort));
		discovery = createImcUdpHost(30100);
		console("Listening for Announces on port "+discovery.getBindPort());
	}
	
	public static void console(String text) {
		System.out.println(format.format(new Date())+text);
	}
	
	@Override
	public void onMessage(IMCMessage msg) {
		//System.out.println("got "+msg.getAbbrev()+","+msg.getClass().getSimpleName()+" from web");
		if (msg.getMgid() == Announce.ID_STATIC) {
			int imcid = msg.getSrc();
			if(!remoteImcHosts.containsKey(imcid)) {
				UDPTransport transport = createImcUdpHost(7000);
				transport.setImcId(imcid);
				remoteImcHosts.put(imcid, transport);
				imcToPort.put(imcid, transport.getBindPort());
				portToImc.put(transport.getBindPort(), imcid);
				console("Remote "+msg.getString("sys_name")+" now visible at localhost:"+transport.getBindPort());
			}
			String[] services = msg.getString("services").split(";");
			Vector<String> udpServices = new Vector<>();
			
			for (String service : services) {
				if (service.startsWith("imc+udp://")) {
					service = service.substring(10);
					udpServices.add(service.substring(service.indexOf('/')));
				}
			}
			try {
				int port = remoteImcHosts.get(imcid).getBindPort();
				String serv = "";
				Enumeration<NetworkInterface> e=NetworkInterface.getNetworkInterfaces();
	            while(e.hasMoreElements())
	            {
	                NetworkInterface n=e.nextElement();
	                Enumeration<InetAddress> ee = n.getInetAddresses();
	                while(ee.hasMoreElements())
	                {
	                	InetAddress i= ee.nextElement();
	                	if (!(i instanceof Inet4Address))
	                		continue;
	                	
	                	for (String path : udpServices) {
	                		if (!serv.isEmpty())
	                			serv += ";";
	                		serv += "imc+udp://"+i.getHostAddress()+":"+port+path;
	                	}
	                }
	            }
				msg.setValue("services", serv);
				for (int p = 30100; p < 30105; p++)
                    remoteImcHosts.get(imcid).sendMessage("224.0.75.69", p, msg);								
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}
		
		int dst = msg.getDst();
		if (localImcHosts.containsKey(dst)) {
			InetSocketAddress addr = localImcHosts.get(msg.getDst());
			if (remoteImcHosts.containsKey(msg.getSrc()))
				remoteImcHosts.get(msg.getSrc()).sendMessage(addr.getHostName(), addr.getPort(), msg);
			else
				discovery.sendMessage(addr.getHostName(), addr.getPort(), msg);
			//System.out.println("\t-->"+addr.getHostName()+":"+addr.getPort());
		}
		//else {
		//	System.err.println("Message "+msg.getAbbrev()+" from web ("+msg.getSrc()+") cannot be delivered to "+dst);
		//}
	}
	
	public void udpMessage(IMCMessage message, int port) {
		//System.out.println("got "+message.getAbbrev()+" from udp to "+portToImc(port));
		try {
			message.setDst(portToImc(port));
			sendMessage(message);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected int imcToPort(int imcid) {
		return imcToPort.get(imcid);
	}
	
	protected int portToImc(int port) {
		Integer p = portToImc.get(port); 
		if (p == null) {
			return port;
		}
			
		return p;
	}
	
	public static void main(String[] args) throws Exception {
		String host = "zpserver.info";
		int port = 9090;
		
		if (args.length == 2) {
			try {
				port = Integer.parseInt(args[1]);
				host = args[0];
			}
			catch (Exception e) {
				System.out.println("Usage: ./imcproxy <host> <port>");
				return;
			}
		}
		ImcProxyClient.console("Connecting to server at "+host+":"+port);
		new ImcProxyClient(host, port);		
	}	
}
