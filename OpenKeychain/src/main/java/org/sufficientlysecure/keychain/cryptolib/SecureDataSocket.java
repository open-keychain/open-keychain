package cryptolib;

import cryptolib.FDESocket;
import cryptolib.CryptoSocketInterface.Channel;
import cryptolib.CryptoSocketInterface.ChannelType;
import java.io.IOException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public class SecureDataSocket {
	
	private int port;
	private FDESocket socket;
	private SocketAddress connectionInfo;	

	public SecureDataSocket(int port){
		this.port = port;
	}

	/**
	 * setup client with commitmentscheme.
	 * connectionDetails have to look like ipAddress:port
	 *
	 * compare the returned String with the one on the other device. call comparedPhrases(stringsWereEqual) afterwards.
	 * */
	public String setupClientNoCamera(String connectionDetails) throws IOException {
		//commitment
		this.socket = new FDESocket(new Channel(ChannelType.WLAN, connectionDetails));
		this.socket.connect();
		return byteArrayToPhrase(this.socket.getOOB());
	}

	/**
	 * compare
	 * */
	public void comparedPhrases(boolean phrasesMatched) {
		if (phrasesMatched) {
			this.socket.verifiedOOB();
		}
		else {
			this.close();
		}
	}

	/**
	 * setup client with already known sharedSecret.
	 * connectionDetails have to look like ipAddress:port:sharedSecret
	 *
	 * Connection established afterwards.
	 * */
	public void setupClientWithCamera(String connectionDetails) throws IOException {
		this.socket = new FDESocket(new Channel(ChannelType.MANUAL, connectionDetails));
		this.socket.connect();
	}


	/**
	 * setup Server with commitmentscheme.
	 * Method blocks until a client connected.
	 *
	 * compare the returned String with the one on the other device. call comparedPhrases(stringsWereEqual) afterwards.
	 * */
	public String setupServerNoClientCamera() throws IOException {
		this.socket.listen(this.port);
		return byteArrayToPhrase(this.socket.getOOB());
	}


	/**
	 * call setupServerWithClientCamera() afterwards.
	 *
	 * returns the connectiondetails and the sharedSecret, that has to be transferred securely to the client by the user.
	 * */
	public String prepareServerWithClientCamera() throws UnknownHostException {
		this.socket = new FDESocket(new Channel(ChannelType.MANUAL, "::"));
		return Inet4Address.getLocalHost().getHostAddress()+":"+this.port+":"+this.socket.createSharedSecret();
	}


	/**
	 * Method blocks until a client connected
	 * */
	public void setupServerWithClientCamera() throws IOException {
		this.socket.listen(this.port);
	}

	private String byteArrayToPhrase(byte[] bytes) throws IOException {
		//TODO create phrases instead of hexstring
		StringBuilder builder = new StringBuilder();
		for(byte b : bytes) {
			builder.append(String.format("%02x", b));
		}
		return builder.toString();
	}

	
	public byte[] read() throws IllegalArgumentException, IllegalStateException, IOException {
		return this.socket.read();
	}

	public String readString() throws IllegalArgumentException, IllegalStateException, IOException {
		return this.socket.readString();
	}
	
	public int readInt() throws IllegalArgumentException, IllegalStateException, IOException {
		return this.socket.readInt();
	}
	
	public float readFloat() throws IllegalArgumentException, IllegalStateException, IOException {
		return this.socket.readFloat();
	}

	public double readDouble() throws IllegalArgumentException, IllegalStateException, IOException {
		return this.socket.readDouble();
	}

	public Serializable readObject() throws IllegalArgumentException, IllegalStateException, IOException, ClassNotFoundException {
		return this.socket.readObject();
	}

	public int write(byte[] array) throws UnverifiedException, IllegalStateException, IOException {
		return this.socket.write(array);
	}

	public int write(int i) throws UnverifiedException, IllegalStateException, IOException {
		return this.socket.write(i);
	}

	public int write(Float f) throws UnverifiedException, IllegalStateException, IOException {
		return this.socket.write(f);
	}

	public int write(Double d) throws UnverifiedException, IllegalStateException, IOException {
		return this.socket.write(d);
	}

	public int write(String s) throws UnverifiedException, IllegalStateException, IOException {
		return this.socket.write(s);
	}

	public int write(Serializable s)  throws UnverifiedException, IllegalStateException, IOException {
		return this.socket.write(s);
	}

	public void close() {
		this.socket.close();
	}
}
