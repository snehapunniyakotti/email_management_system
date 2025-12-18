package com.gmail.demo.service.imaps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.springframework.stereotype.Service;

import com.gmail.demo.config.MailConfiguration;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

@Service
public class ImapBasicService {

	private final MailConfiguration props;

	public ImapBasicService(MailConfiguration props) {
		this.props = props;
	}

	protected Properties sessionProps() {
		Properties p = new Properties();
		p.put("mail.store.protocol", "gimaps");
		p.put("mail.imap.ssl.enable", String.valueOf(props.isSslEnabled()));
		p.put("mail.imap.connectiontimeout", String.valueOf(props.getConnTimeoutMs()));
		p.put("mail.imap.timeout", String.valueOf(props.getReadTimeoutMs()));
		return p;
	}

	protected Properties sessionPropsforGImaps() {
		Properties p = new Properties();
		p.put("mail.store.protocol", "gimaps");
		p.put("mail.gimaps.host", "imap.gmail.com");
		p.put("mail.gimaps.port", "993");
		p.put("mail.gimaps.ssl.enable", "true");
		return p;
	}

	protected Store getStore(Session s) throws MessagingException {
		Store store = s.getStore("gimaps");
		store.connect(props.getImapHost(), props.getImapUsername(), props.getImapPassword());
		return store;
	}

	protected Store getStoreforGImaps(Session s) throws MessagingException {
		Store store = s.getStore("gimaps");
		store.connect(props.getImapHost(), props.getImapUsername(), props.getImapPassword());
		return store;
	}

	public List<String> availableLabels(List<String> set) {
		List<String> customLabels = new ArrayList<String>();
		for (String folderName : set) {
			if (!folderName.startsWith("[Gmail]") && !folderName.equals("INBOX")) {
				customLabels.add(folderName);
			}
		}
		System.err.println("printing the custom labels : " + customLabels);
		return customLabels;

	}

	public List<String> defaultFolders() throws InterruptedException  {

		List<String> allFolderList = new ArrayList<String>();
		try {
			Session session = Session.getInstance(sessionProps());
			Store store = getStore(session);

			// old way which don't include all the parent folder
//		List<String> availableFolders = retrieveAvailableFoldersUsingStore(store);

			Folder[] folders = store.getDefaultFolder().list("*");
			for (Folder f : folders) {
				allFolderList.add(f.getFullName());
			}

			store.close();
		} catch (Exception e) {
			
			System.err.println(e.getMessage());
			Thread.sleep(2000);
		}
//		return availableFolders;
		return allFolderList;
	}

	public void getAllInbox() throws MessagingException {
		Session session = Session.getInstance(sessionProps());
		Store store = getStore(session);
		Folder inbox = store.getFolder("INBOX");
		inbox.open(Folder.READ_ONLY);
		Message[] messages = inbox.getMessages();

		FetchProfile fp = new FetchProfile();
		fp.add(FetchProfile.Item.ENVELOPE);
		fp.add("X-GM-LABELS");

		inbox.fetch(messages, fp);

		for (Message message : messages) {
			String[] labels = message.getHeader("X-GM-LABELS");

			System.out.println(" printing the category ::::::: " + labels);
			System.out.println(Arrays.toString(labels));

			if (labels != null) {
				for (String label : labels) {
					if (label.contains("CATEGORY_PROMOTIONS")) {
						System.out.println("This message is in Promotions tab.");
					} else if (label.contains("CATEGORY_SOCIAL")) {
						System.out.println("This message is in Social tab.");
					} else if (label.contains("CATEGORY_PERSONAL")) {
						System.out.println("This message is in Primary tab.");
					}
				}
			}
		}
	}

}
