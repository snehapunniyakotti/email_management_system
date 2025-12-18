package com.gmail.demo.Controller;

import org.springframework.web.multipart.MultipartFile;

import com.gmail.demo.dto.ForwardEmailDTO;
import com.gmail.demo.dto.GmailSearchFilter;
import com.gmail.demo.dto.ReplyMailDTO;
import com.gmail.demo.dto.ScheduleEmailDTO;
import com.gmail.demo.dto.SendEmailDTO;
import com.gmail.demo.dto.UpdateLabelsInEmailDTO;
import com.gmail.demo.entity.MailTemplete;
import com.gmail.demo.entity.ScheduledEmail;
import com.gmail.demo.handler.ResponseHandler;
import com.gmail.demo.service.api.EmailService;
import com.gmail.demo.service.api.ScheduleEmailService;
import com.gmail.demo.service.imaps.ImapInitSyncService;
import com.gmail.demo.service.imaps.ImapLabelService;
import com.gmail.demo.util.MailUtil;

import jakarta.mail.MessagingException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/emails")
public class EmailController {
	private final ImapInitSyncService sync;

	private final EmailService emailService;

	private final ScheduleEmailService scheduleEmailService;

	private static final Logger log = LoggerFactory.getLogger(EmailController.class);
	private static final Logger knownExpLog = LoggerFactory.getLogger("known-exception-log");
	private static final Logger unKnownExpLog = LoggerFactory.getLogger("unknown-exception-log");

	public EmailController(EmailService emailService, ImapInitSyncService sync,
			ScheduleEmailService scheduleEmailService) {
		this.emailService = emailService;
		this.sync = sync;
		this.scheduleEmailService = scheduleEmailService;
	}

	@Autowired
	private ImapLabelService imapLabelService;

	// Trigger manual incremental sync (optional)
	@PostMapping("/sync")
	public String syncNow(@RequestHeader(defaultValue = "INBOX", required = false) String folder) throws Exception {
		System.out.println("  / sync api called for folder  :::  " + folder);
		log.info("syncNow (+)");
		sync.syncFolder(folder);
		log.info("syncNow (-)");
		return "OK";
	}

	@GetMapping("/mailData") // INBOX/personal
	public ResponseEntity<Object> mailData(@RequestHeader(defaultValue = "INBOX") String folder,
			@RequestHeader(defaultValue = "50") int page, @RequestHeader(defaultValue = "1") int size) {
		log.info("mailData (+)");
		try {
			return emailService.fetchEmails(folder, page, size);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECMD-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECMD-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("mailData (-)");
		}
	}

	@GetMapping("/labels")
	public ResponseEntity<Object> getLabelList() {
		log.info("getLabelList (+)");
		try {
			return ResponseHandler.responseWithObject(imapLabelService.getLabelList(), HttpStatus.OK);
		} catch (InterruptedException e) {
			knownExpLog.error("CECGLL-001" + e.getMessage());
			e.printStackTrace();
			return ResponseHandler.responseWithString("CECGLL-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			unKnownExpLog.error("CECGLL-002" + e.getMessage());
			e.printStackTrace();
			return ResponseHandler.responseWithString("CECGLL-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("getLabelList (-)");
		}
	}

	@PostMapping("/addLabel")
	public ResponseEntity<Object> createLabel(@RequestHeader String label, @RequestHeader String action,
			@RequestHeader(defaultValue = "") String newLabelName) {
		log.info("createLabel (+)");
		try {
			System.out.println(" label ::::::  " + label);
			System.out.println(" action ::::::: " + action);
			System.out.println(" newLabelName ::::::: " + newLabelName);
			return imapLabelService.manipulateLabel(label, action, newLabelName);
		} catch (InterruptedException e) {
			e.printStackTrace();
			knownExpLog.error("CECCL-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECCL-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECCL-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECCL-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("createLabel (-)");
		}
	}

	@PostMapping("/sendMail")
	public ResponseEntity<Object> sendSimpleMail(@ModelAttribute SendEmailDTO mail) {
		log.info("sendSimpleMail (+)");
		try {
			MailTemplete email = new MailTemplete(mail.getSend_to(), mail.getCc(), mail.getBcc(), mail.getSubject(),
					mail.getMsgBody(), mail.getDraft(), mail.getGmailMessageId());
			MultipartFile[] files = mail.getFiles();

			System.err.println("printing the mail :::: " + mail);
			System.err.println("printing the files  :::: " + files);
			System.err.println(" printing the isDraft in controller :::::::::: " + mail.getDraft());
			System.err.println(" printing the GmailMessageId in controller :::::::::: " + mail.getGmailMessageId());

			if (mail.getDraft()) {
				String messageId = emailService.saveDraftEmailInGmail(email, files, mail.getGmailMessageId(),
						mail.getOldFiles());

				System.err.println(" mail.getGmailMessageId() @@@@@@@@@@@@@@@@@@@@@@@@@ " + mail.getGmailMessageId());
				System.err.println(" messageId ###################  " + messageId);
				return emailService.saveDraftEmail(email, files, messageId);
			}
			emailService.SendEmail(email, files);
			return ResponseHandler.responseWithString("Mail send successfully", HttpStatus.OK);
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECSSM-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECSSM-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			e.printStackTrace();
			knownExpLog.error("CECSSM-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECSSM-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			knownExpLog.error("CECSSM-003" + e.getMessage());
			return ResponseHandler.responseWithString("CECSSM-003", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECSSM-004" + e.getMessage());
			return ResponseHandler.responseWithString("CECSSM-004", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("sendSimpleMail (-)");
		}
	}

	@PostMapping("/forwardEmail")
	public ResponseEntity<Object> forwordEmail(@ModelAttribute ForwardEmailDTO req) {
		log.info("forwordEmail (+)");
		try {
			System.out.println("printing the req.toString() :::: " + req.toString());
			emailService.forwardEmail(req.getIds(), req.getTo(), req.getCc(), req.getBcc(), req.getFolderName(),
					req.getContent(), req.getFiles());
			return ResponseHandler.responseWithString("Mail forwarded successfully", HttpStatus.OK);
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECFE-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECFE-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			e.printStackTrace();
			knownExpLog.error("CECFE-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECFE-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECFE-003" + e.getMessage());
			return ResponseHandler.responseWithString("CECFE-003", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("forwordEmail (-)");
		}
	}

	@PostMapping("/replyEmail")
	public ResponseEntity<Object> replayEmail(@ModelAttribute ReplyMailDTO req) {
		log.info("replayEmail (+)");
		try {
			System.out.println("printig the req.toString() ::: " + req.toString());
			emailService.replyEmail(req.getId(), req.getFolderName(), req.getContent(), req.getFiles());
			return ResponseHandler.responseWithString("Reply Mail sent successfully", HttpStatus.OK);
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECRE-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECRE-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			e.printStackTrace();
			knownExpLog.error("CECRE-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECRE-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECRE-003" + e.getMessage());
			return ResponseHandler.responseWithString("CECRE-003", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("replayEmail (-)");
		}
	}

	@PostMapping("/schedule")
	public ResponseEntity<Object> scheduleEmail(@ModelAttribute ScheduleEmailDTO sMail) {
		log.info("scheduleEmail (+)");
		try {

			ScheduledEmail mail = new ScheduledEmail(sMail.getSend_to(), sMail.getFromArr(), sMail.getCc(),
					sMail.getCc(), sMail.getSubject(), sMail.getMsgBody(),
					MailUtil.StringToLocalDateTime(sMail.getScheduledTime()), sMail.getGmailMessageId());
			MultipartFile[] files = sMail.getFiles();

			System.out.println(" scheduleEmail is called !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			System.out.println("printing the mail :::: " + mail);
			System.out.println("printing the files  :::: " + files);

			String messageId = scheduleEmailService.saveScheduleEmailInGmail(mail, files);
			System.err.println(" printing the messageId in schedule controller :::: " + messageId);
			return scheduleEmailService.saveScheduledEmail(mail, files, messageId);
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECSE-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECSE-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (IOException e) {
			e.printStackTrace();
			knownExpLog.error("CECSE-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECSE-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECSE-003" + e.getMessage());
			return ResponseHandler.responseWithString("CECSE-003", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("scheduleEmail (-)");
		}
	}

	@PostMapping("/starred")
	public ResponseEntity<Object> starManipulation(@RequestHeader long id, @RequestHeader boolean starred,
			@RequestHeader String folderName, @RequestHeader String msgId) {
		log.info("starManipulation (+)");
		try {
			System.out.println(
					" /starred  api called  ::   id " + id + " starred " + starred + " folder Name " + folderName);
			emailService.starManipulation(id, starred, folderName, msgId);
			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECSM-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECSM-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECSM-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECSM-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("starManipulation (-)");
		}

	}

	@PostMapping("/read")
	public ResponseEntity<Object> readManipulation(@RequestHeader List<Long> ids, @RequestHeader boolean isRead,
			@RequestHeader String folderName) {
		log.info("readManipulation (+)");
		try {
			emailService.readManipulation(ids, isRead, folderName);
			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECRM-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECRM-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECRM-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECRM-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("readManipulation (-)");
		}
	}

	@PostMapping("/delete")
	public ResponseEntity<Object> moveEmailToTrash(@RequestHeader List<Long> ids, @RequestHeader String folderName) {
		log.info("moveEmailToTrash (+)");
		try {
			emailService.moveEmailToTrash(ids, folderName);
			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECMETT-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECMETT-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("moveEmailToTrash (-)");
		}
	}

	@PostMapping("/move")
	public ResponseEntity<Object> moveEmailToFolder(@RequestHeader List<Long> ids, @RequestHeader String folderName,
			@RequestHeader String newFolderName) {
		log.info("moveEmailToFolder (+)");
		try {
			System.err.println("printing inside the moveEmailToFolder /move .... ids " + ids + " folderName "
					+ folderName + " newFolderName " + newFolderName);
			return emailService.moveToFolder(ids, folderName, newFolderName);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECMETF-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECMETF-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("moveEmailToFolder (-)");
		}
	}

	@PostMapping("/updateLabelsInEmail")
	public ResponseEntity<Object> addOrRemoveLabelToEmail(@RequestBody List<UpdateLabelsInEmailDTO> reqList) {
		log.info("addOrRemoveLabelToEmail (+)");
		try {
			System.out.println(
					"printing the reqList in controller :: reqList.size() " + reqList.size() + " reqList " + reqList);
			emailService.UpdateLabelsToEmail(reqList);
			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECULIE-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECULIE-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECULIE-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECULIE-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("addOrRemoveLabelToEmail (-)");
		}
	}

	@PostMapping("/importantFlag")
	public ResponseEntity<Object> importantFlagManipulation(@RequestHeader List<Long> ids,
			@RequestHeader String folderName) {
		log.info("importantFlagManipulation (+)");
		try {
			return emailService.importantFlagManipulation(ids, folderName);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECIFM-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECIFM-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("importantFlagManipulation (-)");
		}
	}

	@PostMapping("/archive")
	public ResponseEntity<Object> archiveEmail(@RequestHeader List<Long> ids, @RequestHeader String folderName) {
		try {
			log.info("archiveEmail (+)");
			emailService.archiveEmail(ids, folderName);
			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECAE-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECAE-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("archiveEmail (-)");
		}
	}

	@PostMapping("/deleteForever")
	public ResponseEntity<Object> deleteForever(@RequestHeader List<Long> ids, @RequestHeader String folderName) {
		try {
			log.info("deleteForever (+)");
			emailService.deleteForever(ids, folderName);
			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECDF-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECDF-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("deleteForever (-)");
		}
	}

	@GetMapping("/search")
	public ResponseEntity<Object> searchEmails(@RequestHeader String searchTerm) {
		log.info("searchEmails (+)");
		try {
			System.err.println(" printing search term in controller : " + searchTerm);
			return emailService.searchEmailInDB(searchTerm);
		} catch (MessagingException e) {
			e.printStackTrace(); 
			knownExpLog.error("CECSE-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECSE-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECSE-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECSE-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("searchEmails (-)");
		}
	}

	@PostMapping("/filter")
	public ResponseEntity<Object> searchEmailUsingFilters(@ModelAttribute GmailSearchFilter filter) {
		log.info("searchEmailUsingFilters (+)");
		try {
			System.err.println(" printing the filter request in controller :: " + filter);
			return emailService.searchEmailUsingFiltersInDB(filter);
		} catch (ParseException e) {
			e.printStackTrace(); 
			knownExpLog.error("CECSEUF-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECSEUF-001", HttpStatus.INTERNAL_SERVER_ERROR);
		}catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECSEUF-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECSEUF-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("searchEmailUsingFilters (-)");
		}
	}

	@PutMapping("/snooze")
	public ResponseEntity<Object> snoozeEmailMessage(@RequestHeader Long id, @RequestHeader boolean snoozed,
			@RequestHeader String snoozedTime) {
		log.info("snoozeEmailMessage (+)");
		try {
			emailService.snoozeEmailMessage(id, snoozed, MailUtil.StringToLocalDateTime(snoozedTime));
			return ResponseHandler.responseWithString("S", HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECSEM-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECSEM-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("snoozeEmailMessage (-)");
		}
	}

	@GetMapping("/defaultFolders")
	public ResponseEntity<Object> getDefaultFolders() {
		log.info("getDefaultFolders (+)");
		try {
			List<String> defaultFolders = imapLabelService.getDefaultFolders();
			return ResponseHandler.responseWithObject(defaultFolders, HttpStatus.OK);
		}catch (InterruptedException e) {
			e.printStackTrace();
			knownExpLog.error("CECGDF-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECGDF-001", HttpStatus.INTERNAL_SERVER_ERROR);
		}  catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECGDF-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECGDF-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("getDefaultFolders (-)");
		}
	}

	@GetMapping("/syncFolder")
	public ResponseEntity<Object> syncFolder(@RequestHeader String folderName) {
		log.info("syncFolder (+)");
		try {
			return emailService.callSyncFolder(folderName);
		} catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECSF-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECSF-001", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("syncFolder (-)");
		}
	}

	@GetMapping("/fetchRedEmails")
	public ResponseEntity<Object> fetchRedEmails() {
		log.info("fetchRedEmails (+)");
		try {
			return emailService.fetchRedEmails();
		} catch (MessagingException e) {
			e.printStackTrace();
			knownExpLog.error("CECFRE-001" + e.getMessage());
			return ResponseHandler.responseWithString("CECFRE-001", HttpStatus.INTERNAL_SERVER_ERROR);
		}  catch (Exception e) {
			e.printStackTrace();
			unKnownExpLog.error("CECFRE-002" + e.getMessage());
			return ResponseHandler.responseWithString("CECFRE-002", HttpStatus.INTERNAL_SERVER_ERROR);
		} finally {
			log.info("fetchRedEmails (-)");
		}
	}

}
