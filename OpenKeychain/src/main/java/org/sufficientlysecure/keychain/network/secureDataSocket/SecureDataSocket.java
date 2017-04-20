//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;


import org.sufficientlysecure.keychain.network.secureDataSocket.CryptoSocketInterface.Channel;
import org.sufficientlysecure.keychain.network.secureDataSocket.FDESocket;
import org.sufficientlysecure.keychain.network.secureDataSocket.SecureDataSocketException;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class SecureDataSocket {
	
	private int port;
	private FDESocket socket;
	private SocketAddress connectionInfo;	

	public SecureDataSocket(int port){
		this.port = port;
	}

			
	/**
	 * setup client with already known sharedSecret.
	 * connectionDetails have to look like ipAddress:port:sharedSecret
	 *
	 * Connection established afterwards.
	 * */
	public void setupClient(String connectionDetails) throws SecureDataSocketException {
		try {
			this.socket = new FDESocket(new Channel(connectionDetails));
			this.socket.connect();
		} catch(Exception e) {
			throw new SecureDataSocketException(e.toString(), e);
		}
	}


	/**
	 * call setupServerWithClientCamera() afterwards.
	 *
	 * returns the connectiondetails and the sharedSecret, that has to be transferred securely to the client by the user.
	 * */
	public String prepareServer() throws SecureDataSocketException {
		try {
			this.socket = new FDESocket(new Channel("::"));
			return getIPAddress(true) + ":" + this.port + ":" + this.socket.createSharedSecret();
		} catch(Exception e) {
			throw new SecureDataSocketException(e.toString(), e);
		}
	}


	/**
	 * Method blocks until a client connected
	 * */
	public void setupServer() throws SecureDataSocketException {
		try{
			this.socket.listen(this.port);
		} catch(Exception e) {
			throw new SecureDataSocketException(e.toString(), e);
		}
	}
	
	public byte[] read() throws SecureDataSocketException {
		byte[] read;
		try {	
			read = this.socket.read();
		} catch(Exception e) {
			throw new SecureDataSocketException(e.toString(), e);
		}
		return read; 
	}

	public String readString() throws SecureDataSocketException {
		String read;
		try {	
			read = this.socket.readString();
		} catch(Exception e) {
			throw new SecureDataSocketException(e.toString(), e);
		}
		return read; 
	}
	
	public int readInt() throws SecureDataSocketException {
		int read;
		try {	
			read = this.socket.readInt();
		} catch(Exception e) {
			throw new SecureDataSocketException(e.toString(), e);
		}
		return read; 
	}

	public int write(byte[] array) throws SecureDataSocketException {
		int ret = 0;
		try {
			ret = this.socket.write(array);
		}
		catch(Exception e){
			throw new SecureDataSocketException(e.toString(), e);
		}
		return ret;
	}

	public int write(int i) throws SecureDataSocketException {
		int ret = 0;
		try {
			ret = this.socket.write(i);
		}
		catch(Exception e){
			throw new SecureDataSocketException(e.toString(), e);
		}
		return ret;
	}

	public int write(Float f) throws SecureDataSocketException {
		int ret = 0;
		try {
			ret = this.socket.write(f);
		}
		catch(Exception e){
			throw new SecureDataSocketException(e.toString(), e);
		}
		return ret;
	}

	public int write(Double d) throws SecureDataSocketException {
		int ret = 0;
		try {
			ret = this.socket.write(d);
		}
		catch(Exception e){
			throw new SecureDataSocketException(e.toString(), e);
		}
		return ret;
	}

	public int write(String s) throws SecureDataSocketException {
		int ret = 0;
		try {
			ret = this.socket.write(s);
		}
		catch(Exception e){
			throw new SecureDataSocketException(e.toString(), e);
		}
		return ret;
	}

	public int write(Serializable s)  throws SecureDataSocketException {
		int ret = 0;
		try {
			ret = this.socket.write(s);
		}
		catch(Exception e){
			throw new SecureDataSocketException(e.toString(), e);
		}
		return ret;
	}

	public void close() {
		this.socket.close();
	}

	/**
	 * from: http://stackoverflow.com/a/13007325
	 *
	 * Get IP address from first non-localhost interface
	 * @param useIPv4  true=return ipv4, false=return ipv6
	 * @return  address or empty string
	 */
	public static String getIPAddress(boolean useIPv4) {
		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress();
						//boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
						boolean isIPv4 = sAddr.indexOf(':')<0;

						if (useIPv4) {
							if (isIPv4)
								return sAddr;
						} else {
							if (!isIPv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
								return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
							}
						}
					}
				}
			}
		} catch (Exception ex) { } // for now eat exceptions
		return "";
	}
}
