package com.gmail.demo.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gmail.demo.entity.EmailMetadata;
import com.gmail.demo.entity.ScheduledEmail;
import com.gmail.demo.repository.EmailRepo;
import com.gmail.demo.repository.ScheduledEmailRepo;
import com.gmail.demo.service.api.ScheduleEmailService;
import com.gmail.demo.service.imaps.GmailStoreService;
import com.gmail.demo.util.MailUtil;
import com.sun.mail.gimap.GmailFolder;

import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Store;

@Component
public class EmailScheduler {

	private final ScheduleEmailService scheduleEmailService;
	private final ScheduledEmailRepo schEmailRepo;
	private final EmailRepo emailRepo;
	private final GmailStoreService gmailStoreService;
	
	private static final Logger log = LoggerFactory.getLogger(EmailScheduler.class);

	public EmailScheduler(ScheduleEmailService service, ScheduledEmailRepo repo, EmailRepo emailRepo,
			GmailStoreService gmailStoreService) {
		this.scheduleEmailService = service;
		this.schEmailRepo = repo;
		this.emailRepo = emailRepo;
		this.gmailStoreService = gmailStoreService;
	}

	public void sendScheduledEmails() {

		log.info("sendScheduledEmails (+) ");
		List<ScheduledEmail> emailsToSend = schEmailRepo.findByStatusAndScheduledTimeBefore(MailUtil.Status.PENDING,
				LocalDateTime.now());

		log.info(" emailsToSend  size of the shedule email !!!!!!!!!!!!!! " + emailsToSend.size());

		for (ScheduledEmail email : emailsToSend) {
			try {
				/* sent scheduled email */
				scheduleEmailService.sendScheduledEmail(email);
				email.setStatus(MailUtil.Status.SENT);
				schEmailRepo.save(email);

				/* update in [Gmail]/Schedule folder */
				if (email.getGmailMessageId() != null && !email.getGmailMessageId().isEmpty()) {
					
					log.info("email.getGmailMessageId() : "+email.getGmailMessageId());
					
					List<EmailMetadata> emdList = emailRepo.findByMessageIdAndFolder(email.getGmailMessageId(),
							MailUtil.GmailSchedule);

					log.info(" emdList : "+emdList);
					
					for (EmailMetadata emd : emdList) {
						long uid = emd.getUid();
						log.info(" uid : "+uid);
						
						try (Store store = gmailStoreService.getStore()) {
							GmailFolder scheduleFolder = (GmailFolder) store.getFolder(MailUtil.GmailSchedule);
							if ((scheduleFolder.getType() & Folder.HOLDS_MESSAGES) == 0 && scheduleFolder.exists()) {
								return;
							}

							scheduleFolder.open(Folder.READ_WRITE);
							Message deleteMsgForever = scheduleFolder.getMessageByUID(uid);

							if (deleteMsgForever != null) {
								if (!deleteMsgForever.isExpunged()) {
									Flags deleteFlag = new Flags(Flags.Flag.DELETED);
									scheduleFolder.setFlags(new Message[] { deleteMsgForever }, deleteFlag, true);
								}

							} 
							
							emd.setDeleteFlag(true);
							emailRepo.save(emd);

							if (scheduleFolder != null && scheduleFolder.isOpen()) {
								/* Expunge the source folder to remove the marked message */
								scheduleFolder.close(true);
							}

						}

					}
				}

			} catch (Exception e) {
				email.setStatus(MailUtil.Status.FAILED);
				schEmailRepo.save(email);
				log.error("Failed to send scheduled email with ID: " + email.getId());

				e.printStackTrace();
			}
		}
	}

}
