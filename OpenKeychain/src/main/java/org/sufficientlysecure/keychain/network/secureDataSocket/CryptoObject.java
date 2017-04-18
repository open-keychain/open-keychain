//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;

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
	private SecretKeySpec sharedSecretSecond = null;
	private SecretKeySpec sharedSecretFirst = null;
	private Cipher enc = null;
	private Cipher dec = null;
	private String enc_algorithm = "";
	private String curve = "";
	private byte[] OOB = null;
	private boolean merged = false;
	private boolean has_symmetric_key = false;
	private SecureRandom random = null;
	private int iv_size = 16;
	private int tag_size = 32;

	/** 
	* Constructor.
	* Create a new CryptoObject with encryption asymmetric elliptic curve25519 encryption keypair 
	* Short authentication byte length is 3 byte.
	*/
	public CryptoObject() throws CryptoSocketException {
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
	public CryptoObject(String curve, String enc_algorithm, int shortAuthenticationStringSize, int iv_size, int tag_size) throws CryptoSocketException{
		if (0 >= shortAuthenticationStringSize || 0 >= iv_size || 0 >= tag_size){
			throw new CryptoSocketException("shortAuthenticationStringSize,iv_size and tag_size must be a positive number!");
		}

		this.enc_algorithm = enc_algorithm;
		this.curve = curve;
		this.iv_size = iv_size;
		this.tag_size = tag_size;
	}


	/**
	* Get encryption offset (iv_size + tag_size)
	*/
	public int getOffset(){
		return this.iv_size + this.tag_size;
	}

	
	/**
	 * set a already known sharedsecret, instead of using commitment to create one
	 * */
	public void setSharedSecret(byte[] sharedSecret) throws CryptoSocketException {
		if (sharedSecret.length != 32){
			throw new CryptoSocketException("invalid sharedSecret-size; has to have a length of 32 bytes");
		}
		try {
			byte[] byteSharedSecretSecond = new byte[sharedSecret.length/2];
			byte[] byteSharedSecretFirst = new byte[sharedSecret.length/2];
			System.arraycopy(sharedSecret, 0, byteSharedSecretSecond, 0, byteSharedSecretSecond.length);
			System.arraycopy(sharedSecret, byteSharedSecretSecond.length, byteSharedSecretFirst, 0, byteSharedSecretFirst.length);
			this.sharedSecretFirst = new SecretKeySpec(byteSharedSecretFirst, "AES");
			this.sharedSecretSecond = new SecretKeySpec(byteSharedSecretSecond, "AES");
			this.has_symmetric_key = true;
			this.enc = Cipher.getInstance("AES/GCM/NoPadding");
			this.dec = Cipher.getInstance("AES/GCM/NoPadding");
		} catch(IllegalStateException is){
			throw new CryptoSocketException("unable to create shared encryption key, wrong state!");
		} catch(NoSuchAlgorithmException nsa){
			throw new CryptoSocketException("Encryption algorithm not found!");
		} catch (NoSuchPaddingException nsp){
			throw new CryptoSocketException("Invalid padding algorithm!");
		}
		
	}


	/**
	* Encrypt data with AES-GCM mode.
	* Encrypt data array and returns ciphertext.
	*/

	public byte[] encrypt(byte [] data) throws IllegalStateException, CryptoSocketException, IllegalBlockSizeException {
		if (!this.has_symmetric_key){
			throw new IllegalStateException("Have no symmetric key, you need to create a shared secret first!");
		}

		if (data == null){
			throw new CryptoSocketException("No data found for encryption");
		}

		byte[] iv = new byte[this.iv_size];
		byte[] output = null;
		byte[] encryptData = null;
		this.random.nextBytes(iv);
		try {
			this.enc.init(Cipher.ENCRYPT_MODE, this.sharedSecretFirst, new GCMParameterSpec(this.tag_size * 8, iv));
			encryptData = this.enc.doFinal(data);
			output = new byte[this.iv_size + encryptData.length];
			System.arraycopy(iv, 0, output, 0, this.iv_size);
			System.arraycopy(encryptData, 0, output, this.iv_size, encryptData.length);
		} catch(AEADBadTagException abt){
			throw new CryptoSocketException("Decryption exception? Impossible!");
		} catch(BadPaddingException bp){
			throw new CryptoSocketException("Padding exception? Impossible!");
		} catch (IllegalStateException ise){
			throw new IllegalStateException("Wrong AES cipher use!");
		} catch (InvalidKeyException ik){
			ik.printStackTrace();
			throw new CryptoSocketException("Invalid key for encryption!");
		} catch (InvalidAlgorithmParameterException iap){
			iap.printStackTrace();
			throw new CryptoSocketException("Encryption parameters are wrong!");
		}

		return output;
	}

	/**
	* Decrypt data with AES-GCM mode.
	* Decrypt data array and return message, if tag was valid, otherwise null.
	*/

	public byte[] decrypt(byte [] data) throws IllegalStateException, CryptoSocketException, IllegalBlockSizeException {
		if (!this.has_symmetric_key){
			throw new IllegalStateException("Have no symmetric key, you need to create a shared secret first!");
		}

		if (data.length <= this.iv_size + this.tag_size || data == null){
			throw new CryptoSocketException("The data are too small for a ciphertext!");
		}

		byte[] iv = new byte[this.iv_size];
		byte[] ciphertext = new byte[data.length - this.iv_size];
		System.arraycopy(data, 0, iv, 0, this.iv_size);
		System.arraycopy(data, this.iv_size, ciphertext, 0, ciphertext.length);
		byte[] decryptData = null;
		try {
			this.dec.init(Cipher.DECRYPT_MODE, this.sharedSecretFirst, new GCMParameterSpec(this.tag_size * 8, iv));
			decryptData = this.dec.doFinal(ciphertext);
		} catch (AEADBadTagException abt){
			return null;
		} catch (BadPaddingException bp){
			throw new CryptoSocketException("Padding exception? Impossible!");
		} catch (IllegalStateException ibs){
			throw new IllegalStateException("Wrong AES cipher use!");
		} catch (InvalidKeyException ik){
			throw new CryptoSocketException("Invalid key for decryption!");
		} catch (InvalidAlgorithmParameterException iap){
			throw new CryptoSocketException("Decryption parameters are wrong!");
		}

		return decryptData;
	}
}
