package com.gmail.demo.backup;

public class ImapInitSyncbackup {

//
//	package com.gmail.demo.service.imaps;
//
//	import org.springframework.beans.factory.annotation.Autowired;
//	import org.springframework.boot.CommandLineRunner;
//	import org.springframework.scheduling.TaskScheduler;
//	import org.springframework.scheduling.annotation.Scheduled;
//	import org.springframework.scheduling.support.CronTrigger;
//	import org.springframework.stereotype.Service;
//
//	import com.gmail.demo.config.MailConfiguration;
//	import com.gmail.demo.entity.EmailBody;
//	import com.gmail.demo.entity.EmailMetadata;
//	import com.gmail.demo.entity.SyncState;
//	import com.gmail.demo.handler.OAuth2LoginSuccessHandler;
//	import com.gmail.demo.repository.EmailRepo;
//	import com.gmail.demo.repository.SyncStateRepo;
//	import com.gmail.demo.scheduler.EmailScheduler;
//	import com.gmail.demo.scheduler.ImapSyncScheduler;
//	import com.gmail.demo.scheduler.SnoozedEmailScheduler;
//	import com.gmail.demo.util.MailUtil;
//	import com.sun.mail.gimap.GmailFolder;
//	import com.sun.mail.gimap.GmailMessage;
//	import com.sun.mail.gimap.GmailRawSearchTerm;
//	import com.sun.mail.imap.IMAPFolder;
//	import jakarta.annotation.PostConstruct;
//	import jakarta.mail.*;
//	import jakarta.mail.Flags.Flag;
//	import jakarta.mail.event.MessageChangedEvent;
//	import jakarta.mail.event.MessageCountAdapter;
//	import jakarta.mail.event.MessageCountEvent;
//	import jakarta.mail.internet.MimeMessage;
//	import jakarta.mail.internet.MimeMultipart;
//
//	import org.springframework.transaction.annotation.Transactional;
//
//	import java.io.File;
//	import java.io.FileOutputStream;
//	import java.io.InputStream;
//	import java.util.*;
//	import java.util.concurrent.ConcurrentHashMap;
//	import java.util.concurrent.Executors;
//	import java.util.concurrent.TimeUnit;
//	import java.util.stream.Collectors;
//	import java.util.stream.Stream;
//
//	@Service
//	public class ImapInitSyncService  {
//
//		private final OAuth2LoginSuccessHandler OAuth2LoginSuccessHandler;
//
//		private final MailConfiguration props;
//		private final EmailRepo emailRepo;
//		private final SyncStateRepo syncRepo;
//		private final ImapBasicService imapBasicService;
//		private final GmailStoreService gmailStoreService;
//		private final TaskScheduler taskScheduler;
//		private final EmailScheduler emailScheduler;
//		private final SnoozedEmailScheduler snoozedEmailScheduler;
//		private final PersistMailMessage persistMailMessage;
//		private final ImapSyncScheduler imapSyncScheduler;
//
//		private static final int BATCH = 300;
//
//		private boolean isInitCompleted = false;
//
//		private final Map<String, ThreadListenerControl> activeListeners = new ConcurrentHashMap<>();
//
//		public ImapInitSyncService(MailConfiguration props, EmailRepo emailRepo, SyncStateRepo syncRepo,
//				ImapBasicService imapBasicService, GmailStoreService gmailStoreService,
//				OAuth2LoginSuccessHandler OAuth2LoginSuccessHandler, TaskScheduler taskScheduler,
//				EmailScheduler emailScheduler, SnoozedEmailScheduler snoozedEmailScheduler,
//				PersistMailMessage persistMailMessage,ImapSyncScheduler imapSyncScheduler) {
//			this.props = props;
//			this.emailRepo = emailRepo;
//			this.syncRepo = syncRepo;
//			this.imapBasicService = imapBasicService;
//			this.gmailStoreService = gmailStoreService;
//			this.OAuth2LoginSuccessHandler = OAuth2LoginSuccessHandler;
//			this.taskScheduler = taskScheduler;
//			this.emailScheduler = emailScheduler;
//			this.snoozedEmailScheduler = snoozedEmailScheduler;
//			this.persistMailMessage = persistMailMessage;
//			this.imapSyncScheduler = imapSyncScheduler;
//		}
//
////		@Autowired
////		public ImapIdleService imapIdleService;
//
//		private static final String[] CATEGORIES = { "category:personal", "category:promotions", "category:social",
//				"category:updates", "category:forums" };
//
//		@PostConstruct // need to uncomment this for intially loading emails
//		public void initialKickoff() throws Exception {
//			// kick light initial sync asynchronously
//			new Thread(() -> {
//				try {
////					imapBasicService.getCategorizedEmails();
//					List<String> defaultFolders = imapBasicService.defaultFolders();
//					System.err.println("printing inside the initialKickofff !!!!!!!!!!!!!!!!!!!");
//					System.out.println("printing the list of folders  : " + defaultFolders);
//
//					for (String folder : defaultFolders) {
//						syncFolder(folder);
//					}
//					syncFolder("INBOXWC"); // INBOX mail without category
//
//					isInitCompleted = true;
//					// calling the idle listener from here becoz of folder , store close exceptions
//					// and too many connection exception
////					startIdleListeners();
//
//					// multi threading with upding thread based on folder
////					Executors.newSingleThreadScheduledExecutor()
////	                .scheduleAtFixedRate(this::refreshListeners, 0, 30, TimeUnit.SECONDS);
//					runtask();
//
//				} catch (Exception ignored) {
//					System.err.println(ignored.getMessage());
//				}
//			}, "imap-initial-sync").start();
//
//		}
//
////		@Override
//		public void runtask() {
//			System.out.println("Starting scheduled job after init...");
//
//			taskScheduler.schedule(this::syncMails, new CronTrigger("0 */2 * * * *") // every 2 minutes
//			);
//			taskScheduler.schedule(emailScheduler::sendScheduledEmails, new CronTrigger("0 */1 * * * *") // every 1 minutes
//					); // :: means runnable
//			taskScheduler.schedule(() -> {
//				try {
//					snoozedEmailScheduler.markSnoozedByMessageId();
//				} catch (Exception e) {
//					System.out.println(" Error in snooze mail scheduler ...");
//					e.printStackTrace();
//				}
//			},  new CronTrigger("0 */1 * * * *"));
//		}
//
////		@Scheduled(fixedRate = 120000)
//		public void syncMails() {
//			if (!isInitCompleted) {
//				return;
//			}
//			System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//			System.out.println("syncMails :::::::::::  is called ");
//			System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//			try {
//				List<String> folders = imapBasicService.defaultFolders();
//				for (String folder : folders) {
//					imapSyncScheduler.syncfolderMails(folder);
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	   
//
////		public void startIdleListeners() throws Exception {
////			// start one idle listener per folder
////			for (String folder : imapBasicService.defaultFolders()) {
////				new Thread(() -> idleLoop(folder), "imap-idle-" + folder).start();
////			}
//	//
////			// INBOX gets special category handling
////			new Thread(() -> idleLoop("INBOX"), "imap-idle-INBOX").start();
////		}
//
////		private void refreshListeners() {
////	        try {
////	            List<String> currentFolders = new ArrayList<>(imapBasicService.defaultFolders());
////	            currentFolders.add("INBOX"); // ensure INBOX always present
//	//
////	            // 1) stop threads for removed folders
////	            for (String folder : new HashSet<>(activeListeners.keySet())) {
////	                if (!currentFolders.contains(folder)) {
////	                    System.out.println("Stopping listener for: " + folder);
////	                    activeListeners.get(folder).stop();
////	                    activeListeners.remove(folder);
////	                }
////	            }
//	//
////	            // 2) start threads for new folders
////	            for (String folder : currentFolders) {
////	                if (!activeListeners.containsKey(folder) && !folder.equals(MailUtil.Gmail)) {
////	                    System.out.println("Starting listener for: " + folder);
////	                    ThreadListenerControl control = new ThreadListenerControl();
////	                    activeListeners.put(folder, control);
//	//
////	                    Thread t = new Thread(() -> idleLoop(folder, control), "imap-idle-" + folder);
////	                    t.start();
////	                }
////	            }
//	//
////	        } catch (Exception e) {
////	            e.printStackTrace();
////	        }
////	    }
//
//		@Transactional // db data transaction , if error occur it will rollback the saved transaction
//		public void syncFolder(String folderName) throws Exception {
//
//			try (Store store = gmailStoreService.getStore()) {
//
//				// in GmailFolder we can get category-wise email also
//				GmailFolder folder = (GmailFolder) store.getFolder(folderName.equals("INBOXWC") ? "INBOX" : folderName);
//
//				// Skip folders that cannot hold messages (e.g., [Gmail] root) and if folder not
//				// exist
//				if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 && folder.exists()) {
//					return;
//				}
//				folder.open(Folder.READ_ONLY);
//
//				System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! intial sync folder name : " + folderName);
//				if (folderName.equals("INBOX")) {
//
//					for (String category : CATEGORIES) {
//
//						System.out.println("!!!!!!!!!!!!!!!!!!!!!!  inside the category : " + category);
//						String categoryFolderName = folderName + "/" + category.replace("category:", "");
//						long uidValidity = folder.getUIDValidity();
//						long uidNext = folder.getUIDNext();
//
//						System.out.println("priting the uidNext ::::  " + uidNext);
//
//						SyncState state = syncRepo.findById(categoryFolderName).orElseGet(() -> {
//							SyncState s = new SyncState();
//							s.setFolder(categoryFolderName);
//							s.setUidValidity(uidValidity);
//							s.setLastSyncedUid(0);
//							return s;
//						});
//
//						if (state.getUidValidity() != uidValidity) {
//							// UID space changed -> full reindex recommended. For demo, just reset pointer.
//							state.setUidValidity(uidValidity);
//							state.setLastSyncedUid(0);
//						}
//
//						long last = state.getLastSyncedUid(); // 0
//						long newest = uidNext - 1;
//						if (newest <= 0 || last >= newest) {
//							folder.close(false);
//							continue;
//						}
//
//						UIDFolder uidFolder = (UIDFolder) folder;
//						Message[] uidMsgs = uidFolder.getMessagesByUID(last + 1, newest);
//
////						System.err.println("priting the newest and last msgss :::::: " + newest + "  :::::: " + (last + 1));
//
//						GmailRawSearchTerm catTerm = new GmailRawSearchTerm(category);
//
//						Message[] msgs = folder.search(catTerm, uidMsgs);
//
////						if(msgs.length == 0) {
//						/*
//						 * calling recursively because ,if any new msg came in the name of folder name
//						 * 'INBOX' from IMAP it only checks for category type that's i'm triggering this
//						 * INBOX without folder key to get the new msgs without missing
//						 */
////							syncFolder("INBOXWC"); // INBOX mail without category
////						}
//
//						// Prepare FetchProfile once
//						FetchProfile fp = new FetchProfile();
//						fp.add(FetchProfile.Item.ENVELOPE);
//						fp.add(FetchProfile.Item.FLAGS);
//						fp.add(FetchProfile.Item.SIZE);
//						fp.add(IMAPFolder.FetchProfileItem.INTERNALDATE);
//
//						for (int i = 0; i < msgs.length; i += BATCH) {
//							int end = Math.min(i + BATCH, msgs.length);
//
////							System.out.println("printing the start(i) and end ::::  " + i + "  :::  " + end);
//
//							Message[] batch = Arrays.copyOfRange(msgs, i, end);
//
//							// Fetch metadata in bulk
//							folder.fetch(batch, fp);
//
//							// Persist batch
//							persistMailMessage.save(folder, categoryFolderName, batch);
//
//							// Update SyncState with highest UID in this batch
//							long lastUid = folder.getUID(batch[0]); // after reversing, [0] is newest
//							state.setLastSyncedUid(lastUid);
//							syncRepo.save(state);
//							Thread.sleep(30);
//						}
//
//					}
//
//				} else {
//					// some mails are not in category and directly present in inbox
//					String lFolderName = folderName.equals("INBOXWC") ? "INBOX" : folderName;
//
//					long uidValidity = folder.getUIDValidity();
//					long uidNext = folder.getUIDNext();
//
//					System.out.println("priting the uidNext ::::  " + uidNext);
//					SyncState state = syncRepo.findById(lFolderName).orElseGet(() -> {
//						SyncState s = new SyncState();
//						s.setFolder(lFolderName);
//						s.setUidValidity(uidValidity);
//						s.setLastSyncedUid(0);
//						return s;
//					});
//					if (state.getUidValidity() != uidValidity) {
//						// UID space changed -> full reindex recommended. For demo, just reset pointer.
//						state.setUidValidity(uidValidity);
//						state.setLastSyncedUid(0);
//					}
//					long last = state.getLastSyncedUid(); // 0
//					long newest = uidNext - 1;
//					if (newest <= 0 || last >= newest) {
//						folder.close(false);
//						return;
//					}
//
//					// ascending order (oldest to newest)
////				for (long start = last + 1; start <= newest; start += BATCH) {
////					long end = Math.min(start + BATCH - 1, newest);
////					Message[] msgs = folder.getMessagesByUID(start, end);
//	//
////					FetchProfile fp = new FetchProfile();
////					fp.add(FetchProfile.Item.ENVELOPE);
////					fp.add(FetchProfile.Item.FLAGS);
////					fp.add(FetchProfile.Item.SIZE);
////					fp.add(IMAPFolder.FetchProfileItem.INTERNALDATE);
////					folder.fetch(msgs, fp);
//	//
////					persistMetadata(folder, folderName, msgs);
//	//
////					state.setLastSyncedUid(end);
////					syncRepo.save(state);
//	//
////					// tiny pause helps avoid throttling
////					Thread.sleep(30);
////				}
//
//					/// descending order(newest to oldest)
//					for (long start = newest; start > last; start -= BATCH) {
//						long end = Math.max(start - BATCH + 1, last + 1);
//
//						// getMessagesByUID(start, end) expects start <= end, so swap if needed
//						Message[] msgs = folder.getMessagesByUID(end, start);
//
//						FetchProfile fp = new FetchProfile();
//						fp.add(FetchProfile.Item.ENVELOPE);
//						fp.add(FetchProfile.Item.FLAGS);
//						fp.add(FetchProfile.Item.SIZE);
//						fp.add(IMAPFolder.FetchProfileItem.INTERNALDATE);
//						fp.add(GmailFolder.FetchProfileItem.LABELS);
//						folder.fetch(msgs, fp);
//						// Reverse the array using two pointer approach , so the newest message in this
//						// batch comes first
//						for (int i = 0; i < msgs.length / 2; i++) {
//							// swap with temp var
//							Message tmp = msgs[i];
//							msgs[i] = msgs[msgs.length - 1 - i];
//							msgs[msgs.length - 1 - i] = tmp;
//						}
//
//						// using date to compare this and order in descending order.
////				    Arrays.sort(msgs, (m1, m2) -> {
////		                try {
////		                    return m2.getSentDate().compareTo(m1.getSentDate()); // Descending order
////		                } catch (MessagingException e) {
////		                    return 0;
////		                }
////		            });
//
//						persistMailMessage.save(folder, lFolderName, msgs);
//						// Update last synced UID to the max we've processed
//						state.setLastSyncedUid(start);
//						syncRepo.save(state);
//						Thread.sleep(30);
//					}
//				}
//				folder.close(false);
//			}
//		}
//
////		private void persistMetadata(IMAPFolder f, String folderName, Message[] msgs) throws Exception {
////			System.err.println("pritning insideeeeeeeeeeeeeeeeeeeeeee  the persistMetadat");
////			for (Message m : msgs) {
//	//
////				// getting flags
////				Flags flags = m.getFlags();
////				boolean isRead = flags.contains(Flag.SEEN);
////				boolean isStarred = flags.contains(Flag.FLAGGED);
//	//
////				List<String> flagArr = new ArrayList<String>();
//	//
////				if (flags.contains(Flag.SEEN))
////					flagArr.add("SEEN");
//////				System.out.println(" flags.contains(Flag.SEEN) "+flags.contains(Flag.SEEN));
////				if (flags.contains(Flag.USER))
////					flagArr.add("USER");
//////					System.out.println(" flags.contains(Flag.USER) "+flags.contains(Flag.USER));
////				if (flags.contains(Flag.RECENT))
////					flagArr.add("RECENT");
//////					System.out.println(" flags.contains(Flag.RECENT) "+flags.contains(Flag.RECENT));
////				if (flags.contains(Flag.FLAGGED))
////					flagArr.add("FLAGGED");
//////					System.out.println(" flags.contains(Flag.FLAGGED) "+flags.contains(Flag.FLAGGED));
////				if (flags.contains(Flag.DRAFT))
////					flagArr.add("DRAFT");
//////					System.out.println(" flags.contains(Flag.DRAFT) "+flags.contains(Flag.DRAFT));
////				if (flags.contains(Flag.DELETED))
////					flagArr.add("DELETED");
//////					System.out.println(" flags.contains(Flag.DELETED) "+flags.contains(Flag.DELETED));
////				if (flags.contains(Flag.ANSWERED))
////					flagArr.add("ANSWERED");
//////					System.out.println(" flags.contains(Flag.ANSWERED) "+flags.contains(Flag.ANSWERED));
//	//
////				// getting labels
////				List<String> labelList = new ArrayList<String>();
//	//
////				GmailMessage gmailMessage = (GmailMessage) m;
////				String[] labels = gmailMessage.getLabels();
//	//
////				if (labels.length > 0) {
////					for (String label : labels) {
////						labelList.add(label.replace("\\", ""));
////					}
////				}
//	//
////				System.out.println(" labelList  :::::: " + labelList);
//	//
////				// avoid repeated email message if came
////				long uid = f.getUID(m);
//////				if (emailRepo.existsByFolderAndUid(folderName, uid))
//////					continue;
//////				System.err.println(" emailRepo.existsByFolderStartWithAndUid(folderName, uid)  ::::::: "
//////						+ emailRepo.existsByFolderStartWithAndUid(folderName, uid));
////				if (emailRepo.existsByFolderStartWithAndUid(folderName, uid))
////					continue;
//	//
////				// extracting from and to
////				String from = Optional.ofNullable(m.getFrom()).filter(a -> a.length > 0).map(a -> a[0].toString())
////						.orElse(null);
////				String to = Optional.ofNullable(m.getRecipients(Message.RecipientType.TO)).map(Arrays::stream)
////						.orElseGet(Stream::empty).map(Address::toString).collect(Collectors.joining(", "));
////				String cc = Optional.ofNullable(m.getRecipients(Message.RecipientType.CC)).map(Arrays::stream)
////						.orElseGet(Stream::empty).map(Address::toString).collect(Collectors.joining(", "));
////				String bcc = Optional.ofNullable(m.getRecipients(Message.RecipientType.BCC)).map(Arrays::stream)
////						.orElseGet(Stream::empty).map(Address::toString).collect(Collectors.joining(", "));
//	//
////				String mime = m.getContentType();
////				String body = extractText(m);
//	//
////				EmailMetadata e = new EmailMetadata();
////				e.setFolder(folderName);
////				e.setUid(uid);
////				e.setUidValidity(f.getUIDValidity());
////				e.setMessageId(((MimeMessage) m).getMessageID());
////				e.setSubject(m.getSubject());
////				e.setFromAddr(from);
////				e.setToAddr(to);
////				e.setSentAt(m.getSentDate());
//////				e.setFlags(Arrays.toString(m.getFlags().getSystemFlags()));
////				e.setFlags(flagArr);
////				e.setSizeBytes(m.getSize());
////				e.setSnippet(null);
////				e.setHasAttachments(false);
////				e.setBodyCached(!body.isEmpty());
////				e.setStarred(isStarred);
////				e.setRead(isRead);
////				e.setLabels(labelList);
////				e.setDeleteFlag(false);
////				e.setMimeType(mime);
////				e.setBody(body);
////				e.setBcc(bcc);
////				e.setCc(cc);
//	//
////				emailRepo.save(e);
//	//
//////				EmailBody eb = new EmailBody();
//////				eb.setEmailId(meta.getId());
//////				eb.setMimeType(mime);
//////				eb.setBody(body);
//////				
//////				bodyRepo.save(eb);
////			}
////		}
//
////		private String extractText(Message message) throws Exception {
////			if (message.isMimeType("text/plain"))
////				return (String) message.getContent();
////			if (message.isMimeType("text/html"))
////				return (String) message.getContent();
////			if (message.isMimeType("multipart/*")) {
////				MimeMultipart mp = (MimeMultipart) message.getContent();
////				for (int i = 0; i < mp.getCount(); i++) {
////					BodyPart bp = mp.getBodyPart(i);
////					if (bp.isMimeType("text/plain"))
////						return bp.getContent().toString();
////				}
////				// fallback to first part
////				return mp.getBodyPart(0).getContent().toString();
////			}
////			return "";
////		}
//
//		/// will see later about this files saving in local db
//		private void saveAttachments(Message message, String saveDirectory) throws Exception {
//			if (message.isMimeType("multipart/*")) {
//				Multipart multipart = (Multipart) message.getContent();
//
//				for (int i = 0; i < multipart.getCount(); i++) {
//					BodyPart bodyPart = multipart.getBodyPart(i);
//
//					String disposition = bodyPart.getDisposition();
//
//					if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT)
//							|| disposition.equalsIgnoreCase(Part.INLINE))) {
//
//						String fileName = bodyPart.getFileName();
//						InputStream is = bodyPart.getInputStream();
//						File f = new File(saveDirectory + File.separator + fileName);
//
//						try (FileOutputStream fos = new FileOutputStream(f)) {
//							byte[] buf = new byte[4096];
//							int bytesRead;
//							while ((bytesRead = is.read(buf)) != -1) {
//								fos.write(buf, 0, bytesRead);
//							}
//						}
//						System.out.println("Saved attachment: " + f.getAbsolutePath());
//					} else if (bodyPart.isMimeType("multipart/*")) {
//						// handle nested multiparts (sometimes attachments are inside)
//						saveAttachments(new MimeMessage((MimeMessage) message), saveDirectory);
//					}
//				}
//			}
//		}
//
//		private void sleep(long ms) {
//			try {
//				Thread.sleep(ms);
//			} catch (InterruptedException ignored) {
//				System.err.println("got exception in sleep !!!!!!!!!!!");
//				ignored.printStackTrace();
//			}
//		}
//
//		// idleLoop(String folderName,ThreadListenerControl control)
//		public void idleLoop(String folderName) {
////			try {
//			// using multi threding while (control.isRunning())
////			while (true) {
////				System.out.println(" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! printing inside  idleLoop : " + folderName);
//
//			try (Store store = gmailStoreService.getStore()) {
//				GmailFolder folder = (GmailFolder) store.getFolder(folderName);
//
//				if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 || !folder.exists()) {
//					return;
////						break;
//				}
//
//				folder.open(Folder.READ_WRITE);
//				////////////////////// **************************
//
//				UIDFolder uidFolder = (UIDFolder) folder;
//
//				// Get all UIDs currently on server
//				Message[] msgs = folder.getMessages();
//				long[] liveUids = new long[msgs.length];
//				for (int i = 0; i < msgs.length; i++) {
//					if (!msgs[i].isExpunged()) {
//						liveUids[i] = uidFolder.getUID(msgs[i]);
//					}
//				}
//
//				Set<Long> serverUids = Arrays.stream(liveUids).boxed().collect(Collectors.toSet());
//
//				System.out.println(" serverUids.size :::::::::::::::::::::::: " + serverUids.size());
//
//				// Get all UIDs from DB for this folder
//				List<Long> dbUids = emailRepo.findUidsByFolder(folderName);
//
//				System.out.println(" dbUids.size :::::::::::::::::::::::: " + dbUids.size());
//
//				////////////////////// ***************************
//				System.out.println(" idle loop : folderName and it is open ::: " + folderName);
//				// ---- New messages ----
//				folder.addMessageCountListener(new MessageCountAdapter() {
//					@Override
//					public void messagesAdded(MessageCountEvent e) {
//						System.out.println(" idle loop : folderName ::: " + folderName);
//						System.out.println(" %%%%%%%%%%%%%%%%  addMessageCountListener idle : " + e);
//						try {
//							syncFolder(folderName);
//						} catch (Exception ex) {
//							ex.printStackTrace();
//						}
//					}
//
//					@Override
//					public void messagesRemoved(MessageCountEvent e) {
//						System.out.println(" idle loop : folderName ::: " + folderName);
//						System.out.println(" ****************** messagesRemoved idle : " + e);
//
//						resyncFolderForDeletedMessage(folder, folderName);
//					}
//				});
//
//				// ---- Flag changes ----
//				folder.addMessageChangedListener(e -> {
//					System.out.println(" idle loop : folderName ::: " + folderName);
//					System.out.println(" &&&&&&&&&&&&&&&&&&&&&& addMessageChangedListener idle : " + e);
//					System.out.println(" Flag update  :::::::::::::::::::::::::::::::::::::::::::::::: ");
//
////						try {
////							if (!folder.isOpen()) {
////								folder.open(Folder.READ_WRITE);
////							}
////							Message m = e.getMessage();
////							UIDFolder uidFolder = (UIDFolder) folder;
////							long uid = uidFolder.getUID(m);
//	//
////							System.out.println(" uid ********************************* "+uid);
//////							List<EmailMetadata> emd = emailRepo.findByFolderAndUid(folderName, uid);
//////							List<EmailMetadata> emd = emailRepo.findByFolderStartWithAndUid(folderName, uid); 
////							List<EmailMetadata> emd = emailRepo.findByUid(uid);
////						
////							
////							System.out.println(" printing the emd.size() :::::::::::::::::::  " + emd.size());
////							
////							if (!emd.isEmpty()) {
////								emd.forEach((meta -> {
////									if(meta.getFolder().startsWith(folderName)) {
////									try {
////										System.out.println(" meta  >>>>>>>>>>>>>>>>>>>> before updated isStarred "+meta.isStarred());
////										System.out.println(" meta  >>>>>>>>>>>>>>>>>>>> before updated isRead "+meta.isRead());
////										
////										Flags flags = m.getFlags();
////										
////										List<String> flagArr = new ArrayList<String>();
////										
////										if(flags.contains(Flag.SEEN)) flagArr.add("SEEN");
////										if(flags.contains(Flag.USER)) flagArr.add("USER");
////										if(flags.contains(Flag.RECENT)) flagArr.add("RECENT");
////										if(flags.contains(Flag.FLAGGED)) flagArr.add("FLAGGED");
////										if(flags.contains(Flag.DRAFT)) flagArr.add("DRAFT");
////										if(flags.contains(Flag.DELETED)) flagArr.add("DELETED");
////										if(flags.contains(Flag.ANSWERED)) flagArr.add("ANSWERED");
////										
////										meta.setRead(flags.contains(Flags.Flag.SEEN));
////										meta.setStarred(flags.contains(Flags.Flag.FLAGGED));
////										System.out.println(" Flags.Flag.SEEN "+ Flags.Flag.SEEN);
////										System.out.println(" Flags.Flag.FLAGGED "+Flags.Flag.FLAGGED);
////										
////										meta.setFlags(flagArr);
////										
////										emailRepo.save(meta);
////										
////										System.out.println(" meta  <<<<<<<<<<<<<<<<<<<< after updated isStarred "+meta.isStarred());
////										System.out.println(" meta  <<<<<<<<<<<<<<<<<<<< after updated isRead "+meta.isRead()); 
////										
////										System.out.println(" Arrays.toString(flags.getSystemFlags())  :::: "
////												+ Arrays.toString(flags.getSystemFlags()));
////										System.err.println("Message flags updated: UID " + uid + " in " + folderName);
////									} catch (Exception ex) {
////										ex.printStackTrace();
////									}
////									}
////								}));
////							}
////						} catch (Exception ex) {
////							ex.printStackTrace();
////						}
//				});
//
//				// ---- Idle forever ----
//
////					try {
//////							System.out.println(" folder.isOpen() && store.isConnected()  : " + (folder.isOpen() )+" && "+ (store.isConnected()));
////						while (folder.isOpen() && store.isConnected()) {
//////			                	System.out.println("priting inside the while loop true ");
////							System.out.println("Starting IDLE on " + folderName);
////							folder.idle(); // this blocks until server closes it
////						}
////					} catch (FolderClosedException fce) {
////						System.err.println("Folder closed by server, will reopen: " + folderName);
////					} catch (IllegalStateException ise) {
////						System.err.println("Folder was already closed, reconnecting: " + folderName);
////					}
//
//				// cleanup
//				try {
//					if (folder.isOpen()) {
//						folder.close(false);
//					}
//				} catch (Exception ignore) {
//					System.err.println("got exception in cleanup !!!!!!!!!!!");
//					ignore.printStackTrace();
//				}
//
//				// small backoff before retry
//				sleep(1000);
//
//			} catch (Exception ex) {
//				ex.printStackTrace();
//				sleep(1000); // backoff before retry
//			}
//
////			}
////			} 
////			catch (Exception e) {
////				System.err.println(e.getMessage());
////				Thread.currentThread().interrupt();
////			}
//
////			finally {
////	            System.out.println("Listener thread stopped for folder: " + folderName);
////	            activeListeners.remove(folderName);
////	        }
//		}
//
//		private String getSearchFolder(String curFolderName) {
//			String searchfolder = curFolderName.startsWith("INBOX") ? "INBOX" : curFolderName;
//			System.err.println(" printing the searchFolder ::::::: " + searchfolder);
//			return searchfolder;
//		}
//
//		// this resync folder is specially to mark the deleted message from the gmail
//		// server to sync with the local server
//		/*
//		 * once the message is expunged from the gmail server , we can not access that
//		 * expunged message using IMAP . for example : even we can not get the uid to
//		 * update in our local server that message as deleted. so i came with this
//		 * approach that by using folder i fetch all messages uids if the message is not
//		 * expunged from Gmail server throught Imap and by using the folder name i
//		 * fetched all uid from local db now checks that server uid contains db uid , if
//		 * not contains the mark that message as deleted in local server. In this way ,
//		 * i make sure that after any delete flag update happend in real gmail , it wil
//		 * get sync to local server.
//		 */
//		private void resyncFolderForDeletedMessage(Folder folder, String folderName) {
//			System.err.println(
//					" resyncFolder **************** called  : params : folder : " + folder + " folderName : " + folderName);
//			try {
//				if (!folder.isOpen()) {
//					folder.open(Folder.READ_ONLY); // read-only is enough for sync
//				}
//
//				UIDFolder uidFolder = (UIDFolder) folder;
//
//				// Get all UIDs currently on server
//				Message[] msgs = folder.getMessages();
//				long[] liveUids = new long[msgs.length];
//				for (int i = 0; i < msgs.length; i++) {
//					if (!msgs[i].isExpunged()) {
//						liveUids[i] = uidFolder.getUID(msgs[i]);
//					}
//				}
//
//				Set<Long> serverUids = Arrays.stream(liveUids).boxed().collect(Collectors.toSet());
//
//				System.out.println(" serverUids.size :::::::::::::::::::::::: " + serverUids.size());
//
//				// Get all UIDs from DB for this folder
//				List<Long> dbUids = emailRepo.findUidsByFolder(folderName);
//
//				System.out.println(" dbUids.size :::::::::::::::::::::::: " + dbUids.size());
//
//				// Find UIDs missing on server (expunged)
//				for (Long dbUid : dbUids) {
//					if (!serverUids.contains(dbUid)) {
//						// Message no longer exists on server → mark deleted in local server
//						List<EmailMetadata> emd = emailRepo.findByFolderStartWithAndUid(folderName, dbUid);
//
//						System.out.println(" emd.size ::::::::::::::::::::::::::: " + emd.size());
//						emd.forEach(meta -> {
//							meta.setDeleteFlag(true);
//							emailRepo.save(meta);
//							System.out.println("Resync marked UID " + dbUid + " as deleted in " + folderName);
//						});
//					}
//				}
//
//			} catch (Exception ex) {
//				ex.printStackTrace();
//			}
//		}
//		
//	}
//	
	
	
}
