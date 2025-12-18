package com.gmail.demo.service.imaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.gmail.demo.entity.SyncState;
import com.gmail.demo.repository.SyncStateRepo;
import com.gmail.demo.scheduler.EmailScheduler;
import com.gmail.demo.scheduler.ImapSyncScheduler;
import com.gmail.demo.service.api.SnoozedEmailService;
import com.gmail.demo.util.MailUtil;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.imap.IMAPFolder;
import jakarta.annotation.PostConstruct;
import jakarta.mail.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class ImapInitSyncService {

	private final SyncStateRepo syncRepo;
	private final ImapBasicService imapBasicService;
	private final GmailStoreService gmailStoreService;
	private final TaskScheduler taskScheduler;
	private final EmailScheduler emailScheduler;
	private final SnoozedEmailService snoozedEmailService;
	private final PersistMailMessage persistMailMessage;
	private final ImapSyncScheduler imapSyncScheduler;

	private static final int BATCH = 300;

	private boolean isInitCompleted = false;

	private static final Logger knownExpLog = LoggerFactory.getLogger("known-exception-log");
	private static final Logger unKnownExpLog = LoggerFactory.getLogger("unknown-exception-log");

	public ImapInitSyncService(SyncStateRepo syncRepo, ImapBasicService imapBasicService,
			GmailStoreService gmailStoreService, TaskScheduler taskScheduler, EmailScheduler emailScheduler,
			SnoozedEmailService snoozedEmailService, PersistMailMessage persistMailMessage,
			ImapSyncScheduler imapSyncScheduler) {
		this.syncRepo = syncRepo;
		this.imapBasicService = imapBasicService;
		this.gmailStoreService = gmailStoreService;
		this.taskScheduler = taskScheduler;
		this.emailScheduler = emailScheduler;
		this.snoozedEmailService = snoozedEmailService;
		this.persistMailMessage = persistMailMessage;
		this.imapSyncScheduler = imapSyncScheduler;
	}

	@PostConstruct // need to uncomment this for intially loading emails
	public void initialKickoff() throws Exception {
		// kick light initial sync asynchronously
		new Thread(() -> {
			try {
				List<String> defaultFolders = imapBasicService.defaultFolders();
				System.err.println("printing inside the initialKickofff !!!!!!!!!!!!!!!!!!!");
				System.out.println("printing the list of folders  : " + defaultFolders);

				for (String folder : defaultFolders) {
					syncFolder(folder);
				}
				syncFolder("INBOXWC"); // INBOX mail without category

				isInitCompleted = true;
				runtask();

			} catch (InterruptedException e) {
				knownExpLog.error("IMAP-INIT-SYNC-SERVICE-initialKickoff - 001 " + e.getMessage());
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				unKnownExpLog.error("IMAP-INIT-SYNC-SERVICE-initialKickoff - 002 " + e.getMessage());
			}
		}, "imap-initial-sync").start(); 

	}

	public void runtask() {
		System.out.println("Starting scheduled job after init...");
		snoozeMails();

		taskScheduler.schedule(this::syncMails, new CronTrigger("0 */2 * * * *") // every 2 minutes
		);
		taskScheduler.schedule(emailScheduler::sendScheduledEmails, new CronTrigger("0 */1 * * * *") // every 1 minutes
		); // :: means runnable
//		taskScheduler.schedule(this::snoozeMails, new CronTrigger("0 */1 * * * *"));
	}
	
	public void snoozeMails() {
		try {
			snoozedEmailService.markSnoozedByMessageId();
		} catch (GeneralSecurityException e) {
			knownExpLog.error("IMAP-INIT-SYNC-SERVICE-runtask - 001 " + e.getMessage());
		} catch (IOException e) {
			knownExpLog.error("IMAP-INIT-SYNC-SERVICE-runtask - 002 " + e.getMessage());
		} catch (Exception e) {
			System.out.println(" Error in snooze mail scheduler ...");
			e.printStackTrace();
			unKnownExpLog.error("IMAP-INIT-SYNC-SERVICE-runtask - 003 " + e.getMessage());
		}
	}

//	@Scheduled(fixedRate = 120000)
	public void syncMails() {
		if (!isInitCompleted) {
			return;
		}
		System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		System.out.println("syncMails ::::::::::::::::  is called ");
		System.err.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		try {
			List<String> folders = imapBasicService.defaultFolders();
			for (String folder : folders) {
				imapSyncScheduler.syncfolderMails(folder);
			}
		}  catch (InterruptedException e) {
			knownExpLog.error("IMAP-INIT-SYNC-SERVICE-syncMails - 001 " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("IMAP-INIT-SYNC-SERVICE-syncMails - 002 " + e.getMessage());
		}
	}

	@Transactional // db data transaction , if error occur it will rollback the saved transaction
	public void syncFolder(String folderName) throws Exception {

		try (Store store = gmailStoreService.getStore()) {

			// in GmailFolder we can get category-wise email also
			GmailFolder folder = (GmailFolder) store.getFolder(folderName.equals("INBOXWC") ? "INBOX" : folderName);

			// Skip folders that cannot hold messages (e.g., [Gmail] root) and if folder not
			// exist
			if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 && folder.exists()) {
				return;
			}
			folder.open(Folder.READ_ONLY);

			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! intial sync folder name : " + folderName);
			if (folderName.equals(MailUtil.Inbox)) {

				for (String category : MailUtil.Categories) {

					System.out.println("!!!!!!!!!!!!!!!!!!!!!!  inside the category : " + category);
					String categoryFolderName = folderName + "/" + category.replace("category:", "");
					long uidValidity = folder.getUIDValidity();
					long uidNext = folder.getUIDNext();

					System.out.println("priting the uidNext ::::  " + uidNext);

					SyncState state = syncRepo.findById(categoryFolderName).orElseGet(() -> {
						SyncState s = new SyncState();
						s.setFolder(categoryFolderName);
						s.setUidValidity(uidValidity);
						s.setLastSyncedUid(0);
						return s;
					});

					if (state.getUidValidity() != uidValidity) {
						// UID space changed -> full reindex recommended. For demo, just reset pointer.
						state.setUidValidity(uidValidity);
						state.setLastSyncedUid(0);
					}

					long last = state.getLastSyncedUid(); // 0
					long newest = uidNext - 1;
					if (newest <= 0 || last >= newest) {
						folder.close(false);
						continue;
					}

					UIDFolder uidFolder = (UIDFolder) folder;
					Message[] uidMsgs = uidFolder.getMessagesByUID(last + 1, newest);

					GmailRawSearchTerm catTerm = new GmailRawSearchTerm(category);
					Message[] msgs = folder.search(catTerm, uidMsgs);

					// Prepare FetchProfile once
					FetchProfile fp = new FetchProfile();
					fp.add(FetchProfile.Item.ENVELOPE);
					fp.add(FetchProfile.Item.FLAGS);
					fp.add(FetchProfile.Item.SIZE);
					fp.add(IMAPFolder.FetchProfileItem.INTERNALDATE);

					for (int i = 0; i < msgs.length; i += BATCH) {
						int end = Math.min(i + BATCH, msgs.length);
						Message[] batch = Arrays.copyOfRange(msgs, i, end);

						// Fetch metadata in bulk
						folder.fetch(batch, fp);

						// Persist batch
						persistMailMessage.save(folder, categoryFolderName, batch);

						// Update SyncState with highest UID in this batch
						long lastUid = folder.getUID(batch[0]); // after reversing, [0] is newest
						state.setLastSyncedUid(lastUid);
						syncRepo.save(state);
						Thread.sleep(30);
					}

				}

			} else {
				// some mails are not in category and directly present in inbox
				String lFolderName = folderName.equals("INBOXWC") ? "INBOX" : folderName;

				long uidValidity = folder.getUIDValidity();
				long uidNext = folder.getUIDNext();

				System.out.println("priting the uidNext ::::  " + uidNext);
				SyncState state = syncRepo.findById(lFolderName).orElseGet(() -> {
					SyncState s = new SyncState();
					s.setFolder(lFolderName);
					s.setUidValidity(uidValidity);
					s.setLastSyncedUid(0);
					return s;
				});
				if (state.getUidValidity() != uidValidity) {
					// UID space changed -> full reindex recommended. For demo, just reset pointer.
					state.setUidValidity(uidValidity);
					state.setLastSyncedUid(0);
				}
				long last = state.getLastSyncedUid(); // 0
				long newest = uidNext - 1;
				if (newest <= 0 || last >= newest) {
					folder.close(false);
					return;
				}

				/// descending order(newest to oldest)
				for (long start = newest; start > last; start -= BATCH) {
					long end = Math.max(start - BATCH + 1, last + 1);

					// getMessagesByUID(start, end) expects start <= end, so swap if needed
					Message[] msgs = folder.getMessagesByUID(end, start);

					FetchProfile fp = new FetchProfile();
					fp.add(FetchProfile.Item.ENVELOPE);
					fp.add(FetchProfile.Item.FLAGS);
					fp.add(FetchProfile.Item.SIZE);
					fp.add(IMAPFolder.FetchProfileItem.INTERNALDATE);
					fp.add(GmailFolder.FetchProfileItem.LABELS);
					folder.fetch(msgs, fp);
					// Reverse the array using two pointer approach , so the newest message in this
					// batch comes first
					for (int i = 0; i < msgs.length / 2; i++) {
						// swap with temp var
						Message tmp = msgs[i];
						msgs[i] = msgs[msgs.length - 1 - i];
						msgs[msgs.length - 1 - i] = tmp;
					}

					// using date to compare this and order in descending order.
//			    Arrays.sort(msgs, (m1, m2) -> {
//	                try {
//	                    return m2.getSentDate().compareTo(m1.getSentDate()); // Descending order
//	                } catch (MessagingException e) {
//	                    return 0;
//	                }
//	            });
					persistMailMessage.save(folder, lFolderName, msgs);
					// Update last synced UID to the max we've processed
					state.setLastSyncedUid(start);
					syncRepo.save(state);
					Thread.sleep(30);
				}
			}
			folder.close(false);
		}
	}
}
