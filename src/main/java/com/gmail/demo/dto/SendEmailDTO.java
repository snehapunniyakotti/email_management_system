package com.gmail.demo.dto;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.gmail.demo.entity.MailTemplete;

public class SendEmailDTO {

	private MailTemplete mail;
	
	private String[] send_to;
	private String cc ="";
	private String bcc ="";
	private String subject;
	private String msgBody;
	private boolean draft;
	private String gmailMessageId;
	private MultipartFile[] Files;
	private List<Integer> oldFiles;
	
	
	public MailTemplete getMail() {
		return mail;
	}
	public String[] getSend_to() {
		return send_to;
	}
	public String getCc() {
		return cc;
	}
	public String getBcc() {
		return bcc;
	}
	public String getSubject() {
		return subject;
	}
	public String getMsgBody() {
		return msgBody;
	}
	public boolean getDraft() {
		return draft;
	}
	public String getGmailMessageId() {
		return gmailMessageId;
	}
	public MultipartFile[] getFiles() {
		return Files;
	}
	public void setMail(MailTemplete mail) {
		this.mail = mail;
	}
	public void setSend_to(String[] send_to) {
		this.send_to = send_to;
	}
	public void setCc(String cc) {
		this.cc = cc;
	}
	public void setBcc(String bcc) {
		this.bcc = bcc;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public void setMsgBody(String msgBody) {
		this.msgBody = msgBody;
	}
	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	public void setGmailMessageId(String gmailMessageId) {
		this.gmailMessageId = gmailMessageId;
	}
	public void setFiles(MultipartFile[] files) {
		Files = files;
	}
	public List<Integer> getOldFiles() {
		return oldFiles;
	}
	public void setOldFiles(List<Integer> oldFiles) {
		this.oldFiles = oldFiles;
	}
	@Override
	public String toString() {
		return "SendEmailDTO [mail=" + mail + ", send_to=" + Arrays.toString(send_to) + ", cc=" + cc + ", bcc=" + bcc
				+ ", subject=" + subject + ", msgBody=" + msgBody + ", draft=" + draft + ", gmailMessageId="
				+ gmailMessageId + ", Files=" + Arrays.toString(Files) + ", oldFiles=" + oldFiles + "]";
	}
	
}
