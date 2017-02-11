package cryptolib;

import java.security.SecureRandom;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;
import javax.crypto.Mac;
import org.bouncycastle.crypto.generators.BCrypt;


public class CryptoCommitmentObject{

	//sizes of params in bytes
	final int messageLength;
	final int aLength;
	final int xLength;
	final int hLength = 24; //BCrypt
	final int saltLength = 16;
	final int cost = 14;

	private byte[] myCommitment = null, myDecommitment = null;
	private byte[] otherCommitment = null, otherDecommitment = null;
	private byte[]myA, myX;
	private byte[]otherA, otherX;
	private BigInteger myB, myMessage;
	private BigInteger otherB, otherMessage;
	private byte[] mySalt = null;
	private Mac hmac = null;

	public CryptoCommitmentObject(byte[] message) throws IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException {
		messageLength = message.length;
		aLength = messageLength;
		xLength = messageLength;
		this.myMessage = new BigInteger(1,message);
		/*//DEBUG BEGIN
		System.out.println("myMessage");
		for (int i = 0; i < fillUp(myMessage.toByteArray()).length; i++){
			System.out.print(fillUp(myMessage.toByteArray())[i]);
			System.out.print(".");
		}
		System.out.println();
		//DEBUG END*/

		SecureRandom random = new SecureRandom();
		this.myA = new byte[aLength];
		random.nextBytes(this.myA);
		/*//DEBUG BEGIN
		System.out.println("myA");
		for (int i = 0; i < aLength; i++){
			this.myA[i] = (byte) 255;
			System.out.print(myA[i]);
			System.out.print(".");
		}
		System.out.println();
		//DEBUG END*/
		this.myX = new byte[xLength];
		random.nextBytes(this.myX);
		/*//DEBUG BEGIN
		System.out.println("myX");
		for (int i = 0; i < xLength; i++){
			this.myX[i] = (byte) 255;
			System.out.print(myX[i]);
			System.out.print(".");
		}
		System.out.println();
		//DEBUG END*/
		BigInteger a = new BigInteger(1,this.myA);
		BigInteger x = new BigInteger(1,this.myX);
		/*byte[] tmpBytes = new byte[xLength];
		tmpBytes[0] = (byte)128;
		for (int i = 1; i < tmpBytes.length; i++){
			tmpBytes[i] = 0;
		}*/
		this.myB = myMessage.subtract(a.multiply(x)).mod(new BigInteger("2").pow(xLength*8));//.add(new BigInteger(tmpBytes));
		/*//DEBUG BEGIN
		System.out.println("myB");
		System.out.println(myB.toString());
		byte[] tmpB = fillUp(myB.toByteArray());
		for (int i = 0; i < tmpB.length; i++){
			//tmpB[i] = (byte) 255;
			System.out.print(tmpB[i]);
			System.out.print(".");
		}
		System.out.println();
		//DEBUG END*/
		this.mySalt = new byte[saltLength];
		random.nextBytes(this.mySalt);
		SecretKey key = new SecretKeySpec(myX, "HmacSHA512");
		Mac mac = Mac.getInstance(key.getAlgorithm());
		mac.init(key);
		this.hmac = mac;
		//create Commitment
		createCommitment();
		createDecommitment();
	}

	private void createCommitment() {
		byte[] myBb = formatMessage(this.myB.toByteArray());
		int size = this.hmac.getMacLength() + hLength + saltLength + aLength + myBb.length;
		byte[] commitment = new byte[size];
		//create H(x)
		byte[] h = BCrypt.generate(this.myX, this.mySalt, cost);
		System.arraycopy(h, 0, commitment, this.hmac.getMacLength(), hLength);
		//append mySalt
		System.arraycopy(this.mySalt, 0, commitment, this.hmac.getMacLength() + hLength, saltLength);
		//create g (hmac)
		byte [] hb = new byte[hLength+saltLength+myBb.length+myA.length];
		System.arraycopy(myA, 0, hb, hLength+saltLength, aLength);
		System.arraycopy(myBb, 0, hb, hLength+saltLength+aLength, myBb.length);
		System.arraycopy(this.mySalt, 0, hb, hLength, saltLength);
		System.arraycopy(h, 0, hb, 0, hLength);
		System.arraycopy(this.hmac.doFinal(hb), 0, commitment, 0, this.hmac.getMacLength());
		//append a
		System.arraycopy(myA, 0, commitment, this.hmac.getMacLength()+ hLength + saltLength, aLength);
		//append b
		System.arraycopy(myBb, 0, commitment, this.hmac.getMacLength() + hLength + saltLength + aLength, myBb.length);
		Arrays.fill(myBb, (byte) 0);
		Arrays.fill(h, (byte) 0);
		Arrays.fill(hb, (byte) 0);
		this.myCommitment = commitment;
	}

	private void createDecommitment() throws InvalidKeyException, NoSuchAlgorithmException {
		int size = this.hmac.getMacLength() + xLength;
		byte[] decommitment = new byte[xLength];
		System.arraycopy(myX, 0, decommitment, 0, myX.length);
		this.myDecommitment = decommitment;
	}

	public void addOtherCommitment(byte[] commitment) throws IllegalArgumentException {
		if (commitment.length != commitmentSize())
			throw new IllegalArgumentException();

		this.otherCommitment = commitment;
	}

	public void open(byte[] decommitment) throws IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException {
		if (this.myCommitment == null || this.myDecommitment == null || this.otherCommitment == null || decommitment.length != decommitmentSize()){
			throw new IllegalArgumentException();
		}

		//parse decommitment
		int macSize = this.hmac.getMacLength();
		this.otherDecommitment = decommitment;
		byte[] otherXb = Arrays.copyOfRange(this.otherDecommitment, 0, this.xLength);
		//parse commitment
		byte[] otherMacb = Arrays.copyOfRange(this.otherCommitment, 0, macSize);
		byte[] otherHb = Arrays.copyOfRange(this.otherCommitment, macSize, macSize + hLength);
		byte[] otherSaltb = Arrays.copyOfRange(this.otherCommitment, macSize + hLength, macSize + hLength + saltLength);
		byte[] otherAb = Arrays.copyOfRange(this.otherCommitment, macSize + hLength + saltLength, macSize + hLength + saltLength + aLength);
		byte[] otherBb = Arrays.copyOfRange(this.otherCommitment, macSize + hLength + saltLength + aLength, this.otherCommitment.length);
		byte[] hb = new byte[otherHb.length+otherSaltb.length+otherAb.length+otherBb.length];
		System.arraycopy(otherHb, 0, hb, 0, otherHb.length);
		System.arraycopy(otherSaltb, 0, hb, otherHb.length, otherSaltb.length);
		System.arraycopy(otherAb, 0, hb, otherHb.length + otherSaltb.length, otherAb.length);
		System.arraycopy(otherBb, 0, hb, otherHb.length + otherSaltb.length + otherAb.length, otherBb.length);

		/*//DEBUG BEGIN
		System.out.println("otherBb");
		for (int i = 0; i < otherBb.length; i++){
			System.out.print(otherBb[i]);
			System.out.print(".");
		}
		System.out.println();
		System.out.println("otherAb");
		for (int i = 0; i < otherAb.length; i++){
			System.out.print(otherAb[i]);
			System.out.print(".");
		}
		System.out.println();
		System.out.println("otherXb");
		for (int i = 0; i < otherXb.length; i++){
			System.out.print(otherXb[i]);
			System.out.print(".");
		}
		System.out.println();
		//DEBUG END*/

		SecretKey key = new SecretKeySpec(otherXb, this.hmac.getAlgorithm());
		Mac mac = Mac.getInstance(key.getAlgorithm());
		mac.init(key);

		//validate decommitment mac // <-- not needed anymore since decommitment contains only x
		/*if (!Arrays.equals(mac.doFinal(Arrays.copyOfRange(this.otherDecommitment, macSize, this.otherDecommitment.length)), otherTag)){
			this.otherDecommitment = null;
			throw new IllegalArgumentException();
		}*/
	
		//validate commitment mac
		if (!Arrays.equals(mac.doFinal(hb), otherMacb)){
			this.otherDecommitment = null;
			throw new IllegalArgumentException();
		}

		//validate BCrypt
		if (!Arrays.equals(BCrypt.generate(otherXb, otherSaltb, cost), otherHb)){
			this.otherDecommitment = null;
			throw new IllegalArgumentException();
		}

		//set Values
		BigInteger bigA = new BigInteger(1,otherAb);
		BigInteger bigX = new BigInteger(1,otherXb);
		this.otherA = otherAb;
		this.otherX = otherXb;
		this.otherB = new BigInteger(1,otherBb);
		/*byte[] tmpBytes = new byte[xLength];
		tmpBytes[0] = (byte)128;
		for (int i = 1; i < tmpBytes.length; i++){
			tmpBytes[i] = 0;
		}*/
		this.otherMessage = this.otherB.add(bigA.multiply(bigX)).mod(new BigInteger("2").pow(xLength*8));//.add(new BigInteger(tmpBytes));

		Arrays.fill(otherMacb, (byte) 0);
		Arrays.fill(otherHb, (byte) 0);
		Arrays.fill(otherSaltb, (byte) 0);
		Arrays.fill(otherBb, (byte) 0);
		Arrays.fill(hb, (byte) 0);
	}

	public byte[] getCommitment(){
		return this.myCommitment;
	}

	public byte[] getDecommitment(){
		return this.myDecommitment;
	}

	public byte[] getOtherMessage(){
		return formatMessage(this.otherMessage.toByteArray());
	}

	private byte[] formatMessage(byte[] arr){
		byte[] myArr = new byte[this.messageLength];

		if (this.messageLength == arr.length){
			return arr;
		}

		if (this.messageLength > arr.length){
			System.arraycopy(arr, 0, myArr, this.messageLength - arr.length, arr.length);

			for (int i = 0; i < this.messageLength - arr.length; i++){
				myArr[i] = (byte) 0x00;
			}
		} else {
			System.arraycopy(arr, arr.length - this.messageLength, myArr, 0, myArr.length);
		}

		return myArr;
	}

	public int commitmentSize(){
		return myCommitment.length;
	}

	public int decommitmentSize(){
		return myDecommitment.length;
	}
	
}
