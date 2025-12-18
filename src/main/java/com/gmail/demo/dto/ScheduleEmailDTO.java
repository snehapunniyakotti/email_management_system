package com.gmail.demo.dto;

import org.springframework.web.multipart.MultipartFile;

import com.gmail.demo.util.MailUtil;

public class ScheduleEmailDTO {

	private String[] send_to;
	private String fromArr;
	private String cc = "";
	private String bcc = "";
	private String subject;
	private String msgBody;
	private String scheduledTime;
	private MailUtil.Status status;
	private boolean draft;
	private String gmailMessageId;
	private MultipartFile[] Files;
	
	
	public String[] getSend_to() {
		return send_to;
	}
	public void setSend_to(String[] send_to) {
		this.send_to = send_to;
	}
	public String getFromArr() {
		return fromArr;
	}
	public void setFromArr(String fromArr) {
		this.fromArr = fromArr;
	}
	public String getCc() {
		return cc;
	}
	public void setCc(String cc) {
		this.cc = cc;
	}
	public String getBcc() {
		return bcc;
	}
	public void setBcc(String bcc) {
		this.bcc = bcc;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getMsgBody() {
		return msgBody;
	}
	public void setMsgBody(String msgBody) {
		this.msgBody = msgBody;
	}
	public String getScheduledTime() {
		return scheduledTime;
	}
	public void setScheduledTime(String scheduledTime) {
		this.scheduledTime = scheduledTime;
	}
	public MailUtil.Status getStatus() {
		return status;
	}
	public void setStatus(MailUtil.Status status) {
		this.status = status;
	}
	public boolean isDraft() {
		return draft;
	}
	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	public String getGmailMessageId() {
		return gmailMessageId;
	}
	public void setGmailMessageId(String gmailMessageId) {
		this.gmailMessageId = gmailMessageId;
	}
	public MultipartFile[] getFiles() {
		return Files;
	}
	public void setFiles(MultipartFile[] files) {
		Files = files;
	}
	
	
	

}
