package com.gmail.demo.service.imaps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gmail.demo.entity.EmailMetadata;
import com.gmail.demo.entity.InitialFileDetails;
import com.gmail.demo.repository.EmailRepo;
import com.gmail.demo.util.MailUtil;
import com.sun.mail.gimap.GmailMessage;
import com.sun.mail.imap.IMAPFolder;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Flags.Flag;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

@Component
public class PersistMailMessage {

	@Autowired
	private EmailRepo emailRepo;

	private static String FILE_DIRECTORY = "/home/sneha/Sneha/SpringBoot/Task/Gmail/InitialFiles/";

	public void save(IMAPFolder f, String folderName, Message[] msgs) throws MessagingException, IOException  {
		for (Message m : msgs) {
			EmailMetadata e = new EmailMetadata();

			// avoid repeated email message if came
			long uid = f.getUID(m);
			boolean exist = emailRepo.existsByFolderStartWithAndUid(folderName, uid);
			if (exist && !folderName.startsWith(MailUtil.Inbox)) {
				continue;
			}

			if (!exist && folderName.startsWith(MailUtil.Inbox)) {
				e.setInboxUnique(true);
			}

			// --- getting flags ---
			Flags flags = m.getFlags();
			boolean isRead = flags.contains(Flag.SEEN);
			boolean isStarred = flags.contains(Flag.FLAGGED);

			List<String> flagArr = new ArrayList<String>();

			if (flags.contains(Flag.SEEN))
				flagArr.add("SEEN");
			if (flags.contains(Flag.USER))
				flagArr.add("USER");
			if (flags.contains(Flag.RECENT))
				flagArr.add("RECENT");
			if (flags.contains(Flag.FLAGGED))
				flagArr.add("FLAGGED");
			if (flags.contains(Flag.DRAFT))
				flagArr.add("DRAFT");
			if (flags.contains(Flag.DELETED))
				flagArr.add("DELETED");
			if (flags.contains(Flag.ANSWERED))
				flagArr.add("ANSWERED");

			// --- getting labels ---
			List<String> labelList = new ArrayList<String>();

			GmailMessage gmailMessage = (GmailMessage) m;
			String[] labels = gmailMessage.getLabels();

			if (labels.length > 0) {
				for (String label : labels) {
					labelList.add(label.replace("\\", ""));
				}
			}

			// extracting from ,to ,cc ,bcc
			String from = Optional.ofNullable(m.getFrom()).filter(a -> a.length > 0).map(a -> a[0].toString())
					.orElse(null);
			String to = Optional.ofNullable(m.getRecipients(Message.RecipientType.TO)).map(Arrays::stream)
					.orElseGet(Stream::empty).map(Address::toString).collect(Collectors.joining(", "));
			String cc = Optional.ofNullable(m.getRecipients(Message.RecipientType.CC)).map(Arrays::stream)
					.orElseGet(Stream::empty).map(Address::toString).collect(Collectors.joining(", "));
			String bcc = Optional.ofNullable(m.getRecipients(Message.RecipientType.BCC)).map(Arrays::stream)
					.orElseGet(Stream::empty).map(Address::toString).collect(Collectors.joining(", "));

			String mime = m.getContentType();
			String body = extractText(m);

			List<InitialFileDetails> files = extractAndSaveAttachments(m);

			if (files != null) {
				for (InitialFileDetails file : files) {
					file.setEmail(e);
				}
			}

			e.setFolder(folderName);
			e.setUid(uid);
			e.setUidValidity(f.getUIDValidity());
			e.setMessageId(((MimeMessage) m).getMessageID());
			e.setSubject(m.getSubject());
			e.setFromAddr(from);
			e.setToAddr(to);
			e.setSentAt(m.getSentDate());
			e.setFlags(flagArr);
			e.setSizeBytes(m.getSize());
			e.setSnippet(null);
			e.setHasAttachments(!files.isEmpty());
			e.setBodyCached(!body.isEmpty());
			e.setStarred(isStarred);
			e.setRead(isRead);
			e.setLabels(labelList);
			e.setDeleteFlag(false);
			e.setMimeType(mime);
			e.setBody(body);
			e.setBcc(bcc);
			e.setCc(cc);
			e.setFileList(files);

			emailRepo.save(e);
		}
	}

	private String extractText(Message message) throws MessagingException, IOException {
		if (message.isMimeType("text/plain") || message.isMimeType("text/html"))
			return (String) message.getContent();
		if (message.isMimeType("multipart/*")) {
			MimeMultipart mp = (MimeMultipart) message.getContent();
			return getTextFromMimeMultipart(mp);
		}
		return "";
	}

	private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException  {
		StringBuilder result = new StringBuilder();
		int count = mimeMultipart.getCount();

		for (int i = 0; i < count; i++) {
			BodyPart bodyPart = mimeMultipart.getBodyPart(i);
			Object content = bodyPart.getContent();

			if (content instanceof String) {
				// plain text and html text having the same content.
				if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
				  		result.setLength(0);   // resetting length to 0 ,becoz content get duplicated  	
				    	result.append((String) content);
				} 
				
			} else if (content instanceof MimeMultipart) {
				result.append(getTextFromMimeMultipart((MimeMultipart) content));
			}
		} 
		return result.toString();
	}

	private List<InitialFileDetails> extractAndSaveAttachments(Message message) throws IOException, MessagingException  {
        List<InitialFileDetails> fileList = new ArrayList<>();
        Path fileStorageLocation = Paths.get(FILE_DIRECTORY).toAbsolutePath().normalize();
        Files.createDirectories(fileStorageLocation);

        if (message.isMimeType("multipart/*")) {
            MimeMultipart mp = (MimeMultipart) message.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);

                if (Part.ATTACHMENT.equalsIgnoreCase(bp.getDisposition()) || bp.getFileName() != null) {
                    MimeBodyPart mimeBodyPart = (MimeBodyPart) bp;
                    String originalName = mimeBodyPart.getFileName();
                    
                    if (originalName != null && !originalName.isEmpty()) { 
                        String randomName = String.valueOf(new Random().nextInt(10000000));
                        String extension = "";
                        String originalNameWithoutExtension = originalName;
                        int lastDotIndex = originalName.lastIndexOf('.');
                        if (lastDotIndex != -1) {
                            extension = originalName.substring(originalName.lastIndexOf('.'));
                            originalNameWithoutExtension = originalName.substring(0, lastDotIndex);
                        }

                        Path targetPath = fileStorageLocation.resolve(randomName + extension);

                        try (InputStream inputStream = mimeBodyPart.getInputStream()) {
                        	long fileSize = mimeBodyPart.getSize();
                            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            
                            fileList.add(new InitialFileDetails(
                                randomName, 
//                                targetPath.toString(), 
                                fileStorageLocation.toString(),
                                getCurDateTime(), 
                                extension, 
                                originalNameWithoutExtension,
                                fileSize
                            ));
                            System.out.println("Saved attachment: " + originalName);
                        }
                    }
                }
            }
        }
        return fileList;
    }

	public String getCurDateTime() {
		Date currentDateTime = new Date();
		SimpleDateFormat formatedDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String datetime = formatedDateTime.format(currentDateTime);
		return datetime;
	}

}
