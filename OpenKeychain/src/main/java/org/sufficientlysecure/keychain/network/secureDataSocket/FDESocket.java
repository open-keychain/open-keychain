//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;

import java.security.SecureRandom;
import java.io.IOException;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.lang.IllegalStateException;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;

public class FDESocket {
	//cannot be larger than 32767
	public final int PAYLOAD_SIZE;
	public final int PACKET_SIZE;
	private CryptoSocket cryptoSocket;
	
	public FDESocket(CryptoSocketInterface.Channel c, int payloadSize) throws CryptoSocketException{
		this.PAYLOAD_SIZE = payloadSize;
		this.PACKET_SIZE = PAYLOAD_SIZE+2;
		if (PAYLOAD_SIZE > 32767)
			throw new CryptoSocketException("too large PAYLOAD_SIZE");
		if (PAYLOAD_SIZE < 1)
			throw new CryptoSocketException("too small PAYLOAD_SIZE");
		cryptoSocket = new CryptoSocket(c);
	}
	
	public FDESocket(CryptoSocketInterface.Channel c) throws CryptoSocketException{
		this(c, 1024);
	}

	public SocketAddress listen(int port) throws CryptoSocketException, IOException, SocketTimeoutException{
		return cryptoSocket.listen(port);
	}

	public boolean connect() throws CryptoSocketException, IOException, SocketTimeoutException {
		return cryptoSocket.connect();
	}

	public String createSharedSecret() throws CryptoSocketException, IOException {
		return cryptoSocket.createSharedSecret();
	}

	public byte[] read() throws CryptoSocketException, IllegalStateException, IOException {
		boolean done = false;
		byte[] data = new byte[0];
		//stop reading after specific size?
		while(!done){
			byte[] newData = new byte[PACKET_SIZE];
			//maybe read more times here?
			cryptoSocket.read(true, newData);
			done = isLast(newData);
			newData = getContent(newData);
			data = merge(data, newData);
		}
		return data;
	}

	public String readString() throws CryptoSocketException, IllegalStateException, IOException {
		return new String(read());
	}

	public int readInt() throws CryptoSocketException, IllegalStateException, IOException {
		ByteBuffer wrapped = ByteBuffer.wrap(read());
		return wrapped.getInt();
	}
	
	public float readFloat() throws CryptoSocketException, IllegalStateException, IOException {
		ByteBuffer wrapped = ByteBuffer.wrap(read());
		return wrapped.getFloat();
	}

	public double readDouble() throws CryptoSocketException, IllegalStateException, IOException {
		ByteBuffer wrapped = ByteBuffer.wrap(read());
		return wrapped.getDouble();
	}

	public Serializable readObject() throws CryptoSocketException, IllegalStateException, IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(read());
		ObjectInputStream in = new ObjectInputStream(bis);
		return (Serializable) in.readObject();
	}

	public int write(byte[] array) throws UnverifiedException, IllegalStateException, CryptoSocketException, IOException{
		byte[][] packets = splitUp(array);
		for (byte[] packet : packets){
			if (cryptoSocket.write(packet) != RETURN.SUCCESS.getValue()){
				byte[] end = {(byte)0, (byte)0x80}; 
				cryptoSocket.write(end);
				return RETURN.INVALID_CIPHERTEXT.getValue();
			}
		}
		return RETURN.SUCCESS.getValue();
	}

	public int write(int i) throws UnverifiedException, IllegalStateException, CryptoSocketException, IOException{
		ByteBuffer dbuf = ByteBuffer.allocate(4);
		dbuf.putInt(i);
		byte[] bytes = dbuf.array();
		return write(bytes);
	}

	public int write(Float f) throws UnverifiedException, IllegalStateException, CryptoSocketException, IOException{
		ByteBuffer dbuf = ByteBuffer.allocate(4);
		dbuf.putFloat(f);
		byte[] bytes = dbuf.array();
		return write(bytes);
	}

	public int write(Double d) throws UnverifiedException, IllegalStateException, CryptoSocketException, IOException{
		ByteBuffer dbuf = ByteBuffer.allocate(8);
		dbuf.putDouble(d);
		byte[] bytes = dbuf.array();
		return write(bytes);
	}

	public int write(String s) throws UnverifiedException, IllegalStateException, CryptoSocketException, IOException{
		byte[] bytes = s.getBytes();
		return write(bytes);
	}

	public int write(Serializable s)  throws UnverifiedException, IllegalStateException, CryptoSocketException, IOException{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(s);
		out.close();

		// Get the bytes of the serialized object
		byte[] buf = bos.toByteArray();
		return write(buf);
	}

	public void close(){
		this.cryptoSocket.close();
	}

	private byte[][] splitUp(byte[] array){
		int parts = array.length/PAYLOAD_SIZE + 1;
		byte[][] out = new byte[parts][PACKET_SIZE];
		for (int i = 0; i < parts; i++){
			byte header1 = (byte)(PAYLOAD_SIZE%256);
			byte header2 = (byte)(PAYLOAD_SIZE/256);
			int partSize = PAYLOAD_SIZE;

			if (i == parts-1){
				int rest = array.length%PAYLOAD_SIZE; 
				header1 = (byte)(rest%256);
				header2 = (byte)(rest/256);
				/*has no following packets*/
				header2 = (byte)(header2 | 0x80);
				partSize = rest;
				SecureRandom sr = new SecureRandom(); 
				sr.nextBytes(out[i]);
			}
			out[i][0] = header1;
			out[i][1] = header2;
			System.arraycopy(array, i*PAYLOAD_SIZE, out[i], 2, partSize);
		}
		return out;
	}

	private boolean isLast(byte[] packet) throws CryptoSocketException {
		if (packet.length != PACKET_SIZE){
			throw new CryptoSocketException("invalid packetsize!");
		}
		return packet[1] < 0;
	}

	private byte[] getContent(byte[] packet) throws CryptoSocketException {
		/*if (packet.length != PACKET_SIZE){
			throw new CryptoSocketException("invalid packetsize!");
		}*/ //done in getContentSize
		int size = getContentSize(packet);
		byte[] out = new byte[size];
		System.arraycopy(packet, 2, out, 0, size);
		return out;
	}

	private int getContentSize(byte[] packet) throws CryptoSocketException {
		if (packet.length != PACKET_SIZE){
			throw new CryptoSocketException("invalid packetsize!");
		}
		int size = (packet[0] & 0xFF)+(packet[1] & 0x7F)*256;
		if (size > PAYLOAD_SIZE){
			throw new CryptoSocketException("invalid lengthfield!");
		}
		return size;
	}

	private byte[] merge(byte[] arr1, byte[] arr2) {
		byte[] out = new byte[arr1.length+arr2.length];
		System.arraycopy(arr1, 0, out, 0, arr1.length);
		System.arraycopy(arr2, 0, out, arr1.length, arr2.length);
		return out;
	}
}
