package com.gmail.demo.service.imaps;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.stereotype.Service;

import com.gmail.demo.config.MailConfiguration;
import com.sun.mail.gimap.GmailFolder;

import jakarta.annotation.PreDestroy;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

@Service
public class GmailStoreService {

	private final MailConfiguration mailConfig;

	public GmailStoreService(MailConfiguration mailConfig) {
		this.mailConfig = mailConfig;
	}

	private Store store;
	private Store store2;
//	private final List<Store> openStores = new ArrayList<>();
	private Map<String, GmailFolder> folderMap = new HashMap<String, GmailFolder>();

	/**
	 * Returns a singleton Store instance (IMAP connection). If it's already
	 * connected, reuse it. Otherwise, reconnect.
	 */
	public synchronized Store getStore() throws MessagingException {
		if (store == null || !store.isConnected()) {
			connect();
		}
		return store;
	}

	public GmailFolder getFolderConnection(String folder) throws MessagingException {

		System.out.println(" folderMap ::::: " + folderMap.toString());

		if (folderMap.containsKey(folder)) {
			GmailFolder GF = folderMap.get(folder);
			if (GF == null || !GF.isOpen()) {
				GF.open(Folder.READ_WRITE);
				folderMap.put(folder, GF);
			}
			System.err.println(" used folder in folderMap ------");
			return GF;
		}

		GmailFolder gfolder = (GmailFolder) store.getFolder(folder);
		// // --- check folder holds messages ---
		if ((gfolder.getType() & Folder.HOLDS_MESSAGES) == 0 || !gfolder.exists()) {
			return null;
		}

		gfolder.open(Folder.READ_WRITE);
		folderMap.put(folder, gfolder);

		System.err.println(" folder openned for first time ------");
		return gfolder;
	}

	private void connect() throws MessagingException {
		Properties props = new Properties();
		props.put("mail.store.protocol", "gimaps");
		props.put("mail.imaps.host", String.valueOf(mailConfig.getImapHost()));
		props.put("mail.imaps.port", String.valueOf(mailConfig.getImapPort()));
		props.put("mail.imaps.ssl.enable", String.valueOf(mailConfig.isSslEnabled()));

		Session session = Session.getInstance(props);
		store = session.getStore("gimaps");
		store.connect(mailConfig.getImapHost(), mailConfig.getImapUsername(), mailConfig.getImapPassword());

	}

	public synchronized Store getNewStore() throws MessagingException {

		if (store2 == null || !store2.isConnected()) {
			newConnect();
		}
		return store2;
	}

	private void newConnect() throws MessagingException {
		Properties props = new Properties();
		props.put("mail.store.protocol", "gimaps");
		props.put("mail.imaps.host", String.valueOf(mailConfig.getImapHost()));
		props.put("mail.imaps.port", String.valueOf(mailConfig.getImapPort()));
		props.put("mail.imaps.ssl.enable", String.valueOf(mailConfig.isSslEnabled()));

		Session session = Session.getInstance(props);
		store2 = session.getStore("gimaps");
		store2.connect(mailConfig.getImapHost(), mailConfig.getImapUsername(), mailConfig.getImapPassword());

//		System.out.println("Gmail IMAP connected!");

//		return store2;
	}

	/**
	 * @PreDestroy This method is called automatically by Spring before the bean is
	 *             destroyed. It ensures all open Store connections are closed
	 *             properly on application shutdown.
	 */

	@PreDestroy
	public void close() {
		try {
			if (store != null && store.isConnected()) {
				store.close();
				System.out.println(" Gmail IMAP store1 connection closed.");
			}
			if (store2 != null && store2.isConnected()) {
				store2.close();
				System.out.println(" Gmail IMAP store2 connection closed.");
			}

			folderMap.forEach((name, folder) -> {
				if (folder != null) {
					try {
						folder.close();
					} catch (Exception e) {
						System.err.println("Error closing folder: " + name);
						e.printStackTrace();
					}
				}
			});

		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

//    @PreDestroy
//    public void closeAllStores() {
//        System.out.println("Application shutting down. Closing all IMAP stores.");
//        for (Store store : openStores) {
//            try {
//                if (store != null && store.isConnected()) {
//                    store.close();
//                    System.out.println("Store closed successfully.");
//                }
//            } catch (MessagingException e) {
//                System.err.println("Error closing store: " + e.getMessage());
//            }
//        }
//        openStores.clear(); // Clear the list after closing
//    }
}
