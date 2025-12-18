package com.gmail.demo.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class MailTemplete {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String[] send_to;
	private String cc ="";
	private String bcc ="";
	private String subject;
	private String msgBody;
	
//	@JsonProperty("isDraft") 
	private boolean draft;
	
	@OneToMany(mappedBy = "email", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	List<DraftFileDetails> fileList = new ArrayList<DraftFileDetails>();

	private String gmailMessageId;
	
	public MailTemplete() {
		super();
	}

	public MailTemplete(String[] send_to, String cc, String bcc, String subject, String msgBody) {
		super();
		this.send_to = send_to;
		this.cc = cc;
		this.bcc = bcc;
		this.subject = subject;
		this.msgBody = msgBody;
	}
	
	

	public MailTemplete(String[] send_to, String cc, String bcc, String subject, String msgBody, boolean draft,
			String gmailMessageId) {
		super();
		this.send_to = send_to;
		this.cc = cc;
		this.bcc = bcc;
		this.subject = subject;
		this.msgBody = msgBody;
		this.draft = draft;
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

	public List<DraftFileDetails> getFileList() {
		return fileList;
	}

	public boolean getDraft() {
		return draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}

	public void setFileList(List<DraftFileDetails> fileList) {
		this.fileList = fileList;
	}

	public String getGmailMessageId() {
		return gmailMessageId;
	}

	public void setGmailMessageId(String gmailMessageId) {
		this.gmailMessageId = gmailMessageId;
	}

	@Override
	public String toString() {
		return "MailTemplete [id=" + id + ", send_to=" + Arrays.toString(send_to) + ", cc=" + cc + ", bcc=" + bcc
				+ ", subject=" + subject + ", msgBody=" + msgBody + ", draft=" + draft + ", fileList=" + fileList
				+ ", gmailMessageId=" + gmailMessageId + "]";
	}

	

}
