package cryptolib;

import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.IllegalBlockSizeException;
import java.security.SecureRandom;
import java.util.Base64;


public class CryptoSocket implements CryptoSocketInterface {
		private Channel channel = null;
		private ServerSocket server = null;
		private Socket socket = null;
		private OutputStream out = null;
		private InputStream in = null;
		private CryptoObject cobject = null;
		private boolean verified = false;
		private boolean connected = false;
		private boolean running = false;

		/**
		* Constructor.
		* channel defines the interface, which will be used for the 
		* sharedsecretexchange.
		* If you call listen() afterwards, connections from the given
		* destination are accepted.
		* If you otherwise call connect() afterwards, the other device will be 
		* searched in the localnetwork via broadcast and when found, a 
		* connection will be set up.
		*/
		//public CryptoSocket(Channel channel){
		//	this.channel = channel;
		//}

		/**
		* Constructor.
		* channel defines the interface, which will be used for the 
		* sharedsecretexchange.
		* If you call listen() afterwards, only connections from the given
		* destination are accepted.
		* If you otherwise call connect() afterwards, you will connect to the
		* given destination.
		*/
		public CryptoSocket(Channel channel){
			this.channel = channel;
		}

		public SocketAddress listen(int port) throws IOException, SocketTimeoutException {
			if (this.channel.type == ChannelType.WLAN){
				this.server = new ServerSocket(port);
				this.createCryptoObject();
				this.running = true;
				while(this.running){
					while (true){
						this.socket = this.server.accept();

						if (!(this.channel.id.equals("") || this.channel.id.equals(":") || this.channel.id.equals("::")) && !this.channel.id.equals(socket.getRemoteSocketAddress().toString())){
							socket.close();
							continue;
						}

						break;
					}

					//System.out.println("Begin crypto protocol..");
					// begin crypto protocol
					this.out = this.socket.getOutputStream();
					this.in = this.socket.getInputStream();

					if (!this.setup()){
						continue;
					}

					if (!this.exchange()){
						continue;
					}

					this.connected = true;
					break;
				}
				return this.socket.getRemoteSocketAddress();
			}
			if (this.channel.type == ChannelType.MANUAL) {
				if (!this.verified) {
					throw new IllegalArgumentException("call setSharedSecret or createSharedSecret before listen, if the ChannelType MANUAL is used.");
				}
				else {	
					this.server = new ServerSocket(port);
					this.running = true;
					while(this.running){
						while (true){
							this.socket = this.server.accept();

							if (!(this.channel.id.equals("") || this.channel.id.equals(":") || this.channel.id.equals("::")) && !this.channel.id.equals(socket.getRemoteSocketAddress().toString())){
								socket.close();
								continue;
							}

							break;
						}	

						//System.out.println("Begin crypto protocol..");
						// begin crypto protocol
						this.out = this.socket.getOutputStream();
						this.in = this.socket.getInputStream();
						this.connected = true;
						//TODO add check if the same sharedSecret was used
						break;
					}
					return this.socket.getRemoteSocketAddress();
				}
			}
			return null;
		}

		public boolean connect() throws IllegalArgumentException, IOException, SocketTimeoutException {
			if (this.channel.type == ChannelType.WLAN){
				this.createCryptoObject();

				if (!this.channel.id.equals("")){
					String tmp = new StringBuilder(this.channel.id).reverse().toString();

					if (!tmp.contains(":")){
						throw new IllegalArgumentException("ERROR for a WLAN channel the identifier needs to be ip port combination e.g. 127.0.0.1:1337 or ::1:4711");
					}

					String[] stringParts = tmp.split(":", 2);


					if (2 != stringParts.length){
						throw new IllegalArgumentException("ERROR for a WLAN channel the identifier needs to be ip port combination e.g. 127.0.0.1:1337 or ::1:4711");
					}

					stringParts[0] = new StringBuilder(stringParts[0]).reverse().toString();
					stringParts[1] = new StringBuilder(stringParts[1]).reverse().toString();
					String serveraddress = stringParts[1];
					String port = stringParts[0];
					InetSocketAddress address = null;

					try {
						address = new InetSocketAddress(serveraddress, Integer.parseInt(port));
					} catch (Exception e) {
						throw new IllegalArgumentException("ERROR for a WLAN channel the identifier needs to be ip port combination e.g. 127.0.0.1:1337 or ::1:4711");
					}

					this.socket = new Socket(address.getHostString(), address.getPort());
				} else {
					//Search server via broadcast
					//TODO implement
					//while not implemented throw Exception
					throw new IllegalArgumentException("Serversearch via broadcast is not implemented at the Moment.");
				}

				// begin crypto protocol
				//System.out.println("Begin crypto protocol..");
				this.out = this.socket.getOutputStream();
				this.in = this.socket.getInputStream();
				if (!this.setup()){
					return false;
				}

				if (!this.exchange()){
					return false;
				}

				this.connected = true;
				return true;
			}
			if (this.channel.type == ChannelType.MANUAL) {
				if (!this.channel.id.equals("")){
					String tmp = new StringBuilder(this.channel.id).reverse().toString();

					if (!tmp.contains(":")){
						throw new IllegalArgumentException("ERROR for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
					}

					String[] stringParts = tmp.split(":", 3);


					if (3 != stringParts.length){
						throw new IllegalArgumentException("ERROR for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
					}

					stringParts[0] = new StringBuilder(stringParts[0]).reverse().toString();
					stringParts[1] = new StringBuilder(stringParts[1]).reverse().toString();
					stringParts[2] = new StringBuilder(stringParts[2]).reverse().toString();
					this.setSharedSecret(stringParts[0]);
					String serveraddress = stringParts[2];
					String port = stringParts[1];
					InetSocketAddress address = null;

					try {
						address = new InetSocketAddress(serveraddress, Integer.parseInt(port));
					} catch (Exception e) {
						throw new IllegalArgumentException("ERROR for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
					}

					this.socket = new Socket(address.getHostString(), address.getPort());
				} else {
					throw new IllegalArgumentException("ERROR for a MANUAL channel the identifier needs to be ip-port-sharedsecret combination e.g. 127.0.0.1:1337:mySharedSecretMySharedSecret1234 or ::1:4711:mySharedSecretMySharedSecret1234");
				}

				// begin crypto protocol
				//System.out.println("Begin crypto protocol..");
				this.out = this.socket.getOutputStream();
				this.in = this.socket.getInputStream();
				this.connected = true;
				//TODO add check if the same sharedSecret was used
				return true;
			}
			return false;
		}

		private boolean setup() throws IOException{
			boolean sentAck = false;
			boolean receivedAck = false;
			byte [] data = "Ready".getBytes();
			byte [] read = null;
			this.out.write(data);
			this.out.flush();
			int readBytes = 0;
			int sleepSecond = 0;
			int waitForBytes = 5;
			while (sleepSecond < 300){

				if (waitForBytes > this.in.available()){
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch(InterruptedException ie){
						return false;
					}

					sleepSecond = sleepSecond + 1;
					continue;
				}

				read = new byte[waitForBytes];
				readBytes = this.in.read(read);

				if (readBytes != waitForBytes){
					return false;
				}

				String answer = new String(read);
				switch(answer){
				case "Ready":
					if (!sentAck){
						data = "ACK".getBytes();
						this.out.write(data);
						this.out.flush();
						sentAck = true;
						waitForBytes = 3;
						break;
					} else {
						data = "FIN".getBytes();
						this.out.write(data);
						this.out.flush();
						return false;
					}
				case "ACK":
					if (!receivedAck){
						receivedAck = true;
						break;
					} else {
						data = "FIN".getBytes();
						this.out.write(data);
						this.out.flush();
						return false;
					}
				case "FIN":
					return false;
				default:
					data = "FIN".getBytes();
					this.out.write(data);
					this.out.flush();
					return false;
				}

				if (sentAck && receivedAck){
					break;
				}
			}

			if (300 == sleepSecond){
				return false;
			}

			return true;
		}

		private boolean exchange() throws IOException{
			byte [] commitment = this.cobject.getCryptoCommitment().getCommitment();
			byte [] otherCommitment = new byte[this.cobject.getCryptoCommitment().commitmentSize()];
			this.out.write(commitment);
			this.out.flush();


			for(int sleeptime = 0; sleeptime < 5; sleeptime++){
				if (otherCommitment.length <= this.in.available()){
					break;
				}

				try{
					TimeUnit.SECONDS.sleep(1);
				} catch(InterruptedException ie){
					return false;
				}
			}

			int readSize = 0;
			int count = 0;

			if (otherCommitment.length > this.in.available()){
				this.createCryptoObject();
				return false;
			}

			while (readSize < otherCommitment.length){
				count = this.in.read(otherCommitment, readSize, otherCommitment.length - readSize);

				if (0 > count){
					this.createCryptoObject();
					return false;
				}

				readSize = readSize + count;
			}

			if (readSize != otherCommitment.length){
				this.createCryptoObject();
				return false;
			}

			try {
				this.cobject.getCryptoCommitment().addOtherCommitment(otherCommitment);
			} catch(IllegalArgumentException ia){
				this.createCryptoObject();
				return false;
			}

			byte [] decommitment = this.cobject.getCryptoCommitment().getDecommitment();
			byte [] otherDecommitment = new byte[this.cobject.getCryptoCommitment().decommitmentSize()];
			this.out.write(decommitment, 0, decommitment.length);
			this.out.flush();
			readSize = 0;

			for(int sleeptime = 0; sleeptime < 5; sleeptime++){
				if (otherDecommitment.length == this.in.available()){
					break;
				}

				try{
					TimeUnit.SECONDS.sleep(1);
				} catch(InterruptedException ie){
					return false;
				}
			}

			if (otherDecommitment.length != this.in.available()){
				this.createCryptoObject();
				return false;
			}

			while(readSize < otherDecommitment.length){
				count = this.in.read(otherDecommitment, readSize, otherDecommitment.length - readSize);
				
				if (0 > count){
					this.createCryptoObject();
					return false;
				}

				readSize = readSize + count;
			}

			if (readSize != otherDecommitment.length){
				this.createCryptoObject();
				return false;
			}

			try {
				System.out.println("Opening..");
				this.cobject.openCommitmentAndCreateSharedSecret(otherDecommitment);
			} catch(IllegalArgumentException ia){
				this.createCryptoObject();
				ia.printStackTrace();
				return false;
			} catch(InvalidKeyException ike){
				ike.printStackTrace();
			 	this.createCryptoObject();
			 	return false;
			 } catch(NoSuchAlgorithmException nsa){
			 	nsa.printStackTrace();
			 	this.createCryptoObject();
				return false;
			}

			return true;
		}

		private void createCryptoObject() throws IllegalArgumentException {
			this.cobject = new CryptoObject();
			try {
				this.cobject.createCryptoCommitment();
			} catch(InvalidKeyException ike){
				throw new IllegalArgumentException("ERROR cannot create commitment!");
			} catch(NoSuchAlgorithmException nsa){
				throw new IllegalArgumentException("Error cannot create commitment!");
			}
		}

		public byte[] getOOB() throws IllegalStateException{
			if (this.channel.type != ChannelType.WLAN)	
				throw new IllegalArgumentException("only usable with ChannelType.WLAN");
			if (this.connected){
				return this.cobject.getOOB();
			} else {
				throw new IllegalStateException("ERROR not connected, call listen() or connect() first!");
			}
		}

		public void verifiedOOB() throws IllegalStateException{
			if (this.channel.type != ChannelType.WLAN)	
				throw new IllegalArgumentException("only usable with ChannelType.WLAN");
			if (this.connected){
				this.verified = true;
			} else {
				throw new IllegalStateException("ERROR not connected, call listen() or connect() first!");
			}
		}

		private void setSharedSecret(String sharedSecret) throws IllegalArgumentException {
			if (this.channel.type != ChannelType.MANUAL)
				throw new IllegalArgumentException("only usable with ChannelType.MANUAL");
			this.cobject = new CryptoObject();
			byte[] byteSharedSecret = Base64.getDecoder().decode(sharedSecret);
			this.cobject.setSharedSecret(byteSharedSecret);
			this.verified = true;
		}

		public String createSharedSecret() throws IllegalArgumentException {
			if (this.channel.type != ChannelType.MANUAL)
				throw new IllegalArgumentException("only usable with ChannelType.MANUAL");
			//create sharedsecret
			byte[] byteSharedSecret = new byte[32];
			SecureRandom rand = new SecureRandom();
			rand.nextBytes(byteSharedSecret);
			String sharedSecret = Base64.getEncoder().encodeToString(byteSharedSecret);
			//set sharedsecret
			this.setSharedSecret(sharedSecret);
			return sharedSecret;
		}

		public int write(byte[] array) throws UnverifiedException, IllegalStateException, IOException{
			if (!this.connected){
				throw new IllegalStateException("ERROR not connected, call listen() or connect() first!");
			}

			if (!this.verified){
				throw new UnverifiedException("ERROR you need to verifiy the channel first before you can write data! This channel may not trustworthy!");
			}

			byte[] ciphertext = null;
			try {
				ciphertext = this.cobject.encrypt(array);
			} catch(IllegalBlockSizeException ibs){
				return RETURN.INVALID_CIPHERTEXT.getValue();
			}
			this.out.write(ciphertext);
			this.out.flush();

			return RETURN.SUCCESS.getValue();
		}

		public int read(boolean blocking, byte [] data) throws IllegalStateException, IllegalArgumentException, IOException {
			int readBytes = 0;
			int totalBytes = 0;

			if (!this.connected){
				throw new IllegalStateException("ERROR cryptosocket is not connected to communication partner!");
			}

			if (null == data){
				throw new IllegalArgumentException("ERROR null byte array not allowed!");
			}

			int size = this.cobject.getOffset() + data.length;
			byte[] tmp = new byte[size];
			byte[] tmp2 = null;

			if (!blocking){
				int available = this.in.available();

				if (available < size){
					return RETURN.NOT_AVAILABLE.getValue();
				} else {
					totalBytes = size;
				}

				readBytes = this.in.read(tmp, 0, totalBytes);
			} else {
				while(totalBytes < size){
					readBytes = this.in.read(tmp, totalBytes, tmp.length - totalBytes);

					if (0 == readBytes){
						try{
							TimeUnit.SECONDS.sleep(1);
						} catch(InterruptedException ie){
							throw new IllegalArgumentException("ERROR Interrupted!");
						}

						continue;
					}

					if (0 > readBytes){
						return RETURN.READ.getValue();
					}

					totalBytes = totalBytes + readBytes;
				}
			}

			try {
				tmp2 = this.cobject.decrypt(tmp);
			} catch (IllegalBlockSizeException ibs){
				return RETURN.INVALID_CIPHERTEXT.getValue();
			}

			if (null == tmp2){
				return RETURN.WRONG_TAG.getValue();
			}

			int copySize = tmp2.length > data.length ? data.length: tmp2.length;
			System.arraycopy(tmp2, 0, data, 0, copySize);
			totalBytes = copySize;
			return totalBytes;
		}

		public boolean hasNext() throws IOException{
			return this.cobject.getOffset() < this.in.available();
		}

		public void close(){
			//clearing attributes
			this.out = null;
			this.in = null;
			this.cobject = null;
			this.verified = false;
			this.connected = false;
			this.running = false;

			try{
				this.socket.close();
				this.server.close();
			} catch(IOException ioe){}

			this.socket = null;
			this.server = null;
		}
}
