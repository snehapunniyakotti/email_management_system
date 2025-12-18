package com.gmail.demo.backup;

import org.springframework.stereotype.Service;

import com.gmail.demo.config.MailConfiguration;
import com.gmail.demo.entity.EmailMetadata;
import com.gmail.demo.repository.EmailRepo;
import com.gmail.demo.service.imaps.GmailStoreService;
import com.gmail.demo.service.imaps.ImapBasicService;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.imap.IMAPFolder;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import jakarta.mail.Flags.Flag;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.internet.MimeMessage;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


//////////////////////////////// old idle service === now in sync service itself

@Service
public class ImapIdleService {

	private final MailConfiguration props;
	private final EmailRepo emailRepo;
	private final GmailStoreService gmailStoreService;
	private final ImapBasicService imapBasicService;
//	private final ImapSyncService imapSyncService;

	public ImapIdleService(MailConfiguration props, EmailRepo emailRepo, GmailStoreService gmailStoreService,
			ImapBasicService imapBasicService
//			,ImapSyncService imapSyncService
			) {
		this.props = props;
		this.emailRepo = emailRepo;
		this.gmailStoreService = gmailStoreService;
		this.imapBasicService = imapBasicService;
//		this.imapSyncService = imapSyncService;
	}

	private static final String[] CATEGORIES = { "category:personal", "category:promotions", "category:social",
			"category:updates", "category:forums" };

//	@PostConstruct
	public void startIdleListeners() throws Exception {
		// start one idle listener per folder
		for (String folder : imapBasicService.defaultFolders()) {
			new Thread(() -> idleLoop(folder), "imap-idle-" + folder).start();
		}

		// INBOX gets special category handling
		new Thread(() -> idleLoop("INBOX"), "imap-idle-INBOX").start();
	}

	private void idleLoop(String folderName) {
		while (true) {
			System.out.println(" idle loop : folderName ::: " + folderName);

			try (Store store = gmailStoreService.getStore()) {
				GmailFolder folder = (GmailFolder) store.getFolder(folderName);

				if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 || !folder.exists()) {
					return;
				}

				folder.open(Folder.READ_WRITE);

				// ---- New messages ----
				folder.addMessageCountListener(new MessageCountAdapter() {
					@Override
					public void messagesAdded(MessageCountEvent e) {
						System.out.println(" %%%%%%%%%%%%%%%%  addMessageCountListener idle : " + e);
						try {
							// tried new way to get sync email message
//							imapSyncService.syncFolder(folderName);
							
							// old method
							
							FetchProfile fp = new FetchProfile();
							fp.add(FetchProfile.Item.ENVELOPE);
							fp.add(FetchProfile.Item.FLAGS);
							fp.add(FetchProfile.Item.SIZE);
							fp.add(IMAPFolder.FetchProfileItem.INTERNALDATE);

							folder.fetch(e.getMessages(), fp);

							if (folderName.equals("INBOX")) {
								handleInboxCategories(folder, e.getMessages());
							} else {
								persistMessages((UIDFolder) folder, folderName, e.getMessages());
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

					@Override
					public void messagesRemoved(MessageCountEvent e) {
						System.out.println(" ****************** messagesRemoved idle : " + e);
						System.out.println(" %%%%%%%%%%%%%%  folderName  ::: " + folderName);
						try {
							UIDFolder uidFolder = (UIDFolder) folder;
							for (Message m : e.getMessages()) {
								long uid = uidFolder.getUID(m);
								List<EmailMetadata> emd = emailRepo.findByFolderAndUid(folderName, uid);
								if (!emd.isEmpty()) {
									emd.forEach((meta -> {
										meta.setDeleteFlag(true);
//										meta.setFolder("[Gmail]/Trash");
										emailRepo.save(meta);
									}));
								}
							}
							System.err.println("Messages expunged in folder %%%%%%%%%%%%%% " + folderName);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});

				// ---- Flag changes ----
				folder.addMessageChangedListener(event -> {
					System.out.println(" &&&&&&&&&&&&&&&&&&&&&& addMessageChangedListener idle : " + event);
					
					System.out.println("  Starred testinggg   of current folderr   :::: "+ folder);
					try {
						Message m = event.getMessage();
						UIDFolder uidFolder = (UIDFolder) folder;
						long uid = uidFolder.getUID(m);

						List<EmailMetadata> emd = emailRepo.findByFolderAndUid(folderName, uid);
						if (!emd.isEmpty()) {
							emd.forEach((meta -> {
								try {
									Flags flags = m.getFlags();
									meta.setRead(flags.contains(Flags.Flag.SEEN));
									meta.setStarred(flags.contains(Flags.Flag.FLAGGED));
									
									List<String> flagArr = new ArrayList<String>();
									
									if(flags.contains(Flag.SEEN)) flagArr.add("SEEN");
									if(flags.contains(Flag.USER)) flagArr.add("USER");
									if(flags.contains(Flag.RECENT)) flagArr.add("RECENT");
									if(flags.contains(Flag.FLAGGED)) flagArr.add("FLAGGED");
									if(flags.contains(Flag.DRAFT)) flagArr.add("DRAFT");
									if(flags.contains(Flag.DELETED)) flagArr.add("DELETED");
									if(flags.contains(Flag.ANSWERED)) flagArr.add("ANSWERED");

									
//									meta.setFlags(Arrays.toString(flags.getSystemFlags()));
									meta.setFlags(flagArr);
									emailRepo.save(meta);
									
									
									System.out.println(" Arrays.toString(flags.getSystemFlags())  :::: "
											+ Arrays.toString(flags.getSystemFlags()));
									System.err.println("Message flags updated: UID " + uid + " in " + folderName);
								} catch (Exception ex) {
									ex.printStackTrace();
								}
							}));
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});

				// ---- Idle forever ----
//				while (true) {
//					folder.idle();
				try {
//						System.out.println(" folder.isOpen() && store.isConnected()  : " + (folder.isOpen() )+" && "+ (store.isConnected()));
					while (folder.isOpen() && store.isConnected()) {
//		                	System.out.println("priting inside the while loop true ");
						System.out.println("Starting IDLE on " + folderName);
						folder.idle(); // this blocks until server closes it
					}
				} catch (FolderClosedException fce) {
					System.err.println("Folder closed by server, will reopen: " + folderName);
				} catch (IllegalStateException ise) {
					System.err.println("Folder was already closed, reconnecting: " + folderName);
				}

				// cleanup
				try {
					if (folder.isOpen()) {
						folder.close(false);
					}
				} catch (Exception ignore) {
				}

				// small backoff before retry
				sleep(5000);
//				}

			} catch (Exception ex) {
				ex.printStackTrace();
				sleep(5000); // backoff before retry
			}
		}
	}

	private void handleInboxCategories(GmailFolder folder, Message[] newMsgs) throws Exception {
		UIDFolder uidFolder = (UIDFolder) folder;

		for (String category : CATEGORIES) {
			GmailRawSearchTerm catTerm = new GmailRawSearchTerm(category);
			Message[] catMsgs = folder.search(catTerm, newMsgs);

			if (catMsgs.length > 0) {
				String categoryFolderName = "INBOX/" + category.replace("category:", "");
				persistMessages(uidFolder, categoryFolderName, catMsgs);
			}
		}
	}

	private void persistMessages(UIDFolder folder, String folderName, Message[] msgs) throws Exception {
		for (Message m : msgs) {
			long uid = folder.getUID(m);
			if (emailRepo.existsByFolderAndUid(folderName, uid))
				continue;

			Flags flags = m.getFlags();
			boolean isRead = flags.contains(Flags.Flag.SEEN);
			boolean isStarred = flags.contains(Flags.Flag.FLAGGED);
			
			List<String> flagArr = new ArrayList<String>();
			
			if(flags.contains(Flag.SEEN)) flagArr.add("SEEN");
			if(flags.contains(Flag.USER)) flagArr.add("USER");
			if(flags.contains(Flag.RECENT)) flagArr.add("RECENT");
			if(flags.contains(Flag.FLAGGED)) flagArr.add("FLAGGED");
			if(flags.contains(Flag.DRAFT)) flagArr.add("DRAFT");
			if(flags.contains(Flag.DELETED)) flagArr.add("DELETED");
			if(flags.contains(Flag.ANSWERED)) flagArr.add("ANSWERED");

			String from = Optional.ofNullable(m.getFrom()).filter(a -> a.length > 0).map(a -> a[0].toString())
					.orElse(null);

			String to = Optional.ofNullable(m.getRecipients(Message.RecipientType.TO)).map(Arrays::stream)
					.orElseGet(Stream::empty).map(Address::toString).collect(Collectors.joining(", "));

			EmailMetadata meta = new EmailMetadata();
			meta.setFolder(folderName);
			meta.setUid(uid);
			meta.setUidValidity(folder.getUIDValidity());
			meta.setMessageId(((MimeMessage) m).getMessageID());
			meta.setSubject(m.getSubject());
			meta.setFromAddr(from);
			meta.setToAddr(to);
			meta.setSentAt(m.getSentDate());
//			meta.setFlags(Arrays.toString(m.getFlags().getSystemFlags()));
			meta.setFlags(flagArr);
			meta.setSizeBytes(m.getSize());
			meta.setSnippet(null);
			meta.setHasAttachments(false);
			meta.setBodyCached(false);
			meta.setStarred(isStarred);
			meta.setRead(isRead);
			meta.setDeleteFlag(false);

			emailRepo.save(meta);

			System.out.println(" persist message in idle call made on : folder " + folder);
		}
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ignored) {
		}
	}

	/// old logic for sync only new messages and have bug in thread in folder wise

//	@PostConstruct
	public void startIdleLoop() {
		System.err.println("Inside post constructor : !!!!!!!!!");
		new Thread(this::runLoop, "imap-idle-loop").start();
	}

	private void runLoop() {
		while (true) {
			try {
				System.err.println("callling idleOnce !!!!!!!!!!!!!!!!!!!!!!!!");
				for (String folder : imapBasicService.defaultFolders()) {
					System.err.println("callling idleOnce folder :::::  " + folder);
					idleOnce(folder);
//					syncFolder(folder);
				}
			} catch (Exception e) {
				e.printStackTrace();
				sleep(2000);
			} // backoff and retry
		}
	}

	/// need to update for all folder idle auto update
	private void idleOnce(String folder) throws Exception {
		System.out.println(" idleOnce folder-wise  call cur folder : " + folder);
		Properties p = new Properties();
		p.put("mail.store.protocol", "imaps");
		p.put("mail.imap.ssl.enable", "true");
		p.put("mail.imap.timeout", "5000");

		Session session = Session.getInstance(p);
		try (Store store = session.getStore("imaps")) {
			store.connect(props.getImapHost(), props.getImapUsername(), props.getImapPassword());
//			IMAPFolder inbox = (IMAPFolder) store.getFolder(props.getFolder());
			IMAPFolder inbox = (IMAPFolder) store.getFolder(folder);
			inbox.open(Folder.READ_ONLY);

			inbox.addMessageCountListener(new MessageCountAdapter() {
				@Override
				public void messagesAdded(MessageCountEvent e) {
					try {
						FetchProfile fp = new FetchProfile();
						fp.add(FetchProfile.Item.ENVELOPE);
						fp.add(FetchProfile.Item.FLAGS);
						fp.add(FetchProfile.Item.SIZE);
						fp.add(IMAPFolder.FetchProfileItem.INTERNALDATE);
						inbox.fetch(e.getMessages(), fp);

						for (Message m : e.getMessages()) {
							long uid = inbox.getUID(m);
							if (emailRepo.existsByFolderAndUid(props.getFolder(), uid))
								continue;

							// getting flags
							Flags flags = m.getFlags();
							boolean isRead = flags.contains(Flag.SEEN);
							boolean isStarred = flags.contains(Flag.FLAGGED);
							
							List<String> flagArr = new ArrayList<String>();
							
							if(flags.contains(Flag.SEEN)) flagArr.add("SEEN");
							if(flags.contains(Flag.USER)) flagArr.add("USER");
							if(flags.contains(Flag.RECENT)) flagArr.add("RECENT");
							if(flags.contains(Flag.FLAGGED)) flagArr.add("FLAGGED");
							if(flags.contains(Flag.DRAFT)) flagArr.add("DRAFT");
							if(flags.contains(Flag.DELETED)) flagArr.add("DELETED");
							if(flags.contains(Flag.ANSWERED)) flagArr.add("ANSWERED");


							EmailMetadata meta = new EmailMetadata();
							meta.setFolder(props.getFolder());
							meta.setUid(uid);
							meta.setUidValidity(inbox.getUIDValidity());
							meta.setMessageId(((MimeMessage) m).getMessageID());
							meta.setSubject(m.getSubject());
							meta.setFromAddr(
									m.getFrom() != null && m.getFrom().length > 0 ? m.getFrom()[0].toString() : null);
							meta.setToAddr(null);
							meta.setSentAt(m.getSentDate());
//							meta.setFlags(Arrays.toString(m.getFlags().getSystemFlags()));
							meta.setFlags(flagArr);
							meta.setSizeBytes(m.getSize());
							meta.setSnippet(null);
							meta.setHasAttachments(false);
							meta.setBodyCached(false);
							meta.setStarred(isStarred);
							meta.setRead(isRead);
							meta.setDeleteFlag(false);
							emailRepo.save(meta);
							System.err.println(" new emails persist !!!!!!!! ");
						}
					} catch (Exception ignore) {
					}
				}
			});

			// Block and listen; server will periodically wake it up
			while (true) {
				System.err.println("pritning inside while loopppppppppp !!!!!!!!!!!!!!");
				inbox.idle();
			}
		}
	}

}
