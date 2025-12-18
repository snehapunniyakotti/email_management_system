package com.gmail.demo.entity;

import java.util.Date;

import jakarta.persistence.*;

@Entity
public class EmailMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String subject;
	private String fromAddress;

	private String content;
	private Date receivedDate;

	public Long getId() {
		return id;
	}

	public String getSubject() {
		return subject;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public String getContent() {
		return content;
	}

	public Date getReceivedDate() {
		return receivedDate;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}

}
