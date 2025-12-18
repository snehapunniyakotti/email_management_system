package com.gmail.demo.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class MailConfiguration {

	@Value("${mail.imaps.host}")
	private String imapHost;

	@Value("${mail.imaps.port}")
	private String imapPort;

	@Value("${mail.imaps.username}")
	private String imapUsername;

	@Value("${mail.imaps.password}")
	private String imapPassword;

	@Value("${mail.imap.ssl.enable}")
	private boolean sslEnabled;

	@Value("${mail.imap.connection-timeout:5000}")
	private int connTimeoutMs;

	@Value("${mail.imap.read-timeout:15000}")
	private int readTimeoutMs;

	@Value("${mail.imap.folder:INBOX}")
	private String folder;
	
	@Value("${mail.store.protocol.gimaps}")
	private String gimaps;

	@Value("${mail.store.protocol}")
	private String imaps;

	@Bean
	public Session mailSession() {
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", imaps);
		props.setProperty("mail.imaps.host", imapHost);
		props.setProperty("mail.imaps.port", imapPort);
		props.put("mail.imaps.ssl.enable", sslEnabled);
		return Session.getInstance(props);
	}

	@Bean
	public Store mailStore(Session session) throws NoSuchProviderException {
		Store store = session.getStore("imaps");
		return store;
	}

	public String getImapHost() {
		return imapHost;
	}

	public String getImapPort() {
		return imapPort;
	}

	public String getImapUsername() {
		return imapUsername;
	}

	public String getImapPassword() {
		return imapPassword;
	}

	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public int getConnTimeoutMs() {
		return connTimeoutMs;
	}

	public int getReadTimeoutMs() {
		return readTimeoutMs;
	}

	public String getFolder() {
		return folder;
	}
	
	
	

}