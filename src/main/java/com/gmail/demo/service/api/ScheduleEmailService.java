package com.gmail.demo.service.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gmail.demo.entity.EmailMetadata;
import com.gmail.demo.entity.ScheduledEmail;
import com.gmail.demo.entity.ScheduledFileDetails;
import com.gmail.demo.handler.ResponseHandler;
import com.gmail.demo.repository.EmailRepo;
import com.gmail.demo.repository.ScheduledEmailRepo;
import com.gmail.demo.service.imaps.GmailStoreService;
import com.gmail.demo.util.MailUtil;
import com.sun.mail.gimap.GmailFolder;

import jakarta.activation.DataHandler;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
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
import jakarta.mail.Message;

@Service
public class ScheduleEmailService {

	@Autowired
    private final EmailRepo emailRepo;

	@Autowired
	private JavaMailSender javaMailSender;
	
	@Autowired
	private ScheduledEmailRepo scheduledEmailRepo;
	
	@Autowired
	private GmailStoreService gmailStoreService;

//	@Value("${file.scheduled.dir}")
	private static String FILE_DIRECTORY = "/home/sneha/Sneha/SpringBoot/Task/Gmail/schedule/";
	
	private static String fromMail = "snehademo27@gmail.com";
	
	private static final Logger log = LoggerFactory.getLogger(ScheduleEmailService.class);

    ScheduleEmailService(EmailRepo emailRepo) {
        this.emailRepo = emailRepo;
    }

	public void sendScheduledEmail(ScheduledEmail mail) throws MessagingException {
		
		log.info("sendScheduledEmail (+)");
		
		log.info("sendScheduledEmail param - mail : "+mail);

		MimeMessage mm = javaMailSender.createMimeMessage();
		MimeMessageHelper mmh = new MimeMessageHelper(mm, true);

		mmh.setFrom(mail.getFromArr());
//		mmh.setFrom(from);
		mmh.setTo(mail.getSend_to());
		if (!mail.getCc().isEmpty()) {
			mmh.setCc(mail.getCc());
		}
		if (!mail.getBcc().isEmpty()) {
			mmh.setBcc(mail.getBcc());
		}
		mmh.setSubject(mail.getSubject());
		mmh.setText(mail.getMsgBody());

		System.out.println(" files :::::::::::::: " + mail.getFileList());

		for (ScheduledFileDetails filedetails : mail.getFileList()) {

			File tempfile = new File(filedetails.getLocation());
			mmh.addAttachment(filedetails.getOgname(), tempfile.getAbsoluteFile());
		}
		javaMailSender.send(mm);
		
		log.info("sendScheduledEmail (-)");
	}

	public ResponseEntity<Object> saveScheduledEmail(ScheduledEmail email, MultipartFile[] files, String messageId)  {
		
		log.info("saveScheduledEmail (+)");
		log.info("saveScheduledEmail param - email : "+email);
		log.info("saveScheduledEmail param - messageId : "+messageId);
		
		List<ScheduledFileDetails> fileList  = new ArrayList<ScheduledFileDetails>();
		
		try {
			System.out.println("printing the files ::::::::: "+files.length);
			System.out.println(" files[0] != null  ::: "+files[0] != null +" files[0] "+files[0] );
			if(files != null && files.length > 0) {
				log.info("saveScheduledEmail param - files.length : "+files.length);
				fileList = multiFileUploadInLocalDir(files);
			}
		} catch (Exception e) {
			System.err.println(" file count error: "+e.getMessage());
		}
		
		
		if (fileList != null) {
			for (ScheduledFileDetails file : fileList) {
				file.setEmail(email);;
			}
		}
		email.setFromArr(fromMail);
		email.setFileList(fileList);
		email.setStatus(MailUtil.Status.PENDING);
		email.setGmailMessageId(messageId);
		scheduledEmailRepo.save(email);
		
		log.info("saveScheduledEmail (-)");

		return ResponseHandler.responseWithString("S", HttpStatus.OK);   
	}

	public List<ScheduledFileDetails> multiFileUploadInLocalDir(MultipartFile[] files) throws Exception {
		
		log.info("multiFileUploadInLocalDir (+)");
		
		List<ScheduledFileDetails> fileList = new ArrayList<ScheduledFileDetails>();

		for (MultipartFile file : files) {
			File dir = new File(FILE_DIRECTORY);

			if (!dir.exists()) {
				dir.mkdir();
			}
			
			String randomname = Integer.toString(new Random().nextInt(10000000));

			File myfile = new File(FILE_DIRECTORY + randomname);
			myfile.createNewFile();
			FileOutputStream fos = new FileOutputStream(myfile);
			fos.write(file.getBytes());
			fos.close();

			String name = file.getOriginalFilename();

			int dotindex = name.indexOf('.');
			String ogname = name.substring(0, dotindex);
			String extention = name.substring(dotindex, name.length());

			fileList.add(new ScheduledFileDetails(randomname, FILE_DIRECTORY + randomname, getCurDateTime(), extention, ogname));

		}

		log.info("multiFileUploadInLocalDir (-)");
		return fileList;
	}
	
	public String saveScheduleEmailInGmail(ScheduledEmail email, MultipartFile[] files) throws MessagingException, IOException {
		
		log.info("saveScheduleEmailInGmail (+)");
		log.info("saveScheduleEmailInGmail param - email : "+email);
		 
		String newFolderName = MailUtil.GmailSchedule;
		String messageId = "";
		
		String draftFolder = MailUtil.GmailDrafts;
	
		System.out.println(" printig the newFolderName :::::: " + newFolderName);
		try(Store store = gmailStoreService.getStore()){
			
			GmailFolder scheduleFolder = (GmailFolder) store.getFolder(newFolderName);
			if (!scheduleFolder.exists()) {
				scheduleFolder.create(Folder.HOLDS_MESSAGES);
				System.err.println("Schedule folder does not exist. schedule is created in Gmail main folder");
			}
			
			scheduleFolder.open(Folder.READ_WRITE);
			
			/* Create the message object to store in gmail server through IMAP */
	        Session session = Session.getDefaultInstance(new Properties());
	        MimeMessage message = new MimeMessage(session);

			/* Set basic details */
//	        message.setFrom(new InternetAddress(email.getFromArr()));
	        message.setFrom(new InternetAddress(fromMail));
	        for(String to : email.getSend_to()) {	        	
	        	message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
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
	        textBodyPart.setText(email.getMsgBody(), "utf-8", "html"); // use "plain" if needed
	        multipart.addBodyPart(textBodyPart);

			/* Adding the attachments if present */
	        if (files != null) {
	            for (MultipartFile file : files) {
	                if (!file.isEmpty()) {
	                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
	                    attachmentBodyPart.setFileName(file.getOriginalFilename());
	                    attachmentBodyPart.setDataHandler(new DataHandler(
	                        new ByteArrayDataSource(file.getBytes(), file.getContentType())
	                    ));
	                    multipart.addBodyPart(attachmentBodyPart);
	                }
	            }
	        }

			/* Set content */
	        message.setContent(multipart);
	        message.saveChanges();

			/* Append message to Gmail schedule folder */
	        scheduleFolder.appendMessages(new Message[]{message});
	        scheduleFolder.close(false);
	        
	        scheduleFolder.open(Folder.READ_WRITE);
	        UIDFolder uidFolder = (UIDFolder) scheduleFolder;

			/* Search by Message-ID */
	        Message[] found = scheduleFolder.search(
	            new HeaderTerm("Message-ID", message.getMessageID())
	        );
	        if (found.length > 0) {
	            long uid = uidFolder.getUID(found[0]);
	            System.out.println("Gmail UID: " + uid);
	        }
	        messageId = message.getMessageID();
	        System.out.println(" printing the message id after the message get saved in gmail message.getMessageID() : "+message.getMessageID());
	        scheduleFolder.close(false);
	        
	        
	        if(email.getGmailMessageId()!=null) {
	        	
	        	GmailFolder draftMailFolder = (GmailFolder) store.getFolder(draftFolder);
				if (!draftMailFolder.exists()) {
					draftMailFolder.create(Folder.HOLDS_MESSAGES);
					System.err.println("Schedule folder does not exist. schedule is created in Gmail main folder");
				}
				
				draftMailFolder.open(Folder.READ_WRITE);
				
				List<EmailMetadata> mdatas = emailRepo.findByMessageIdAndFolder(email.getGmailMessageId(), draftFolder);
				for (EmailMetadata mdata : mdatas) {
					System.out.println(" mdata.id in schedhule maill :: "+ mdata.getId() + " msg id "+ mdata.getMessageId());
					long uid = mdata.getUid();
					Message deleteMsgForever = draftMailFolder.getMessageByUID(uid);

					if (deleteMsgForever != null) {
						if (!deleteMsgForever.isExpunged()) {
							Flags deleteFlag = new Flags(Flags.Flag.DELETED);
							draftMailFolder.setFlags(new Message[] { deleteMsgForever }, deleteFlag, true);
						}

					} else {
						System.out.println("Message with UID " + uid + " not found in folder: " + draftFolder);
					}

					mdata.setDeleteFlag(true);
				}
				emailRepo.saveAll(mdatas);
				
				draftMailFolder.close(false);
	        }
		}
		
		log.info("saveScheduleEmailInGmail (-)");
		
		return messageId;
	}
	

	public String getCurDateTime() {
		Date currentDateTime = new Date();
		SimpleDateFormat formatedDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String datetime = formatedDateTime.format(currentDateTime);
		return datetime;
	}

}
