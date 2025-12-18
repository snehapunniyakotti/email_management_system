package com.gmail.demo.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.sun.mail.gimap.GmailFolder;

public class MailUtil {

	public static String[] Categories = { "category:personal", "category:promotions", "category:social",
			"category:updates", "category:forums" };

	public static enum Status {
		SENT, PENDING, FAILED
	}

	public static enum Range {
		GT, LT
	}

	public static enum Size {
		MB, KB, Bytes
	}

	public static enum Period {
		DAY, WEEK, MONTH, YEAR
	}
	
	public static String read = "read";
	public static String unread = "unread";
	
	public static String anywhere = "anywhere";

	public static String GmailArchive = "[Gmail]/Archive";
	public static String GmailSchedule = "[Gmail]/Schedule";
	public static String GmailAllMails = "[Gmail]/All Mail";
	public static String Gmail = "[Gmail]";
	public static String Inbox = "INBOX";
	public static String GmailDrafts = "[Gmail]/Drafts";
	public static String GmailTrash = "[Gmail]/Trash";
	public static String GmailSpam = "[Gmail]/Spam";
	public static String GmailSnooze = "[Gmail]/Snoozed";
	public static String GmailImportant = "[Gmail]/Important";
	public static String InboxPersonal = "INBOX/personal";

	public static LocalDateTime StringToLocalDateTime(String dateTimeString) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
		return LocalDateTime.parse(dateTimeString, formatter);
	}
	 
	

}
