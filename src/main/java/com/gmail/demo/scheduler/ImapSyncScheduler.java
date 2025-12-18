package com.gmail.demo.scheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gmail.demo.entity.EmailMetadata;
import com.gmail.demo.repository.EmailRepo;
import com.gmail.demo.service.imaps.GmailStoreService;
import com.gmail.demo.service.imaps.PersistMailMessage;
import com.gmail.demo.util.MailUtil;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.imap.IMAPFolder;

import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.Flags.Flag;

@Component
public class ImapSyncScheduler {  
	
	@Autowired
	private EmailRepo emailRepo;
	
	@Autowired
	private GmailStoreService gmailStoreService;
	
	@Autowired
	private PersistMailMessage persistMailMessage;
	
	public Map<String,Integer> folderConCount = new HashMap<String, Integer>();
	
	private static final Logger knownExpLog = LoggerFactory.getLogger("known-exception-log");
	private static final Logger unKnownExpLog = LoggerFactory.getLogger("unknown-exception-log");
	
	public boolean syncfolderMails(String folderName) {
		
		folderName = folderName.startsWith("INBOX") ? MailUtil.Inbox : folderName ;

		try (Store store = gmailStoreService.getNewStore()) {
			
			boolean changesFlag = false;
			
			
			GmailFolder folder = (GmailFolder) store.getFolder(folderName);

			// // --- check folder holds messages ---
			if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 || !folder.exists()) {
				return false;
			}   

			folder.open(Folder.READ_WRITE);
		    folderConCount.put(folderName, folderConCount.getOrDefault(folder, 0)+1);
			
			UIDFolder uidFolder = (UIDFolder) folder;

			// --- Get UIDNEXT (the next UID that Gmail will assign) ---
			long uidNext = uidFolder.getUIDNext();
			System.out.println("UIDNEXT for folder " + folderName + " = " + uidNext);

			// ---  Load DB  ---
			List<EmailMetadata> dbMessages = folderName.startsWith(MailUtil.Inbox)
					? emailRepo.findByStartsWithFolderName(folderName)
					: emailRepo.findByFolder(folderName);
			Set<Long> dbUids = dbMessages.stream().map(EmailMetadata::getUid).collect(Collectors.toSet());
			
			// --- 1: Detect NEW messages (UID >= max DB UID + 1) ---
			long maxDbUid = dbUids.isEmpty() ? 0 : Collections.max(dbUids);
			if (uidNext > maxDbUid + 1) {
				Message[] newMsgs = uidFolder.getMessagesByUID(maxDbUid + 1, uidNext - 1);
				
				if(folderName.equals(MailUtil.Inbox)) {
					for(String category : MailUtil.Categories) {
						GmailRawSearchTerm catTerm = new GmailRawSearchTerm(category);
						Message[] msgs = folder.search(catTerm, newMsgs);
						String categoryFolderName = folderName + "/" + category.replace("category:", "");
						
						System.out.println(" category messages count "+msgs.length + " categoryName  : "+ categoryFolderName);
						if(persistNewMessage(folder, categoryFolderName, msgs)) {
							// update dbMessages and dbIids if new messages added
							dbMessages =emailRepo.findByFolder(categoryFolderName);
						    dbUids = dbMessages.stream().map(EmailMetadata::getUid).collect(Collectors.toSet());
						    changesFlag =true;
						}
					}
				}
				if(persistNewMessage(folder, folderName, newMsgs)) {
					// update dbMessages and dbIids if new messages added
					dbMessages =emailRepo.findByFolder(folderName);
				    dbUids = dbMessages.stream().map(EmailMetadata::getUid).collect(Collectors.toSet());
				    changesFlag =true;
				}

			}
			
			// --- 2: Detect DELETED messages ---
			Message[] msgs = folder.getMessages();
			long[] liveUidsArr = new long[msgs.length];
			for (int i = 0; i < msgs.length; i++) {
				if (!msgs[i].isExpunged()) {
					liveUidsArr[i] = uidFolder.getUID(msgs[i]);
				}
			}
			Set<Long> serverUids = Arrays.stream(liveUidsArr).boxed().collect(Collectors.toSet());

			Set<Long> deletedUids = new HashSet<>(dbUids);
			deletedUids.removeAll(serverUids);

			if (!deletedUids.isEmpty()) {
				dbMessages.stream().filter(m -> deletedUids.contains(m.getUid())).forEach(m -> {
					m.setDeleteFlag(true);
					emailRepo.save(m);
				});
				System.out.println("Deleted messages in " + folderName + ": " + deletedUids.size());
				// update dbMessages and dbIids if any messages deleted
				dbMessages = folderName.startsWith(MailUtil.Inbox)
						? emailRepo.findByStartsWithFolderName(folderName)
						: emailRepo.findByFolder(folderName);
			    dbUids = dbMessages.stream().map(EmailMetadata::getUid).collect(Collectors.toSet());
			    changesFlag =true;
			}

			// --- 3: Detect FLAG CHANGES (only for existing UIDs) --- 	
			Message[] existingMsgs = uidFolder.getMessagesByUID(dbUids.stream().mapToLong(Long::longValue).toArray());
	
			FetchProfile flagProfile = new FetchProfile();
			flagProfile.add(FetchProfile.Item.FLAGS);
			folder.fetch(existingMsgs, flagProfile); 

			Map<Long, Flags> serverFlags = new HashMap<>();
			for (Message msg : existingMsgs) {
				long uid = uidFolder.getUID(msg);
				serverFlags.put(uid, msg.getFlags());
			}

			for (EmailMetadata dbMsg : dbMessages) {
				Flags currentFlags = serverFlags.get(dbMsg.getUid());
				if (currentFlags != null) {
					boolean isSeen = currentFlags.contains(Flags.Flag.SEEN);
					boolean isStarred = currentFlags.contains(Flags.Flag.FLAGGED);

					List<String> flagArr = new ArrayList<String>();

					if (currentFlags.contains(Flag.SEEN))
						flagArr.add("SEEN");
					if (currentFlags.contains(Flag.USER))
						flagArr.add("USER");
					if (currentFlags.contains(Flag.RECENT))
						flagArr.add("RECENT");
					if (currentFlags.contains(Flag.FLAGGED))
						flagArr.add("FLAGGED");
					if (currentFlags.contains(Flag.DRAFT))
						flagArr.add("DRAFT");
					if (currentFlags.contains(Flag.DELETED))
						flagArr.add("DELETED");
					if (currentFlags.contains(Flag.ANSWERED))
						flagArr.add("ANSWERED");

					if (dbMsg.isRead() != isSeen || dbMsg.isStarred() != isStarred) {
						updateFlagForEmailMessage(dbMsg.getMessageId(), isSeen, isStarred, flagArr);
						changesFlag =true;
						System.out.println("Flags updated for UID " + dbMsg.getUid() + " in " + folderName);
					}
				}
			}

			folderConCount.put(folderName, folderConCount.getOrDefault(folderName, 0)-1);
			System.out.println(" folderConCount.get(folder) :::: "+folderConCount.get(folderName));
//			System.err.println("folderConCount ::  "+ folderConCount.toString());
			
			if(folderConCount.get(folderName) == 0){				
				folder.close(false);
			}
			return changesFlag;

		}catch (MessagingException e) {
			knownExpLog.error("IMAP-SYNC-SCHEDULER - 001 "+e.getMessage());
		} catch (IOException e) {
			knownExpLog.error("IMAP-SYNC-SCHEDULER - 002 "+e.getMessage());
		}
		catch (Exception ex) {
			unKnownExpLog.error("IMAP-SYNC-SCHEDULER - 002 "+ex.getMessage());
			System.err.println(ex.getMessage()+"--- error occured in ImapSyncScheduler ---");
			sleep(2000);
			syncfolderMails(folderName); // try again 
		}
		return false;
	}
	
	private boolean persistNewMessage(IMAPFolder folder, String folderName, Message[] newMsgs) throws MessagingException, IOException  {
		FetchProfile fp = new FetchProfile();
		fp.add(FetchProfile.Item.ENVELOPE);
		fp.add(FetchProfile.Item.FLAGS);
		folder.fetch(newMsgs, fp);
		
		System.out.println("New messages added in " + folderName + ": " + newMsgs.length);
		if(newMsgs.length > 0) {
			persistMailMessage.save(folder, folderName, newMsgs);
			return true;
		}
		return false;
	}
	
	// --- update flag for message id present in any folder ---
	private void updateFlagForEmailMessage(String Msgid, boolean isSeen, boolean isStarred, List<String> flagArr) {
		List<EmailMetadata> mailList = emailRepo.findByMessageId(Msgid);
	
		if(mailList == null || mailList.isEmpty()) return;
		
		for(EmailMetadata mail : mailList) {
			mail.setRead(isSeen);
			mail.setStarred(isStarred);
			mail.setFlags(flagArr);
		}
		emailRepo.saveAll(mailList);
	}
	
	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ignored) {
			System.err.println("got exception in sleep !!!!!!!!!!!");
			ignored.printStackTrace();
		}
	}

}


