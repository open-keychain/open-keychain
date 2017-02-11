package cryptolib;
import java.lang.IllegalStateException;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.io.IOException;

public interface CryptoSocketInterface {

	public enum ChannelType {
		/**
		 * A Commitmentscheme will be used to create a sharedSecret. Therefore a OOB is created. which has to be compared.
		 * Method to get the OOB is called getOOB().
		 * Method to sign the OOBs of both sides equal is verifiedOOB().
		 * These Methods have to be called AFTER a networkconnection has been established (via listen or connect), because the connection will be required for the commitmentscheme.
		 * The networkconnection will be fully usable after verifiedOOB() has been called.
		 *
		 * Methods setSharedSecret(byte[] sharedSecret) and createSharedSecret() will be unnecassary here.
		 * */
		WLAN, 
		/**
		 * A sharedSecret will be created and given to the user. The User has to transfer it securely (!!!) to the partnerdevice.
		 * Once a sharedSecret is known by both devices a networkconnection can be established. I.e. a sharedSecret has not to be created on both sides.
		 * Method to create the sharedSecret is called createSharedSecret().
		 * Method to set the sharedSecret in the partnerdevice is called setSharedSecret(byte[] sharedSecret).
		 * These Methods have to be called BEFORE a networkconnection has been established (via listen or connect), because the sharedSecret will be used for the Crypto.
		 * The networkconnection will be fully usable afert it has been established.
		 * 
		 * Methods getOOB() and verifiedOOB() will be unnecassary here.
		 * */
		MANUAL;	
	}

	public class Channel {
		/**
		 * definitions of id.
		 *
		 *  ChannelType | id                          | Description
		 * -------------+-----------------------------+--------------------------------------------
		 *  WLAN        | ipAddress:Port              | OOBChannel and networkChannel are the same.
		 *              |                             | ipAddress is optional in future versions 
		 *              |                             | beause then the partnerdevice may be found 
		 *              |                             | via broadcast.
		 *              |                             |
		 *  MANUAL      | ipAddress:Port:sharedSecret | only the networkChannel is defined here.
		 *              |                             | If used as a client the sharedSecret has to
		 *              |                             | be set.
		 * */
		public final String id;
		public final ChannelType type; 

		public Channel(ChannelType type, String id) {
			this.id = id;
			this.type = type;
		}
	}

	public enum RETURN {
		SUCCESS(0),
		READ(-1), 
		WRITE(-2), 
		WRONG_TAG(-3),
		NOT_AVAILABLE(-4), 
		INVALID_CIPHERTEXT(-5);

		private final int value;

		RETURN(int value) {
			this.value = value;
		}

		public int getValue(){
			return this.value;
		}
	}

	/**
	* Await a connection from a device, which calls connect().
	* listen is blocking, until a connection is established
	* port - On which port server listening. 0 for random
	*/
	SocketAddress listen(int port) throws IOException, SocketTimeoutException; //blocking until connection is established


	/**
	 * Same as listen(int port). Except choosing port outof Channel.id.
	 * */
	//SocketAddress listen() throws IOException, SocketTimeoutException; //blocking until connection is established


	/**
	* Connect to a device, which awaits a connection and called listen().
	* Throws SocketTimeoutException when connection timedout, IllegalArgumentException when the 
	* destination id is wrong or IOException if socket creation fails
	* returns if connection is established
	*/
	boolean connect() throws IllegalArgumentException, IOException, SocketTimeoutException;

	/**
	* Return the OutOfBand information.
	* IMPORTANT: This has to be the same between both 
	* communicationpartners. If it differs, NEVER call verifiedOOB()!
	* Usable with ChannelType.WLAN
	*/
	byte[] getOOB() throws IllegalStateException;
	
	/**
	* Confirmation, that the other side is the one we expected.
	* Use getOOB() for comparison, between both partners. 
	* IMPORTANT: when calling verifiedOOB() you sign, that you verified the
	* communicationpartner is the one you expect. If you call verifiedOOB() 
	* though your verificationresult is false, an adversary will be able to
	* read and manipulate your communication.
	* Usable with ChannelType.WLAN
	*/
	void verifiedOOB() throws IllegalStateException;

	/**
	 * Sets the SharedSecret.
	 * This has to be used if the channel MANUAL is used and you will not create a sharedSecret.
	 * */
	//void setSharedSecret(byte[] sharedSecret) throws IllegalArgumentException; // <--- private now, because used in connect internaly

	/**
	 * Creates and sets a sharedSecret. 
	 * It will be returned, so the user can transfer it to the communicationpartner and set it there.
	 * I.e. channel MANUAL has to be used.
	 * */
	String createSharedSecret() throws IllegalArgumentException;

	/**
	* Sends the given bytearray to the communicatonpartner.
	* If the partnerdevice is not verified (via OOB), an UnverifiedException will be
	* thrown.
	* Return negativ value on error, for error codes please look at enum RETURN.
	* 	Returns RETURN.INVALID_CIPHERTEXT or RETURN.SUCCESS
	*/
	int write(byte[] array) throws UnverifiedException, IllegalStateException, IOException;
		
	/*
	* Reads bytes send from the communicationpartner and stores them in data.
	* If blocking is true, this function blocks until the next entity is 
	* read.
	* If blocking is false, methods tries to read data.length byte or less bytes.
	* Returns the total number of bytes read into the buffer. 
	* Negativ value on error, for error codes please look at enum RETURN.
	* 	Returns RETURN.READ, RETURN.WRONG_TAG, RETURN.NOT_AVAILABLE, RETURN.INVALID_CIPHERTEXT
	*/
	int read(boolean blocking, byte[] data) throws IllegalStateException, IllegalArgumentException, IOException;
	
	/**
	* Returns true, if there is something to read, false, if not.
	*/
	boolean hasNext() throws IOException;

	/**
	* Close the connection. 
	*/
	void close();
}
