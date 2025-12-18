package com.gmail.demo.service.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gmail.demo.dto.GmailSearchFilter;
import com.gmail.demo.dto.UpdateLabelsInEmailDTO;
import com.gmail.demo.entity.DraftFileDetails;
import com.gmail.demo.entity.EmailMetadata;
import com.gmail.demo.entity.InitialFileDetails;
import com.gmail.demo.entity.MailTemplete;
import com.gmail.demo.handler.ResponseHandler;
import com.gmail.demo.repository.EmailRepo;
import com.gmail.demo.repository.InitialFileDetailsRepo;
import com.gmail.demo.repository.MailTempleteRepo;
import com.gmail.demo.scheduler.ImapSyncScheduler;
import com.gmail.demo.service.imaps.GmailStoreService;
import com.gmail.demo.util.MailUtil;
import com.google.api.client.util.Value;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailMessage;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.util.ByteArrayDataSource;

@Service
public class EmailService {

	@Autowired
	private EmailRepo emailRepo;

	@Autowired
	private GmailStoreService gmailStoreService;

	@Autowired
	private JavaMailSender javaMailSender;

	@Autowired
	private MailTempleteRepo mailTempleteRepo;

	@Autowired
	private ImapSyncScheduler imapSyncScheduler;

	@Autowired
	private InitialFileDetailsRepo initialFileDetailsRepo;
	
	@Autowired
	private SnoozedEmailService snoozedEmailService;

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	@Value("${mail.imaps.username}")
	String from;

//	@Value("${file.attachments.dir}")
	String FILE_DIR = "/home/sneha/Sneha/SpringBoot/Task/Gmail/attachments/";

	String DRAFT_FILE_DIR = "/home/sneha/Sneha/SpringBoot/Task/Gmail/Draft_Attachments/";

	String fromMail = "snehademo27@gmail.com";

	public ResponseEntity<Object> fetchEmails(String folder, int page, int size) throws GeneralSecurityException, IOException {

		log.info("fetchEmails (+)");
		log.info("fetchEmails param : folder : + " + folder + ", page : " + page + ", size : " + size);

		try {
			Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
			Page<EmailMetadata> mails = null;

			/* for inbox folder only snoozed mails are hidden */
			if (folder.startsWith(MailUtil.Inbox)) {
				if (folder.equals(MailUtil.InboxPersonal)) {

					log.info("fetchEmails : personal and inbox mails");

					mails = emailRepo.fetchCustomInboxMessage(pageable);
				} else {
					mails = emailRepo.findByFolderAndSnoozed(folder, pageable); 
				}
			} else {
				if (folder.equals(MailUtil.GmailSnooze)) {
					snoozedEmailService.markSnoozedByMessageId(); /* sycn the snoozed email before fetching */
					mails = emailRepo.findBySnoozedAndFolder(true, MailUtil.GmailAllMails, pageable);
					/* return duplicates becoz , one email can present in many folder */
//			        mails = emailRepo.findBySnoozed(true, pageable); 
				} else {
					mails = emailRepo.findByFolder(folder, pageable);
				}
			}

			log.info("fetchEmails : mails.getTotalElements() " + mails.getTotalElements());
			log.info("fetchEmails : mails " + mails);

			for (EmailMetadata mail : mails) {
				List<InitialFileDetails> fileList = mail.getFileList();
				if (fileList != null) {
					Map<String, InitialFileDetails> map = new HashMap<>();
					List<InitialFileDetails> filteredFiles = fileList.stream().filter(file -> {
						boolean isDuplicate = map.containsKey(file.getOgname());
						if (!isDuplicate) {
							map.put(file.getOgname(), file);
						}
						return !isDuplicate;
					}).collect(Collectors.toList());

					mail.setFileList(filteredFiles);
				}
			}
			return ResponseHandler.responseWithObject(mails, HttpStatus.OK);

		} finally {
			log.info("fetchEmails (-)");
		}
	}

	public ResponseEntity<Object> callSyncFolder(String folderName) {
		log.info("callSyncFolder (+)");
		log.info("callSyncFolder param : folderName " + folderName);

		try {
			return ResponseHandler.responseWithObject(imapSyncScheduler.syncfolderMails(folderName), HttpStatus.OK);
		} finally {
			log.info("callSyncFolder (-)");
		}
	}

	public void sendSimpleMail(MailTemplete mail) {
		log.info("sendSimpleMail (+)");
		log.info("sendSimpleMail param : mail " + mail);

		try {
			SimpleMailMessage smm = new SimpleMailMessage();
			smm.setFrom(fromMail);
			smm.setTo(mail.getSend_to());
			if (!mail.getCc().isEmpty()) {
				smm.setCc(mail.getCc());
			}
			if (!mail.getBcc().isEmpty()) {
				smm.setBcc(mail.getBcc());
			}
			smm.setSubject(mail.getSubject());
			smm.setText(mail.getMsgBody());

			javaMailSender.send(smm);
		} finally {
			log.info("sendSimpleMail (-)");
		}
	}

	public void sendMailWithAttachment(MultipartFile[] files, MailTemplete mail)
			throws MessagingException, IllegalStateException, IOException {

		log.info("sendMailWithAttachment (+)");
		log.info("sendMailWithAttachment param : files.length " + files.length);
		log.info("sendMailWithAttachment param : mail " + mail);
		log.info("sendMailWithAttachment : FILE_DIR " + FILE_DIR);

		try {

			File dir = new File(FILE_DIR);

			if (!dir.exists()) {
				dir.mkdir();
			}

			MimeMessage mm = javaMailSender.createMimeMessage();
			MimeMessageHelper mmh = new MimeMessageHelper(mm, true);

			mmh.setFrom(fromMail);
//		    mmh.setFrom(from);
			mmh.setTo(mail.getSend_to());
			if (!mail.getCc().isEmpty()) {
				mmh.setCc(mail.getCc());
			}
			if (!mail.getBcc().isEmpty()) {
				mmh.setBcc(mail.getBcc());
			}
			mmh.setSubject(mail.getSubject());
			mmh.setText(mail.getMsgBody());

			for (MultipartFile file : files) {
				if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
					log.info("sendMailWithAttachment : Skipping empty/null file ");
					continue;
				}
				File tempfile = new File(FILE_DIR + file.getOriginalFilename());

				log.info("sendMailWithAttachment : tempfile " + tempfile);

				file.transferTo(tempfile);
				mmh.addAttachment(file.getOriginalFilename(), tempfile.getAbsoluteFile());
			}

			javaMailSender.send(mm);
		} catch (Exception e) {
			log.info("sendMailWithAttachment (-)");
		}
	}

	public void SendEmail(MailTemplete mail, MultipartFile[] files)
			throws IllegalStateException, MessagingException, IOException {

		log.info("SendEmail (+)");
		log.info("SendEmail param : mail " + mail);

		try {

			if (files == null || files.length == 0 || (files.length == 1 && files[0].isEmpty())) {
				log.info("SendEmail : inside the if (no attachments)");

				sendSimpleMail(mail);
			} else {
				log.info("SendEmail param : files.length " + files.length + "files[0].getOriginalFilename()"
						+ files[0].getOriginalFilename());

				sendMailWithAttachment(files, mail);
			}

			/*
			 * if gmail message is is not empty then this email is saved in draft by this
			 * backend then update in IMAP to delete the draft message which is edited and
			 * sent email
			 */
			if (mail.getGmailMessageId() != null && !mail.getGmailMessageId().isEmpty()) {
				List<EmailMetadata> draftMsg = emailRepo.findByMessageIdAndFolder(mail.getGmailMessageId(),
						MailUtil.GmailDrafts);

				try (Store store = gmailStoreService.getStore()) {
					GmailFolder sourceFolder = (GmailFolder) store.getFolder(MailUtil.GmailDrafts);
					if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == 0 && sourceFolder.exists()) {
						return;
					}
					sourceFolder.open(Folder.READ_WRITE);

					for (EmailMetadata mdata : draftMsg) {
						long uid = mdata.getUid();
						Message messageToFlagUpdate = sourceFolder.getMessageByUID(uid);

						if (messageToFlagUpdate != null) {
							if (!messageToFlagUpdate.isExpunged()) {
								Flags deleteFlag = new Flags(Flags.Flag.DELETED);
								sourceFolder.setFlags(new Message[] { messageToFlagUpdate }, deleteFlag, true);
							}
						} else {
							log.info("SendEmail : Message with UID " + uid + " not found in folder: "
									+ MailUtil.GmailDrafts);
						}
						mdata.setDeleteFlag(true);
						log.info("SendEmail : mdata.getDeleteFlag() " + mdata.getDeleteFlag());
					}
					emailRepo.saveAll(draftMsg);

					if (sourceFolder != null && sourceFolder.isOpen()) {
						/* Expunge the source folder to remove the marked message */
						sourceFolder.close(true);
					}

				}
			}
		} finally {
			log.info("SendEmail (-)");
		}
	}

	public ResponseEntity<Object> saveDraftEmail(MailTemplete email, MultipartFile[] files, String messageId) throws IOException {
		log.info("saveDraftEmail (+)");
		log.info("saveDraftEmail param : email " + email);
		log.info("saveDraftEmail param : messageId " + messageId);

		try {
			List<DraftFileDetails> fileList = new ArrayList<DraftFileDetails>();
			if (files != null) {
				fileList = multiFileUploadInLocalDir(files);

				log.info("saveDraftEmail param : files.length " + files.length);
			}
			if (fileList != null) {
				for (DraftFileDetails file : fileList) {
					file.setEmail(email);
				}
				email.setFileList(fileList);
			}

			/* db la store pannanum */
			email.setGmailMessageId(messageId);
			mailTempleteRepo.save(email);

			return ResponseHandler.responseWithDraftMessageId(messageId, HttpStatus.OK);
		} finally {
			log.info("saveDraftEmail (-)");
		}

	}

	public String saveDraftEmailInGmail(MailTemplete email, MultipartFile[] files, String msgId,
			List<Integer> oldFileIds) throws MessagingException, IOException {

		log.info("saveDraftEmailInGmail (+)");
		log.info("saveDraftEmailInGmail param : email " + email);
		log.info("saveDraftEmailInGmail param : msgId " + msgId);
		log.info("saveDraftEmailInGmail param : oldFileIds " + oldFileIds);

		String newFolderName = MailUtil.GmailDrafts;
		String messageId = msgId;

		log.info("saveDraftEmailInGmail : newFolderName " + newFolderName);

		try (Store store = gmailStoreService.getStore()) {
			GmailFolder draftFolder = (GmailFolder) store.getFolder(newFolderName);

			if (!draftFolder.exists()) {
				draftFolder.create(Folder.HOLDS_MESSAGES);

				log.error("saveDraftEmailInGmail : " + newFolderName
						+ " does not exist. schedule is created in Gmail main folder");
			}

			draftFolder.open(Folder.READ_WRITE);

			List<EmailMetadata> emdList = new ArrayList<EmailMetadata>();
			if (messageId != null && !messageId.isEmpty()) {
				emdList = emailRepo.findByMessageIdAndFolder(messageId, newFolderName);
			}

			if (emdList != null && !emdList.isEmpty()) {

				for (EmailMetadata emd : emdList) {
					Message existingMessage = draftFolder.getMessageByUID(emd.getUid());

					/*
					 * delete the existing draft message , becoz cant not update the existing one.
					 * so, delete it and create new one
					 */
					if (existingMessage != null) {
						log.info("saveDraftEmailInGmail : Deleting old draft with UID: " + emd.getUid());

						existingMessage.setFlag(Flags.Flag.DELETED, true);
					}

				}

			}

			/* Create the message object to store in gmail server through IMAP */
			Session session = Session.getDefaultInstance(new Properties());
			MimeMessage message = new MimeMessage(session);

			/* Set basic details */
			message.setFrom(new InternetAddress(fromMail));
			if (email.getSend_to() != null) {
				for (String to : email.getSend_to()) {
					message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
				}
			}
			if (email.getCc() != null) {
				message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(email.getCc()));
			}
			if (email.getBcc() != null) {
				message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(email.getBcc()));
			}
			message.setSubject(email.getSubject());
			message.setSentDate(new Date());

			/* Build email body */
			Multipart multipart = new MimeMultipart();

			/* Add text content (message body) */
			MimeBodyPart textBodyPart = new MimeBodyPart();
			textBodyPart.setText(email.getMsgBody(), "utf-8", "html");
			multipart.addBodyPart(textBodyPart);

			/* Adding the new attachments if present */
			if (files != null) {
				log.info("saveDraftEmailInGmail param : files.length " + files.length);
				for (MultipartFile file : files) {
					if (!file.isEmpty()) {
						MimeBodyPart attachmentBodyPart = new MimeBodyPart();
						attachmentBodyPart.setFileName(file.getOriginalFilename());
						attachmentBodyPart.setDataHandler(
								new DataHandler(new ByteArrayDataSource(file.getBytes(), file.getContentType())));
						multipart.addBodyPart(attachmentBodyPart);
					}
				}
			}

			/* Adding the old attachments if present */
			if (!oldFileIds.isEmpty()) {
				List<InitialFileDetails> oldFiles = initialFileDetailsRepo.findAllById(oldFileIds);
				for (InitialFileDetails file : oldFiles) {
					Path filePath = Paths.get(file.getLocation());

					MimeBodyPart attachmentBodyPart = new MimeBodyPart();
					FileDataSource fileDataSource = new FileDataSource(filePath.toFile());

					attachmentBodyPart.setDataHandler(new DataHandler(fileDataSource));
					attachmentBodyPart.setFileName(file.getOgname() + file.getExtention());
					multipart.addBodyPart(attachmentBodyPart);
				}
			}

			/* Set content */
			message.setContent(multipart);
			message.saveChanges();

			/* Append message to Gmail schedule folder */
			draftFolder.appendMessages(new Message[] { message });
			draftFolder.close(true);

			/*
			 * reopen the folder to get message id get the message id for further
			 * manipulation
			 */
			draftFolder.open(Folder.READ_WRITE);
			UIDFolder uidFolder = (UIDFolder) draftFolder;

			/* Search by Message-ID */
			Message[] found = draftFolder.search(new HeaderTerm("Message-ID", message.getMessageID()));
			if (found.length > 0) {
				long uid = uidFolder.getUID(found[0]);
				log.info("saveDraftEmailInGmail : Gmail UID: " + uid);
			}
			messageId = message.getMessageID();

			log.info(
					"saveDraftEmailInGmail : printing the message id after the message get saved in gmail message.getMessageID() : "
							+ message.getMessageID());

			draftFolder.close(false);
			return messageId;
		} finally {
			log.info("saveDraftEmailInGmail (-)");
		}

	}

	public void forwardEmail(List<Long> ids, String[] to, String[] cc, String[] bcc, String folderName,
			String initialContent, MultipartFile[] Files) throws MessagingException, IOException{

		log.info("forwardEmail (+)");
		log.info("forwardEmail param : ids " + ids);
		log.info("forwardEmail param : to " + to);
		log.info("forwardEmail param : cc " + cc);
		log.info("forwardEmail param : bcc " + bcc);
		log.info("forwardEmail param : folderName " + folderName);
		log.info("forwardEmail param : initialContent " + initialContent);

		try (Store store = gmailStoreService.getStore()) {
			String searchFolder = getSearchFolder(folderName);

			log.info("forwardEmail : searchFolder " + searchFolder);

			GmailFolder folder = (GmailFolder) store.getFolder(searchFolder);
			folder.open(Folder.READ_ONLY);
			Message messageToForward = null;

			for (long id : ids) {

				log.info("forwardEmail : id " + id);

				EmailMetadata eMsg = emailRepo.findById(id)
						.orElseThrow(() -> new IllegalArgumentException("Email meta data not found for this id"));

				log.info("forwardEmail : eMsg.getId() " + eMsg.getId());

				for (Message message : folder.getMessages()) {
					if (message.getHeader("Message-ID")[0].equals(eMsg.getMessageId())) {
						messageToForward = message;
						break;
					}
				}
				if (messageToForward == null) {
					throw new NullPointerException("Message not found!");
				}

				log.info("forwardEmail : messageToForward.toString() " + messageToForward.toString());

				MimeMessage forwardMessage = javaMailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(forwardMessage, true);

				log.info("forwardEmail :  fromMail : " + fromMail + ", to : " + to);

				helper.setFrom(fromMail);
				helper.setTo(to);
				if (bcc.length > 0) {
					helper.setBcc(bcc);
				}
				if (cc.length > 0) {
					helper.setCc(cc);
				}
				helper.setSubject("local Fwd: " + messageToForward.getSubject());
//				helper.setText("Forwarded message:\n\n" + messageToForward.getContent(), true);

				/* adding intro part */
				MimeBodyPart forwardIntroPart = new MimeBodyPart();
				forwardIntroPart.setText(initialContent + "\n\n", "UTF-8");

				/* create multipart holder */
				Multipart multipart = new MimeMultipart();
				multipart.addBodyPart(forwardIntroPart);

				/*setDataHandler will handle both the plain text and multipart email messages*/
				MimeBodyPart originalMessagePart = new MimeBodyPart();
				originalMessagePart.setDataHandler(messageToForward.getDataHandler());
				multipart.addBodyPart(originalMessagePart);

				if (Files != null) {
					log.info("forwardEmail param : Files.length " + Files.length);
					for (MultipartFile file : Files) {
						if (!file.isEmpty()) {
							MimeBodyPart attachmentBodyPart = new MimeBodyPart();
							attachmentBodyPart.setFileName(file.getOriginalFilename());
							attachmentBodyPart.setDataHandler(
									new DataHandler(new ByteArrayDataSource(file.getBytes(), file.getContentType())));
							multipart.addBodyPart(attachmentBodyPart);
						}
					}
				}

				forwardMessage.setContent(multipart);
				forwardMessage.saveChanges();

				javaMailSender.send(forwardMessage);

			}
		} finally {
			log.info("forwardEmail (-)");
		}

	}

	public void replyEmail(Long id, String folderName, String initialContent, MultipartFile[] Files) throws MessagingException, IOException {

		log.info("replyEmail (+) ");
		log.info("replyEmail param : id " + id);
		log.info("replyEmail param : folderName " + folderName);
		log.info("replyEmail param : initialContent " + initialContent);

		try (Store store = gmailStoreService.getStore()) {
			String searchFolder = getSearchFolder(folderName);

			log.info("replyEmail : searchFolder " + searchFolder);

			GmailFolder folder = (GmailFolder) store.getFolder(searchFolder);
			folder.open(Folder.READ_ONLY);
			Message messageToReply = null;

			EmailMetadata eMsg = emailRepo.findById(id)
					.orElseThrow(() -> new IllegalArgumentException("Email meta data not found for this id"));

			log.info("replyEmail : eMsg.getId() " + eMsg.getId());

			for (Message message : folder.getMessages()) {
				if (message.getHeader("Message-ID")[0].equals(eMsg.getMessageId())) {
					messageToReply = message;
					break;
				}
			}
			if (messageToReply == null) {
				throw new NullPointerException("Message not found!");
			}

			log.info("replyEmail : messageToReply.toString() " + messageToReply.toString());

			MimeMessage replyMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(replyMessage, true);

			log.info("replyEmail : fromMail " + fromMail);

			helper.setFrom(fromMail);
			helper.setTo(eMsg.getFromAddr());
			helper.setSubject("local Re: " + messageToReply.getSubject());
//				helper.setText("Forwarded message:\n\n" + messageToForward.getContent(), true);

			/* adding intro part */
			MimeBodyPart replyIntroPart = new MimeBodyPart();
			replyIntroPart.setText(initialContent + "\n\n", "UTF-8");

			/* create multipart holder */
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(replyIntroPart);

			/*setDataHandler will handle both the plain text and multipart email messages*/
			MimeBodyPart originalMessagePart = new MimeBodyPart();
			originalMessagePart.setDataHandler(messageToReply.getDataHandler());
			multipart.addBodyPart(originalMessagePart);

			if (Files != null) {
				log.info("replyEmail param : Files.length " + Files.length);
				for (MultipartFile file : Files) {
					if (!file.isEmpty()) {
						MimeBodyPart attachmentBodyPart = new MimeBodyPart();
						attachmentBodyPart.setFileName(file.getOriginalFilename());
						attachmentBodyPart.setDataHandler(
								new DataHandler(new ByteArrayDataSource(file.getBytes(), file.getContentType())));
						multipart.addBodyPart(attachmentBodyPart);
					}
				}
			}

			replyMessage.setContent(multipart);

			replyMessage.saveChanges();
			javaMailSender.send(replyMessage);

		} finally {
			log.info("replyEmail (-) ");
		}

	}

	/*
	 * as of now this flag manipulation working fine by using param id , fetch the
	 * uid of the particular msg from db then, by using message id fetched the same
	 * email message present in different to update flag to all then, using folder
	 * get the gmail folder -> then manipulated the flag in IMAP lastly if no
	 * exception occured in IMAP then updated the change in db for list of email
	 * message present in different folder with same message id
	 * 
	 */
	public void starManipulation(long id, boolean isStarred, String folderName, String msgId) throws MessagingException  {

		log.info("starManipulation (+) ");
		log.info("starManipulation param : id " + id);
		log.info("starManipulation param : isStarred " + isStarred);
		log.info("starManipulation param : folderName " + folderName);
		log.info("starManipulation param : msgId " + msgId);

		EmailMetadata eMsg = emailRepo.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Email meta data not found for this id"));
		List<EmailMetadata> mdata = emailRepo.findByMessageId(msgId);

		try (Store store = gmailStoreService.getStore()) {
			long uid = eMsg.getUid(); // getting uid of any one of the msg
			String searchFolder = getSearchFolder(folderName);

			log.info("starManipulation : searchFolder " + searchFolder);

			GmailFolder folder = (GmailFolder) store.getFolder(searchFolder);
			if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 && folder.exists()) {
				return;
			}
			folder.open(Folder.READ_WRITE);

			if (folder instanceof UIDFolder) {
				UIDFolder uidFolder = (UIDFolder) folder;
				Message message = uidFolder.getMessageByUID(uid);
				Flags starredFlag = new Flags(Flags.Flag.FLAGGED);
				folder.setFlags(new Message[] { message }, starredFlag, isStarred);

				log.info("starManipulation : changed in IMAP(check og gmail)  id   " + id + " isStarred   :::::::::::: "
						+ isStarred);

				folder.close();
				mdata.forEach(t -> t.setStarred(isStarred));
				emailRepo.saveAll(mdata);
			}
		} finally {
			log.info("starManipulation (-) ");
		}

	}

	/*long id, boolean isRead, String folderName, String msgId ,ReadAndUnReadDTO req*/
	public void readManipulation(List<Long> ids, boolean isRead, String folderName) throws MessagingException  {

		log.info("readManipulation (+)");
		log.info("readManipulation param : ids " + ids);
		log.info("readManipulation param : isRead " + isRead);
		log.info("readManipulation param : folderName " + folderName);

		List<EmailMetadata> mdatas = emailRepo.findAllById(ids);
		try (Store store = gmailStoreService.getStore()) {
			String searchFolder = getSearchFolder(folderName);

			log.info("readManipulation : searchFolder " + searchFolder);

			GmailFolder folder = (GmailFolder) store.getFolder(searchFolder);
			if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 && folder.exists()) {
				return;
			}
			folder.open(Folder.READ_WRITE);

			if (folder instanceof UIDFolder) {
				UIDFolder uidFolder = (UIDFolder) folder;

				for (EmailMetadata mdata : mdatas) {
					long uid = mdata.getUid();
					Message message = uidFolder.getMessageByUID(uid);

					log.info("readManipulation : mdata.getId() " + mdata.getId());
					log.info("readManipulation : (message != null) " + (message != null));

					if (message != null) {
						Flags starredFlag = new Flags(Flags.Flag.SEEN);
						folder.setFlags(new Message[] { message }, starredFlag, isRead);

						log.info("readManipulation : changed in IMAP(check og gmail)  id   " + mdata.getId()
								+ " isRead   :::::::::::: " + isRead);

						List<EmailMetadata> mailByMsgId = emailRepo.findByMessageId(mdata.getMessageId());
						mailByMsgId.forEach(t -> t.setRead(isRead));
						emailRepo.saveAll(mailByMsgId);
					}
				}

			}
			folder.close();
		} finally {
			log.info("readManipulation (-)");
		}

	}

	public void moveEmailToTrash(List<Long> ids, String folderName)  {

		log.info("moveEmailToTrash (+)");
		log.info("moveEmailToTrash param :  ids " + ids);
		log.info("moveEmailToTrash param :  folderName " + folderName);

		List<EmailMetadata> mdatas = emailRepo.findAllById(ids);

		if (mdatas.isEmpty()) {
			log.error("moveEmailToTrash : Email meta data not found for this list of id");
			throw new NullPointerException("Email meta data not found for this list of id");
		}

		try (Store store = gmailStoreService.getStore()) {
			String searchFolder = getSearchFolder(folderName);

			log.info("moveEmailToTrash : searchFolder " + searchFolder);

			GmailFolder sourceFolder = gmailStoreService.getFolderConnection(searchFolder);
			String trashFolderName = MailUtil.GmailTrash;
			GmailFolder trashFolder = gmailStoreService.getFolderConnection(trashFolderName);

			for (EmailMetadata mdata : mdatas) {
				long uid = mdata.getUid();
				Message messageToTrash = sourceFolder.getMessageByUID(uid);

				log.info("moveEmailToTrash : messageToTrash " + messageToTrash);

				if (messageToTrash != null) {

					// copy to [Gmail]/Trash folder
					Message[] messagesToCopy = { messageToTrash };
					sourceFolder.copyMessages(messagesToCopy, trashFolder);

					log.info(
							"moveEmailToTrash : ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ");
					log.info("moveEmailToTrash : !messageToTrash.isExpunged() " + !messageToTrash.isExpunged());

					// if not expunged messages , meaning already marked as true for delete
					if (!messageToTrash.isExpunged()) {
						// mark delete flag true in sourceFolder
						Flags deleteFlag = new Flags(Flags.Flag.DELETED);
						sourceFolder.setFlags(new Message[] { messageToTrash }, deleteFlag, true);
					}

				} else {
					log.info("moveEmailToTrash : Message with UID  " + uid + " not found in folder: " + folderName);
				}

				mdata.setDeleteFlag(true);

				log.info("moveEmailToTrash : mdata.getDeleteFlag() " + mdata.getDeleteFlag());
			}
			emailRepo.saveAll(mdatas);

			if (sourceFolder != null && sourceFolder.isOpen()) {
				// Expunge the source folder to remove the marked message
				sourceFolder.close(true);
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error("moveEmailToTrash : exception " + e.getMessage());
		} finally {
			log.info("moveEmailToTrash (-)");
		}

	}

	///// need to handle for INBOX category (updates,social, forums, promotions) for
	///// newFolderName
	/*
	 * this is for move folder from one to another , report spam any folder to spam
	 * folder (note : folderName : any folder , newFolderName : [Gmail]/Spam), and
	 * not spam (spam folder to inbox) (note : folderName : [Gmail]/Spam ,
	 * newFolderName : INBOX)
	 ******
	 * USING IMAP WE CAN NOT DO MOVE FOLDER INSIDE THE INBOX FOLDER (from social to
	 * updates) THESE ARE LABELS INSIDE INBOX FOLDER
	 */
	public ResponseEntity<Object> moveToFolder(List<Long> ids, String folderName, String newFolderName) {

		log.info("moveToFolder (+)");
		log.info("moveToFolder param : ids " + ids);
		log.info("moveToFolder param : folderName " + folderName);
		log.info("moveToFolder param : newFolderName " + newFolderName);

		try {
			String NewFolderSearchName = searchNewFolderByLabelName(newFolderName);
			NewFolderSearchName = NewFolderSearchName.startsWith(MailUtil.Inbox) ? MailUtil.Inbox : NewFolderSearchName;

			log.info("moveToFolder : NewFolderSearchName " + NewFolderSearchName);

			/* specially checking for INBOX folder case */
			if (!folderName.equals(NewFolderSearchName)) {
				List<EmailMetadata> mdatas = emailRepo.findAllById(ids);
				if (mdatas.isEmpty()) {
					log.error("moveToFolder : Email meta data not found for this list of id ");
					throw new NullPointerException("Email meta data not found for this list of id");
				}

				try (Store store = gmailStoreService.getStore()) {
					/*
					 * -- source folder --
					 */
					String searchFolder = getSearchFolder(folderName);
					GmailFolder sourceFolder = (GmailFolder) store.getFolder(searchFolder);

					if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == 0 && sourceFolder.exists()) {
						return ResponseHandler.responseWithString(
								" source folder " + searchFolder + " doesn't have any messages",
								HttpStatus.INTERNAL_SERVER_ERROR);
					}
					sourceFolder.open(Folder.READ_WRITE);

					/* 
					 * --- move folder --- 
					 */
					GmailFolder moveFolder = (GmailFolder) store.getFolder(NewFolderSearchName);
					if (!moveFolder.exists()) {
						moveFolder.create(Folder.HOLDS_MESSAGES);

						log.error("moveToFolder : source folder " + folderName
								+ " does not exist. Check Gmail IMAP settings.");
					}
					moveFolder.open(Folder.READ_WRITE);

					/*
					 * --- update in IMAP ---
					 */
					for (EmailMetadata mdata : mdatas) {
						long uid = mdata.getUid();
						Message message = sourceFolder.getMessageByUID(uid);
						if (message != null) {
							// move to new folder
							Message[] messagesToMove = { message };
							sourceFolder.moveMessages(messagesToMove, moveFolder);

							log.error("moveToFolder : message moved from sourceFolder " + sourceFolder + " moveFolder "
									+ moveFolder);
						} else {
							log.error("moveToFolder :Message with UID " + uid + " not found in folder: " + folderName);
						}

						/*
						 *  --- marking as delete only in source folder --- 
						 */
						List<EmailMetadata> res = emailRepo.findByFolderAndUid(folderName, uid);
						for (EmailMetadata emd : res) {
							emd.setDeleteFlag(true); /* marks as delete for faster ui rendering */
						}
						emailRepo.saveAll(res);
					}

					if (sourceFolder != null && sourceFolder.isOpen()) {
						sourceFolder.close(false);
					}
					if (moveFolder != null && moveFolder.isOpen()) {
						moveFolder.close(false);
					}

				} catch (Exception e) {
					e.printStackTrace();
					log.error("moveToFolder : exception " + e.getMessage());
					return ResponseHandler.responseWithString(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
				}
			}
			return ResponseHandler.responseWithString("S", HttpStatus.OK);

		} finally {
			log.info("moveToFolder (+)");
		}
	}

	/*
	 * can not to handle for INBOX category (updates,social, forums, promotions) can
	 * not update the labels within INBOX category using IMAP
	 */
	public void UpdateLabelsToEmail(List<UpdateLabelsInEmailDTO> reqList) throws MessagingException {

		log.info("UpdateLabelsToEmail (+)");
		log.info("UpdateLabelsToEmail param : reqList " + reqList);

		try (Store store = gmailStoreService.getStore()) {

			String folderName = reqList.get(0).getFolderName();
			String searchFolder = (folderName.startsWith("INBOX")) ? "INBOX" : folderName;

			log.info("UpdateLabelsToEmail : searchFolder : " + searchFolder);

			GmailFolder folder = (GmailFolder) store.getFolder(searchFolder);
			if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0 && folder.exists()) {
				return;
			}
			folder.open(Folder.READ_WRITE);

			if (folder instanceof UIDFolder) {
				UIDFolder uidFolder = (UIDFolder) folder;

				for (UpdateLabelsInEmailDTO req : reqList) {
					EmailMetadata mdata = emailRepo.findById(req.getId())
							.orElseThrow(() -> new NullPointerException("Email meta data not found for this id"));
					long uid = mdata.getUid();

					log.info("UpdateLabelsToEmail : uid " + uid);

					Message message = uidFolder.getMessageByUID(uid);
					if (message == null) {
						log.error("UpdateLabelsToEmail : Message not found for UID: " + uid + " in folder: "
								+ searchFolder);
						throw new NullPointerException("Message not found for UID: " + uid + " in folder: " + searchFolder);
					}

					if (!(message instanceof GmailMessage)) {
						log.error("UpdateLabelsToEmail : Message is not a GmailMessage, UID: " + uid);
						throw new IllegalArgumentException("Message is not a GmailMessage, UID: " + uid);
					}

					GmailMessage gmailMessage = (GmailMessage) message;
					Set<String> labelsToAdd = new HashSet<>();
					Set<String> labelsToRemove = new HashSet<>();

					for (Map<String, Boolean> label : req.getLabels()) {
						for (Map.Entry<String, Boolean> entry : label.entrySet()) {
							if (!entry.getKey().isEmpty()) {

								String[] labelArr = entry.getKey().split("-");
								Boolean boolValue = entry.getValue();
								gmailMessage.setLabels(labelArr, boolValue);

								log.info(" UpdateLabelsToEmail :  Key :::: " + labelArr + ", Value :::: " + boolValue);
								if (boolValue) {
									labelsToAdd.addAll(List.of(labelArr));
								} else {
									labelsToRemove.addAll(List.of(labelArr));
								}

							}
						}
					}

					log.info("UpdateLabelsToEmail : labelsToAdd " + labelsToAdd);
					log.info("UpdateLabelsToEmail : labelsToRemove " + labelsToRemove);

					List<EmailMetadata> res = emailRepo.findByMessageId(mdata.getMessageId());

					log.info("UpdateLabelsToEmail : res before :: " + res);

					for (EmailMetadata emd : res) {
						List<String> existingLabels = emd.getLabels();
						Set<String> updatedLabels = new HashSet<>(existingLabels);
						if (!labelsToRemove.isEmpty()) {
							updatedLabels.removeAll(labelsToRemove);
						}
						if (!labelsToAdd.isEmpty()) {
							updatedLabels.addAll(labelsToAdd);
						}
						emd.setLabels(new ArrayList<>(updatedLabels));
					}

					emailRepo.saveAll(res);

					log.info("UpdateLabelsToEmail : res after :: " + res);
				}
			}
			folder.close(false);
		} finally {
			log.info("UpdateLabelsToEmail (-)");
		}

	}

	/*
	 * important is not marked through flags , just need to add that email message
	 * in important folder as well need to update this method as move to folder but
	 * not removing from source folder. not moving but copying
	 */
	public ResponseEntity<Object> importantFlagManipulation(List<Long> ids, String folderName) {

		log.info("importantFlagManipulation (+)");
		log.info("importantFlagManipulation param : ids " + ids);
		log.info("importantFlagManipulation param : folderName " + folderName);

		try (Store store = gmailStoreService.getStore()) {
			String searchFolder = (folderName.startsWith("INBOX")) ? "INBOX" : folderName;

			log.info("importantFlagManipulation : searchFolder " + searchFolder);

			GmailFolder sourceFolder = (GmailFolder) store.getFolder(searchFolder);
			if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == 0 && sourceFolder.exists()) {
				log.error("importantFlagManipulation : Folder does not have any messages");
				return ResponseHandler.responseWithString("Folder does not have any messages",
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
			sourceFolder.open(Folder.READ_WRITE);

			GmailFolder importantFolder;
			if (!folderName.equals(MailUtil.GmailImportant)) {
				importantFolder = (GmailFolder) store.getFolder(MailUtil.GmailImportant);
				if (!importantFolder.exists()) {
					importantFolder.create(Folder.HOLDS_MESSAGES);
					log.error("importantFlagManipulation : source folder " + folderName
							+ " does not exist. Check Gmail IMAP settings.");
				}
				importantFolder.open(Folder.READ_WRITE);
			} else {
				importantFolder = sourceFolder;
			}

			for (Long id : ids) {

				log.info("importantFlagManipulation : id " + id);
				log.info("importantFlagManipulation : folderName " + folderName);

				EmailMetadata eMsg = emailRepo.findById(id)
						.orElseThrow(() -> new Exception("Email meta data not found for   id"));
				long uid = eMsg.getUid();

				log.info("importantFlagManipulation : uid " + uid);

				if (sourceFolder instanceof UIDFolder) {
					UIDFolder uidFolder = (UIDFolder) sourceFolder;
					Message message = uidFolder.getMessageByUID(uid);

					if (message != null) {
						Message[] msgs = { message };

						/*
						 * if both folders are same then mark the message as not important. As it call
						 * from important folder
						 */
						if (!folderName.equals(MailUtil.GmailImportant)) {
							/* copy to important folder */
							sourceFolder.copyMessages(msgs, importantFolder);

							log.info("importantFlagManipulation : message moved from sourceFolder " + sourceFolder
									+ " importantFolder " + importantFolder);
						} else {
							/* mark msgs as deleted */
							for (Message msg : msgs) {
								msg.setFlag(Flags.Flag.DELETED, true);
							}
							log.info("importantFlagManipulation : message delete from importantFolder "
									+ importantFolder);
						}

					} else {
						log.info("importantFlagManipulation : Message with UID " + uid + " not found in folder: "
								+ folderName);
					}

				}
			}
			if (sourceFolder != null && sourceFolder.isOpen()) {
				sourceFolder.close(false);
			}
			if (importantFolder != null && importantFolder.isOpen()) {
				importantFolder.close(false);
			}

			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (Exception e) {
			log.error("importantFlagManipulation : exception " + e.getMessage());
			return ResponseHandler.responseWithString(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("importantFlagManipulation (-)");
		}
	}

	public void archiveEmail(List<Long> ids, String folderName) {

		log.info("archiveEmail (+)");
		log.info("archiveEmail param : ids " + ids);
		log.info("archiveEmail param : folderName " + folderName);

		try {
			String newFolderName = folderName.equals(MailUtil.GmailArchive) ? MailUtil.Inbox : MailUtil.GmailArchive;
			List<EmailMetadata> mdatas = emailRepo.findAllById(ids);

			if (mdatas.isEmpty()) {
				throw new NullPointerException("Email meta data not found for this list of id");
			}

			log.info("archiveEmail : newFolderName " + newFolderName);

			try (Store store = gmailStoreService.getStore()) {
				String searchFolder = getSearchFolder(folderName);

				log.info("archiveEmail : searchFolder " + searchFolder);

				GmailFolder sourceFolder = (GmailFolder) store.getFolder(searchFolder);
				if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == 0 && sourceFolder.exists()) {
					return;
				}
				sourceFolder.open(Folder.READ_WRITE);

//				Folder archiveFolder = store.getFolder(newFolderName);
				GmailFolder archiveFolder = (GmailFolder) store.getFolder(newFolderName);
				if (!archiveFolder.exists()) {
					archiveFolder.create(Folder.HOLDS_MESSAGES);
					log.info("archiveEmail : Archive folder does not exist. new archive folder is created ");
				}
				archiveFolder.open(Folder.READ_WRITE);

				for (EmailMetadata mdata : mdatas) {
					long uid = mdata.getUid();
					Message message = sourceFolder.getMessageByUID(uid);

					if (message != null) {
						/* copy to archive folder */
						Message[] messagesToMove = { message };
						sourceFolder.copyMessages(messagesToMove, archiveFolder);
						/* Mark messages as deleted in source folder */
						for (Message msg : messagesToMove) {
							msg.setFlag(Flags.Flag.DELETED, true);
						}
					} else {
						log.info("archiveEmail : Message with UID " + uid + " not found in folder: " + folderName);
					}

					/* updating in database delete flag true in source folder */
					List<EmailMetadata> res = emailRepo.findByFolderAndUid(folderName, uid);

					for (EmailMetadata emd : res) {
						emd.setFolder(newFolderName);
						emd.setDeleteFlag(true);
					}
					emailRepo.saveAll(res);
				}

				if (sourceFolder != null && sourceFolder.isOpen()) {
					/* while closing folder delete message in source folder */
					sourceFolder.close(true);
				}
				if (archiveFolder != null && archiveFolder.isOpen()) {
					archiveFolder.close(false);
				}

			} catch (Exception e) {
				log.error("archiveEmail : exception " + e.getMessage());
				e.printStackTrace();
			}

		} finally {
			log.info("archiveEmail (+)");
		}

	}

	/* deleteForever used in spam and trash folder */
	public void deleteForever(List<Long> ids, String folderName) {

		log.info("deleteForever (+)");
		log.info("deleteForever param : ids " + ids);
		log.info("deleteForever param : folderName " + folderName);

		try {
			List<EmailMetadata> mdatas = emailRepo.findAllById(ids);

			if (mdatas.isEmpty()) {
				log.error("deleteForever : Email meta data not found for this list of id");
				throw new NullPointerException("Email meta data not found for this list of id");
			}

			try (Store store = gmailStoreService.getStore()) {
				String searchFolder = getSearchFolder(folderName);

				log.info("deleteForever : searchFolder " + searchFolder);

				GmailFolder sourceFolder = (GmailFolder) store.getFolder(searchFolder);
				if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == 0 && sourceFolder.exists()) {
					return;
				}

				sourceFolder.open(Folder.READ_WRITE);
				for (EmailMetadata mdata : mdatas) {
					long uid = mdata.getUid();
					Message deleteMsgForever = sourceFolder.getMessageByUID(uid);

					if (deleteMsgForever != null) {
						if (!deleteMsgForever.isExpunged()) {
							Flags deleteFlag = new Flags(Flags.Flag.DELETED);
							sourceFolder.setFlags(new Message[] { deleteMsgForever }, deleteFlag, true);
						}
					} else {
						log.info("deleteForever : \"Message with UID " + uid + " not found in folder: " + folderName);
					}
					mdata.setDeleteFlag(true);
				}
				emailRepo.saveAll(mdatas);

				if (sourceFolder != null && sourceFolder.isOpen()) {
					/* Expunge the source folder to remove the marked message */
					sourceFolder.close(true);
				}

			} catch (Exception e) {
				log.error("deleteForever : exception : " + e.toString());
				e.printStackTrace();
			}
		} finally {
			log.info("deleteForever (-)");
		}
	}

	public ResponseEntity<Object> fetchRedEmails() throws MessagingException {

		log.info("fetchRedEmails (+)");
		try {
			String gmailAllMail = MailUtil.GmailAllMails;
			Set<EmailMetadata> responseEmails = emailRepo.findByReadAndFolder(true, gmailAllMail);

			return ResponseHandler.responseWithObject(responseEmails, "size of result : " + responseEmails.size(),
					HttpStatus.OK);
		} finally {
			log.info("fetchRedEmails (-)");
		}
	}

	/* search email using query */
	public ResponseEntity<Object> searchEmailInDB(String searchTermReq) throws MessagingException {

		log.info("searchEmailInDB (+)");
		try {
			String gmailAllMail = MailUtil.GmailAllMails;
			Set<EmailMetadata> responseEmails = emailRepo.searchEmailByWord(searchTermReq, gmailAllMail);
			return ResponseHandler.responseWithObject(responseEmails, "size of result : " + responseEmails.size(),
					HttpStatus.OK);
		} finally {
			log.info("searchEmailInDB (-)");
		}
	}

	/* search filter using query */
	public ResponseEntity<Object> searchEmailUsingFiltersInDB(GmailSearchFilter filter) throws ParseException {

		log.info("searchEmailUsingFiltersInDB (+)");
		log.info("searchEmailUsingFiltersInDB param : filter " + filter);

		try {
			String folder = (filter.getFolder() != null && !filter.getFolder().isEmpty()) ? filter.getFolder()
					: MailUtil.GmailAllMails;
			String readStr = ""; 
			boolean searchFlag = false;
			if (folder.equals(MailUtil.read) || folder.equals(MailUtil.unread)) {
				readStr = folder;
				filter.setRead(folder.equals(MailUtil.read) ? true : false);
				folder = MailUtil.GmailAllMails;
			}

			List<String> searchFolders = new ArrayList<String>();
			if (folder.equals(MailUtil.anywhere)) {
				searchFolders.add(MailUtil.GmailAllMails);
				searchFolders.add(MailUtil.GmailTrash);
				searchFolders.add(MailUtil.GmailSpam);
			} else {
				searchFolders.add(folder);
			}

			log.info("searchEmailUsingFiltersInDB : searchFolders " + searchFolders);
			Set<EmailMetadata> finalResponseEmails = new HashSet<EmailMetadata>();

			for (String searchFolder : searchFolders) {
				log.info("searchEmailUsingFiltersInDB : searchFolder " + searchFolder);

				if (searchFolder.isEmpty())
					continue;

				Set<EmailMetadata> responseEmails = new HashSet<EmailMetadata>();

				/* From */
				if (filter.getFromAdd() != null && !filter.getFromAdd().isEmpty()) {
					searchFlag = true;
					Set<EmailMetadata> response = emailRepo.findByfromAddrAndFolder(filter.getFromAdd(), searchFolder);

					log.info("searchEmailUsingFiltersInDB : from search response size " + response.size());

					if (responseEmails.isEmpty())
						responseEmails.addAll(response);
					else
						responseEmails.retainAll(response);/* retainAll will make that only common message should present in responseEmails */
				}

				/* To */
				if (filter.getToAdd() != null && !filter.getToAdd().isEmpty()) {
					searchFlag = true;
					Set<EmailMetadata> response = emailRepo.findByToAddrAndFolder(filter.getToAdd(), searchFolder);

					log.info("searchEmailUsingFiltersInDB : to search response size " + response.size());

					if (responseEmails.isEmpty())
						responseEmails.addAll(response);
					else
						responseEmails.retainAll(response);
				}

				/* Subject */
				if (filter.getSubject() != null && !filter.getSubject().isEmpty()) {
					searchFlag = true;
					Set<EmailMetadata> response = emailRepo.findBySubjectAndFolder(filter.getSubject(), searchFolder);

					log.info("searchEmailUsingFiltersInDB : subject search response size " + response.size());

					if (responseEmails.isEmpty())
						responseEmails.addAll(response);
					else
						responseEmails.retainAll(response);
				}

				/* Has the word */
				if (filter.getHasTheWord() != null && !filter.getHasTheWord().isEmpty()) {
					searchFlag = true;
					Set<EmailMetadata> response = emailRepo.searchEmailByWord(filter.getHasTheWord(), searchFolder);

					log.info("searchEmailUsingFiltersInDB : hasTheWord search response size " + response.size());

					if (responseEmails.isEmpty())
						responseEmails.addAll(response);
					else
						responseEmails.retainAll(response);
				}

				/* Does not have a word */
				if (filter.getDoesNotHave() != null && !filter.getDoesNotHave().isEmpty()) {
					searchFlag = true;
					Set<EmailMetadata> response = emailRepo.searchEmailByNotWord(filter.getDoesNotHave(), searchFolder);

					log.info("searchEmailUsingFiltersInDB : doesNotHave search response size " + response.size());

					if (responseEmails.isEmpty())
						responseEmails.addAll(response);
					else
						responseEmails.retainAll(response);
				}

				/* Date */
				if (filter.getDateWithinValue() > 0 && filter.getDateWithinPeriod() != null && filter.getDate() != null
						&& !filter.getDate().isEmpty()) {
					searchFlag = true;
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
					Date baseDate = (filter.getDate() != null && !filter.getDate().isEmpty())
							? formatter.parse(filter.getDate())
							: new Date();

					int afterdatePlusOneDay = filter.getDateWithinValue() + 1;
					Calendar cal1 = Calendar.getInstance();
					cal1.setTime(baseDate);
					Calendar cal2 = Calendar.getInstance();
					cal2.setTime(baseDate);

					switch (filter.getDateWithinPeriod()) {
					case DAY:
						cal1.add(Calendar.DAY_OF_MONTH, -filter.getDateWithinValue());
						cal2.add(Calendar.DAY_OF_MONTH, +afterdatePlusOneDay);
						break;
					case WEEK:
						cal1.add(Calendar.WEEK_OF_YEAR, -filter.getDateWithinValue());
						cal2.add(Calendar.WEEK_OF_YEAR, +afterdatePlusOneDay);
						break;
					case MONTH:
						cal1.add(Calendar.MONTH, -filter.getDateWithinValue());
						cal2.add(Calendar.MONTH, +afterdatePlusOneDay);
						break;
					case YEAR:
						cal1.add(Calendar.YEAR, -filter.getDateWithinValue());
						cal2.add(Calendar.YEAR, +afterdatePlusOneDay);
						break;
					}

					Date beforeDate = convertDateFormate(cal1.getTime());
					Date afterDate = convertDateFormate(cal2.getTime());

					Set<EmailMetadata> response = emailRepo.findBySentAtAndFolder(beforeDate, afterDate, searchFolder);

					log.info("searchEmailUsingFiltersInDB : beforeDate " + beforeDate);
					log.info("searchEmailUsingFiltersInDB : afterDate " + afterDate);
					log.info("searchEmailUsingFiltersInDB : date search response size " + response.size());

					if (responseEmails.isEmpty())
						responseEmails.addAll(response);
					else
						responseEmails.retainAll(response);

				}

				/* Size */
				if (filter.getSizeValue() > 0 && filter.getSizeRange() != null && filter.getSize() != null) {
					searchFlag = true;
					int bytes = convertToBytes(filter.getSizeValue(), filter.getSize());
					if (filter.getSizeRange() == MailUtil.Range.GT) {
						Set<EmailMetadata> response = emailRepo.findBySizeBytesGreaterAndFolder(bytes, searchFolder);

						log.info("searchEmailUsingFiltersInDB : (size >) search response size" + response.size());

						if (responseEmails.isEmpty()) {
							responseEmails.addAll(response);
						} else {
							responseEmails.retainAll(response);
						}

					} else {
						Set<EmailMetadata> response = emailRepo.findBySizeBytesLesserAndFolder(bytes, searchFolder);

						log.info("searchEmailUsingFiltersInDB : (size <) search response size" + response.size());

						if (responseEmails.isEmpty())
							responseEmails.addAll(response);
						else
							responseEmails.retainAll(response);
					}

				}
				if (filter.isHasAttachment()) {
					searchFlag = true;
					log.info("searchEmailUsingFiltersInDB : attachment is on ");

					if (responseEmails.isEmpty()) {
						responseEmails = emailRepo.findByFolderAndHasAttachments(searchFolder, true);
					} else {
						// creating iterating variable to handle the concurrent modification exception
						Set<EmailMetadata> iterateResponseEmails = new HashSet<EmailMetadata>(responseEmails);
						for (EmailMetadata email : iterateResponseEmails) {
							if (!email.isHasAttachments())
								responseEmails.remove(email);
						}
					}
					log.info("searchEmailUsingFiltersInDB : after attachment filter responseEmails size "
							+ responseEmails.size());
				}

				/* read and unread (selected in folder list) */
				if (readStr.equals(MailUtil.read) || readStr.equals(MailUtil.unread)) {
					searchFlag = true;
					if (responseEmails.isEmpty()) {
						Set<EmailMetadata> response = emailRepo.findByReadAndFolder(filter.getRead(), searchFolder);
						
						log.info("searchEmailUsingFiltersInDB : read filter response size " + response.size());
						responseEmails.addAll(response);
					} else {
						/* creating iterating variable to handle the concurrent modification exception */
						Set<EmailMetadata> iterateResponseEmails = new HashSet<EmailMetadata>(responseEmails);
						for (EmailMetadata email : iterateResponseEmails) {
							if (email.isRead() != filter.getRead())
								responseEmails.remove(email);
						}
					}

					log.info("searchEmailUsingFiltersInDB : after read/unread filter responseEmails size "
							+ responseEmails.size());
				}
				if (responseEmails.isEmpty() && !searchFlag) {
					responseEmails = emailRepo.findByFolderName(searchFolder);
				}

				finalResponseEmails.addAll(responseEmails);

				log.info("searchEmailUsingFiltersInDB : final responseEmails size " + responseEmails.size()
						+ " for searchFolder " + searchFolder);
			}
			log.info("searchEmailUsingFiltersInDB : final finalResponseEmails size " + finalResponseEmails.size());

			return ResponseHandler.responseWithObject(finalResponseEmails,
					" filteredMessages size " + finalResponseEmails.size(), HttpStatus.OK);
		} finally {
			log.info("searchEmailUsingFiltersInDB (-)");
		}
	}

	//// -- helper methods --

	private int convertToBytes(int value, MailUtil.Size unit) {
		switch (unit) {
		case MB:
			return value * 1024 * 1024;
		case KB:
			return value * 1024;
		case Bytes:
		default:
			return value;
		}
	}

	private String getSearchFolder(String curFolderName) {
		String searchfolder = curFolderName.startsWith("INBOX") ? "INBOX" : curFolderName;
		System.err.println(" printing the searchFolder ::::::: " + searchfolder);
		return searchfolder;
	}

	private String getCurDateTime() {
		Date currentDateTime = new Date();
		SimpleDateFormat formatedDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String datetime = formatedDateTime.format(currentDateTime);
		return datetime;
	}

	private Date convertDateFormate(Date date) {

		System.out.println(" input ::::::::::::::: " + date);
		SimpleDateFormat formatedDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String datetime = formatedDateTime.format(date);
		try {
			Date output = formatedDateTime.parse(datetime);
			System.out.println(" output :::::::::::::::::::  " + output);
			return output;
		} catch (ParseException e) {
			e.printStackTrace();
			return date;
		}
	}

	private List<DraftFileDetails> multiFileUploadInLocalDir(MultipartFile[] files) throws IOException  {
		List<DraftFileDetails> fileList = new ArrayList<DraftFileDetails>();

		for (MultipartFile file : files) {
			File dir = new File(DRAFT_FILE_DIR);

			if (!dir.exists()) {
				dir.mkdir();
			}

			String randomname = Integer.toString(new Random().nextInt(10000000));

			File myfile = new File(DRAFT_FILE_DIR + randomname);
			myfile.createNewFile();
			FileOutputStream fos = new FileOutputStream(myfile);
			fos.write(file.getBytes());
			fos.close();

			String name = file.getOriginalFilename();

			int dotindex = name.indexOf('.');
			String ogname = name.substring(0, dotindex);
			String extention = name.substring(dotindex, name.length());

			fileList.add(
					new DraftFileDetails(randomname, DRAFT_FILE_DIR + randomname, getCurDateTime(), extention, ogname));

		}

		return fileList;
	}

	public void snoozeEmailMessage(Long id, boolean snoozed, LocalDateTime snoozedLocalDateTime) {

		EmailMetadata emd = emailRepo.findById(id).orElseThrow();

		LocalDateTime currentDateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
		String formattedDateTime = currentDateTime.format(formatter);

		emd.setSnoozed(snoozed);
		emd.setSnoozedTime(snoozedLocalDateTime);
		emd.setSnoozedSetTime(MailUtil.StringToLocalDateTime(formattedDateTime));

		emailRepo.save(emd);

	}

	public void copySnoozeEmailToInbox(EmailMetadata email) {

		long uid = email.getUid();
		try (Store store = gmailStoreService.getStore()) {

			String searchFolder = getSearchFolder(email.getFolder());
			System.out.println(" searchFolder ::::::  " + searchFolder);

			GmailFolder sourceFolder = (GmailFolder) store.getFolder(searchFolder);
			if ((sourceFolder.getType() & Folder.HOLDS_MESSAGES) == 0 && sourceFolder.exists()) {
				return;
			}

			sourceFolder.open(Folder.READ_WRITE);

			Folder InboxFolder = store.getFolder(MailUtil.Inbox);
			if (!InboxFolder.exists()) {
				System.err.println("INBOX folder does not exist. Check Gmail IMAP settings.");
				return;
			}
			InboxFolder.open(Folder.READ_WRITE);
			Message snoozeMessage = sourceFolder.getMessageByUID(uid);

			if (snoozeMessage != null) {
				Message[] messagesToCopy = { snoozeMessage };
				sourceFolder.copyMessages(messagesToCopy, InboxFolder);

			}

			if (sourceFolder != null && sourceFolder.isOpen()) {
				sourceFolder.close(false);
			}
			if (InboxFolder != null && InboxFolder.isOpen()) {
				InboxFolder.close(false);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static String searchNewFolderByLabelName(String label) {

		Set<String> inboxLabels = Set.of("updates", "social", "forums", "promotions");
		Set<String> gmailLabels = Set.of("spam", "trash");

		String lower = label.toLowerCase();
		if (inboxLabels.contains(lower)) {
			return "INBOX/" + label;
		} else if (gmailLabels.contains(lower)) {
			return "[Gmail]/" + capitalizeFirst(lower);
		} else if (label.equals(MailUtil.Inbox)) {
			return MailUtil.Inbox;
		} else {
			return label;
		}

	}

	private static String capitalizeFirst(String word) {
		if (word == null || word.isEmpty())
			return word;
		return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
	}

}
