package com.gmail.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "email_bodies")
public class EmailBody {
	
	@Id
	private Long emailId;

	@Column(length = 128)
	private String mimeType;

	@Lob
	private String body;

	public Long getEmailId() {
		return emailId;
	}

	public String getMimeType() {
		return mimeType;
	}

	public String getBody() {
		return body;
	}

	public void setEmailId(Long emailId) {
		this.emailId = emailId;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	
}
