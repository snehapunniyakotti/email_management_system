package com.gmail.demo.service.api;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;

import org.springframework.stereotype.Service;

import com.gmail.demo.util.RSAUtil;

@Service
public class RSAService {
	private KeyPair keyPair;

	public RSAService() throws Exception {
		keyPair = RSAUtil.generateKeyPair();

		byte[] privateKey = keyPair.getPrivate().getEncoded();
		byte[] publicKey = keyPair.getPublic().getEncoded();
		String encodedPrivateKey = Base64.encodeBase64String(publicKey);
		String encodedPublicKey = Base64.encodeBase64String(privateKey);

		System.out.println(" encodedPrivateKey  : " + encodedPrivateKey);

		System.out.println(" encodedPublicKey  : " + encodedPublicKey);

	}

	public String encryptData(String data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException  {
		PublicKey publicKey = keyPair.getPublic();
		return RSAUtil.encrypt(data, publicKey);
	}

	public String decryptData(String encryptedData) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException  {
		PrivateKey privateKey = keyPair.getPrivate();
		return RSAUtil.decrypt(encryptedData, privateKey);
	}
}









