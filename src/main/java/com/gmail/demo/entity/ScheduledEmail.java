package com.gmail.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gmail.demo.util.MailUtil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class ScheduledEmail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String[] send_to;
	private String fromArr;
	private String cc = "";
	private String bcc = "";
	private String subject;
	private String msgBody;

	@JsonFormat(pattern = "dd/MM/yyyy hh:mm a")
	private LocalDateTime scheduledTime;

	private MailUtil.Status status;
	
	private boolean isDraft;

	@OneToMany(mappedBy = "email", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	List<ScheduledFileDetails> fileList = new ArrayList<ScheduledFileDetails>();
	
	private String gmailMessageId;

	public ScheduledEmail() {
		super();
	}
	
	public ScheduledEmail(String[] send_to, String fromArr, String cc, String bcc, String subject, String msgBody,
			LocalDateTime scheduledTime,String gmailMessageId) {
		super();
		this.send_to = send_to;
		this.fromArr = fromArr;
		this.cc = cc;
		this.bcc = bcc;
		this.subject = subject;
		this.msgBody = msgBody;
		this.scheduledTime = scheduledTime;
		this.gmailMessageId = gmailMessageId;
	}



	public Integer getId() {
		return id;
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

	public LocalDateTime getScheduledTime() {
		return scheduledTime;
	}

	public MailUtil.Status getStatus() {
		return status;
	}

	public void setId(Integer id) {
		this.id = id;
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

	public void setScheduledTime(LocalDateTime scheduledTime) {
		this.scheduledTime = scheduledTime;
	}

	public void setStatus(MailUtil.Status status) {
		this.status = status;
	}
	public List<ScheduledFileDetails> getFileList() {
		return fileList;
	}

	public void setFileList(List<ScheduledFileDetails> fileList) {
		this.fileList = fileList;
	}

	public boolean isDraft() {
		return isDraft;
	}

	public void setDraft(boolean isDraft) {
		this.isDraft = isDraft;
	}

	public String getFromArr() {
		return fromArr;
	}

	public void setFromArr(String fromArr) {
		this.fromArr = fromArr;
	}

	public String getGmailMessageId() {
		return gmailMessageId;
	}

	public void setGmailMessageId(String gmailMessageId) {
		this.gmailMessageId = gmailMessageId;
	}

	@Override
	public String toString() {
		return "ScheduledEmail [id=" + id + ", send_to=" + Arrays.toString(send_to) + ", fromArr=" + fromArr + ", cc="
				+ cc + ", bcc=" + bcc + ", subject=" + subject + ", msgBody=" + msgBody + ", scheduledTime="
				+ scheduledTime + ", status=" + status + ", isDraft=" + isDraft + ", fileList=" + fileList
				+ ", gmailMessageId=" + gmailMessageId + "]";
	}

	
}
