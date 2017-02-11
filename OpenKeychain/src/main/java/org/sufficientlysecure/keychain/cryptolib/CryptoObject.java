package cryptolib;

import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException;
import java.security.InvalidKeyException;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Key;
import java.security.Provider;
import java.security.KeyPair;
import java.security.KeyFactory;
import java.security.Security;
import javax.crypto.BadPaddingException;
import javax.crypto.AEADBadTagException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.KeyAgreement;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.ec.*;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.agreement.ECDHCBasicAgreement;

import java.util.Arrays;

public class CryptoObject {
	private Provider provider = null;
	private KeyPair encKeypair = null;
	private SecretKeySpec sharedSecretInner = null;
	private SecretKeySpec sharedSecretOuter = null;
	private Cipher encOuter = null;
	private Cipher encInner = null;
	private Cipher decOuter = null;
	private Cipher decInner = null;
	private String enc_algorithm = "";
	private String curve = "";
	private byte[] OOB = null;
	private boolean merged = false;
	private boolean has_symmetric_key = false;
	private CryptoCommitmentObject cc = null;
	private SecureRandom random = null;
	private int iv_size = 16;
	private int tag_size = 32;

	/** 
	* Constructor.
	* Create a new CryptoObject with encryption asymmetric elliptic curve25519 encryption keypair 
	* Short authentication byte length is 3 byte.
	*/
	public CryptoObject() throws IllegalArgumentException {
		this("curve25519", "ECDH", 3, 16, 16);
	}

	/** 
	* Constructor.
	* Create a new CryptoObject with encryption asymmetric elliptic curve encryption keypair 
	* and digital sign asymmetric elliptic curve keypair.
	* curve specificies elliptic curve for encryption scheme and sign algorithm e.g. "curve25519"
	* enc_algorithm must be an implemented elliptic curve encryption algorithm e.g. "ECDH"
	* shortAuthenticationStringSize must be a positive number, that represents the short authentication byte length.
	* iv_size must be positiv, byte size of iv for encryption scheme
	* tag_size must be positiv, byte size of tag for encryption scheme
	*/
	public CryptoObject(String curve, String enc_algorithm, int shortAuthenticationStringSize, int iv_size, int tag_size) throws IllegalArgumentException{
		if (0 >= shortAuthenticationStringSize || 0 >= iv_size || 0 >= tag_size){
			throw new IllegalArgumentException("shortAuthenticationStringSize,iv_size and tag_size must be a positive number!");
		}

		try{
			X9ECParameters ecP = CustomNamedCurves.getByName(curve);
			org.bouncycastle.jce.spec.ECParameterSpec ecGenSpec = new org.bouncycastle.jce.spec.ECParameterSpec(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(), ecP.getSeed());
			this.provider = new BouncyCastleProvider();
			KeyPairGenerator g = KeyPairGenerator.getInstance(enc_algorithm, this.provider);
			this.random = new SecureRandom();
			g.initialize(ecGenSpec, this.random);
			this.encKeypair = g.generateKeyPair();

			if (this.encKeypair == null){
				throw new IllegalArgumentException("Unable to create new key pair!");
			}

			this.OOB = new byte[shortAuthenticationStringSize];
			this.random.nextBytes(this.OOB);
		} catch(NoSuchAlgorithmException nsa){
			throw new IllegalArgumentException("Algorithm is not supported!");
		} catch(InvalidAlgorithmParameterException iap){
			throw new IllegalArgumentException("Wrong parameter for algorithm!");
		}

		this.enc_algorithm = enc_algorithm;
		this.curve = curve;
		this.iv_size = iv_size;
		this.tag_size = tag_size;
	}

	/**
	* Merge out of band challange.
	* For e.g. short authentication string this would merge the two strings together, that Alice and Bob should have the same string.
	* partner string from the other party.
	*/
	public void mergeOOB(byte[] partner) throws IllegalArgumentException, IllegalStateException {
		if (this.OOB.length != partner.length){
			throw new IllegalArgumentException("ERROR out of band challange has not the same length!");
		}

		if (this.merged){
			throw new IllegalStateException("ERROR you already merged the OOB challanges! You can't merge them twice!");
		}

		for(int i = 0; i < this.OOB.length; i++){
			this.OOB[i] = (byte) ((int) this.OOB[i] ^ (int) partner[i]);
		}

		this.merged = true;
	}

	/**
	* Get encryption offset (iv_size + tag_size)
	*/
	public int getOffset(){
		return 2*(this.iv_size + this.tag_size);
	}

	/**
	* Returns out of band challange, if merged, otherwise null.
	*/
	public byte[] getOOB(){
		if (this.merged){
			return this.OOB;
		} else {
			return null;
		}
	}

	/**
	* Get merge state.
	*/
	public boolean getMergeStatus(){
		return this.merged;
	}

	/**
	* Create new commitment for protocol exchange.
	*/
	public void createCryptoCommitment() throws IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException {
		BCECPublicKey pk = (BCECPublicKey) (this.encKeypair.getPublic());
		int publicKeySize = pk.getQ().getEncoded(true).length - 1;
		byte[] message = new byte[publicKeySize + this.OOB.length];
		System.arraycopy(pk.getQ().getEncoded(true), 1, message, 0, publicKeySize);
		System.arraycopy(this.OOB, 0, message, publicKeySize, this.OOB.length);
		this.cc = new CryptoCommitmentObject(message);
	}

	/**
	* Get commitment object.
	*/
	public CryptoCommitmentObject getCryptoCommitment(){
		return this.cc;
	}

	/**
	* Open commitment and extract message to create shared secret.
	*/
	public void openCommitmentAndCreateSharedSecret(byte [] decommitment) throws IllegalArgumentException, InvalidKeyException, NoSuchAlgorithmException{
		this.cc.open(decommitment);

		try{
			BCECPublicKey mypk = (BCECPublicKey) (this.encKeypair.getPublic());
			int publicKeySize = mypk.getQ().getEncoded(true).length - 1;
			byte[] message =  this.cc.getOtherMessage();

			if (message.length != publicKeySize + this.OOB.length){
				throw new IllegalArgumentException("Message size is wrong!");
			}

			byte[] otherPK = new byte[publicKeySize + 1];

			//compressed encoding magic byte
			otherPK[0] = (byte) 0x02;
			byte[] otherOOB = new byte[this.OOB.length];
			System.arraycopy(message, 0, otherPK, 1, publicKeySize);
			System.arraycopy(message, publicKeySize, otherOOB, 0, otherOOB.length);
			X9ECParameters ecP = CustomNamedCurves.getByName(curve);
			org.bouncycastle.jce.spec.ECParameterSpec ecGenSpec = new org.bouncycastle.jce.spec.ECParameterSpec(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH());
			//ECNamedCurveParameterSpec ecP = ECNamedCurveTable.getParameterSpec(this.curve);
			ECPublicKeySpec pubKey = new ECPublicKeySpec(ecP.getCurve().decodePoint(otherPK), ecGenSpec);
			KeyFactory kf = KeyFactory.getInstance(this.enc_algorithm, new BouncyCastleProvider());
			ECPublicKey pk = (ECPublicKey) kf.generatePublic(pubKey);
			createSharedEncKey(pk);
			mergeOOB(otherOOB);
		} catch(NoSuchAlgorithmException nsa){
			throw new IllegalArgumentException("Algorithm is not supported!");
		} catch(InvalidKeySpecException iks){
			throw new IllegalArgumentException("Wrong parameter for algorithm!");
		}
	}

	/**
	* Performs ECDH
	*/
	public void createSharedEncKey(ECPublicKey key) throws IllegalArgumentException {
		try {
			X9ECParameters ecP = CustomNamedCurves.getByName(curve);
			ECDomainParameters ecdp = new ECDomainParameters(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH());
			ECPublicKeyParameters ecpkp = new ECPublicKeyParameters(key.getQ(), ecdp);
			BCECPrivateKey sk = (BCECPrivateKey) this.encKeypair.getPrivate();
			ECPrivateKeyParameters ecskp = new ECPrivateKeyParameters(sk.getD() , ecdp);
			ECDHCBasicAgreement ba = new ECDHCBasicAgreement();
			ba.init(ecskp);
			byte[] byteSharedSecret = ba.calculateAgreement(ecpkp).toByteArray();
			byte[] byteSharedSecretInner = new byte[byteSharedSecret.length/2];
			byte[] byteSharedSecretOuter = new byte[byteSharedSecret.length/2];
			System.arraycopy(byteSharedSecret, 0, byteSharedSecretInner, 0, byteSharedSecretInner.length);
			System.arraycopy(byteSharedSecret, byteSharedSecretInner.length, byteSharedSecretOuter, 0, byteSharedSecretOuter.length);
			this.sharedSecretOuter = new SecretKeySpec(byteSharedSecretOuter, "AES");
			this.sharedSecretInner = new SecretKeySpec(byteSharedSecretInner, "AES");
			this.has_symmetric_key = true;
			this.encInner = Cipher.getInstance("AES/GCM/NoPadding");
			this.encOuter = Cipher.getInstance("AES/GCM/NoPadding");
			this.decInner = Cipher.getInstance("AES/GCM/NoPadding");
			this.decOuter = Cipher.getInstance("AES/GCM/NoPadding");
		} catch(IllegalStateException is){
			throw new IllegalArgumentException("ERROR unable to create shared encryption key, wrong state!");
		} catch(NoSuchAlgorithmException nsa){
			throw new IllegalArgumentException("Encryption algorithm not found!");
		} catch (NoSuchPaddingException nsp){
			throw new IllegalArgumentException("Invalid padding algorithm!");
		} 
	}


	/**
	 * set a already known sharedsecret, instead of using commitment to create one
	 * */
	public void setSharedSecret(byte[] sharedSecret) throws IllegalArgumentException {
		if (sharedSecret.length != 32){
			throw new IllegalArgumentException("invalid sharedSecret-size; has to have a length of 32 bytes");
		}
		try {
			byte[] byteSharedSecretInner = new byte[sharedSecret.length/2];
			byte[] byteSharedSecretOuter = new byte[sharedSecret.length/2];
			System.arraycopy(sharedSecret, 0, byteSharedSecretInner, 0, byteSharedSecretInner.length);
			System.arraycopy(sharedSecret, byteSharedSecretInner.length, byteSharedSecretOuter, 0, byteSharedSecretOuter.length);
			this.sharedSecretOuter = new SecretKeySpec(byteSharedSecretOuter, "AES");
			this.sharedSecretInner = new SecretKeySpec(byteSharedSecretInner, "AES");
			this.has_symmetric_key = true;
			this.encInner = Cipher.getInstance("AES/GCM/NoPadding");
			this.encOuter = Cipher.getInstance("AES/GCM/NoPadding");
			this.decInner = Cipher.getInstance("AES/GCM/NoPadding");
			this.decOuter = Cipher.getInstance("AES/GCM/NoPadding");
		} catch(IllegalStateException is){
			throw new IllegalArgumentException("ERROR unable to create shared encryption key, wrong state!");
		} catch(NoSuchAlgorithmException nsa){
			throw new IllegalArgumentException("Encryption algorithm not found!");
		} catch (NoSuchPaddingException nsp){
			throw new IllegalArgumentException("Invalid padding algorithm!");
		}
		
	}


	/**
	* Encrypt data with AES-GCM mode.
	* Encrypt data array and returns ciphertext.
	*/

	public byte[] encrypt(byte [] data) throws IllegalStateException, IllegalArgumentException, IllegalBlockSizeException {
		if (!this.has_symmetric_key){
			throw new IllegalStateException("Have no symmetric key, you need to create a shared secret first!");
		}

		if (data == null){
			throw new IllegalArgumentException("No data found for encryption");
		}

		byte[] ivInner = new byte[this.iv_size];
		byte[] ivOuter = new byte[this.iv_size];
		byte[] inputOuter = null;
		byte[] encryptedInner = null;
		byte[] encryptedOuter = null;
		byte[] output = null;
		byte[] encryptData = null;
		this.random.nextBytes(ivInner);
		this.random.nextBytes(ivOuter);
		try {
			this.encInner.init(Cipher.ENCRYPT_MODE, this.sharedSecretInner, new GCMParameterSpec(this.tag_size * 8, ivInner));
			this.encOuter.init(Cipher.ENCRYPT_MODE, this.sharedSecretOuter, new GCMParameterSpec(this.tag_size * 8, ivOuter));
			encryptedInner = this.encInner.doFinal(data);
			inputOuter = new byte[this.iv_size + encryptedInner.length];
			System.arraycopy(ivInner, 0, inputOuter, 0, this.iv_size);
			System.arraycopy(encryptedInner, 0, inputOuter, this.iv_size, encryptedInner.length);
			encryptedOuter = this.encOuter.doFinal(inputOuter);
			output = new byte[this.iv_size + encryptedOuter.length];
			System.arraycopy(ivOuter, 0, output, 0, this.iv_size);
			System.arraycopy(encryptedOuter, 0, output, this.iv_size, encryptedOuter.length);
		} catch(AEADBadTagException abt){
			throw new IllegalArgumentException("Decryption exception? Impossible!");
		} catch(BadPaddingException bp){
			throw new IllegalArgumentException("Padding exception? Impossible!");
		} catch (IllegalStateException ise){
			throw new IllegalStateException("Wrong AES cipher use!");
		} catch (InvalidKeyException ik){
			ik.printStackTrace();
			throw new IllegalArgumentException("Invalid key for encryption!");
		} catch (InvalidAlgorithmParameterException iap){
			iap.printStackTrace();
			throw new IllegalArgumentException("Encryption parameters are wrong!");
		}

		return output;
	}

	/**
	* Decrypt data with AES-GCM mode.
	* Decrypt data array and return message, if tag was valid, otherwise null.
	*/

	public byte[] decrypt(byte [] data) throws IllegalStateException, IllegalArgumentException, IllegalBlockSizeException {
		if (!this.has_symmetric_key){
			throw new IllegalStateException("Have no symmetric key, you need to create a shared secret first!");
		}

		if (data.length <= this.iv_size + this.tag_size || data == null){
			throw new IllegalArgumentException("The data are too small for a ciphertext!");
		}

		byte[] ivOuter = new byte[this.iv_size];
		byte[] ivInner = new byte[this.iv_size];
		byte[] ciphertext = new byte[data.length - this.iv_size];
		System.arraycopy(data, 0, ivOuter, 0, this.iv_size);
		System.arraycopy(data, this.iv_size, ciphertext, 0, ciphertext.length);
		byte[] decryptInner = null;
		byte[] decryptData = null;
		byte[] decryptedOuter = null;
		try {
			this.decOuter.init(Cipher.DECRYPT_MODE, this.sharedSecretOuter, new GCMParameterSpec(this.tag_size * 8, ivOuter));
			decryptedOuter = this.decOuter.doFinal(ciphertext);
			decryptInner = new byte[decryptedOuter.length - this.iv_size];
			System.arraycopy(decryptedOuter, 0, ivInner, 0, this.iv_size);
			System.arraycopy(decryptedOuter, this.iv_size, decryptInner, 0, decryptedOuter.length - this.iv_size);
			this.decInner.init(Cipher.DECRYPT_MODE, this.sharedSecretInner, new GCMParameterSpec(this.tag_size * 8, ivInner));
			decryptData = this.decInner.doFinal(decryptInner);
		} catch (AEADBadTagException abt){
			return null;
		} catch (BadPaddingException bp){
			throw new IllegalArgumentException("Padding exception? Impossible!");
		} catch (IllegalStateException ibs){
			throw new IllegalStateException("Wrong AES cipher use!");
		} catch (InvalidKeyException ik){
			throw new IllegalArgumentException("Invalid key for decryption!");
		} catch (InvalidAlgorithmParameterException iap){
			throw new IllegalArgumentException("Decryption parameters are wrong!");
		}

		return decryptData;
	}
}
