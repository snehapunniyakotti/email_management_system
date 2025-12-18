package com.gmail.demo.service.api;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.gmail.demo.entity.EmailMetadata;
import com.gmail.demo.repository.EmailRepo;

@Component
public class SnoozedEmailService {

	private final EmailRepo emailRepo;
	private final GmailAPIService gmailAPIService;

	public SnoozedEmailService(EmailRepo emailRepo, GmailAPIService gmailAPIService) {
		super();
		this.emailRepo = emailRepo;
		this.gmailAPIService = gmailAPIService;
	}

	public void markSnoozedByMessageId() throws GeneralSecurityException, IOException   {
		System.err.println("mark snoozed by message id !!!!!!!!!!!!!!!!!!!!!!!!!!!");

		/* local server */
		List<EmailMetadata> snoozedMailInLocal = emailRepo.findBySnoozed(true);
		
		/* Get all snoozed email message IDs from the server into a Set for fast lookup*/
		Set<String> snoozedMsgIds = gmailAPIService.getSnoozedMsgIds().stream().collect(Collectors.toSet());

		if (!snoozedMsgIds.isEmpty()) {
			/* A separate list to hold the EmailMetadata objects from the server */
		    List<EmailMetadata> gmailSnoozedMails = new ArrayList<>(); 

			/* Process all snoozed mails from the server and update local status */
		    for (String msgId : snoozedMsgIds) {
		        List<EmailMetadata> emdList = emailRepo.findByMessageId(msgId);
		        for (EmailMetadata emd : emdList) {
		            gmailSnoozedMails.add(emd);
		            if (!emd.getSnoozed()) {
		                System.err.println("mark snoozed by message id !!!!!!!!!!! affected emd.getId :" + emd.getId());
		                emd.setSnoozed(true);
		                emailRepo.save(emd);
		            }
		        }
		    }
		    
			/* Now, go through the local snoozed mails and check if they are still snoozed on the server */
		    for (EmailMetadata mail : snoozedMailInLocal) {
				/* Here's the fixed comparison logic */
		    	if (!snoozedMsgIds.contains(mail.getMessageId())) {
		            System.out.println("Mail no longer snoozed on server, updating local status for id: " + mail.getId());
		            mail.setSnoozed(false);
		            emailRepo.save(mail);
		        }
		    }
		}

	}

}
