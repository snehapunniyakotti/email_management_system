package com.gmail.demo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;

@Entity
@Table(name = "emails", uniqueConstraints = @UniqueConstraint(columnNames = { "folder", "uid" }))
public class EmailMetadata {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String folder;
	private long uid;
	private long uidValidity;

	private String messageId;

	@Column(length = 1024)
	private String subject;
	@Column(length = 1024)
	private String fromAddr;
	@Column(length = 2048)
	private String toAddr;
	@Column(length = 2048)
	private String  cc;
	@Column(length = 2048)
	private String bcc;

	@Temporal(TemporalType.TIMESTAMP)
	private Date sentAt;

	@Column(length = 256)
	private List<String> flags;
	private long sizeBytes;

	@Column(length = 512)
	private String snippet;
	private boolean hasAttachments;
	private boolean bodyCached;
	
	private boolean starred;
	private boolean read;
	private List<String> labels;
	
	private boolean deleteFlag;
	
	@Column(length = 128)
	private String mimeType;

	@Lob
	private String body;
	
	private boolean snoozed;
	
	@JsonFormat(pattern = "dd/MM/yyyy hh:mm a")
	private LocalDateTime snoozedSetTime;
	
	
	@JsonFormat(pattern = "dd/MM/yyyy hh:mm a")
	private LocalDateTime snoozedTime;
	
	private boolean inboxUnique;
	
	@OneToMany(mappedBy = "email", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	List<InitialFileDetails> fileList = new ArrayList<InitialFileDetails>();
	
	
	public Long getId() {
		return id;
	}
	public String getFolder() {
		return folder;
	}
	public long getUid() {
		return uid;
	}
	public long getUidValidity() {
		return uidValidity;
	}
	public String getMessageId() {
		return messageId;
	}
	public String getSubject() {
		return subject;
	}
	public String getFromAddr() {
		return fromAddr;
	}
	public String getToAddr() {
		return toAddr;
	}
	public Date getSentAt() {
		return sentAt;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}
	public String getSnippet() {
		return snippet;
	}
	public boolean isHasAttachments() {
		return hasAttachments;
	}
	public boolean isBodyCached() {
		return bodyCached;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public void setUidValidity(long uidValidity) {
		this.uidValidity = uidValidity;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public void setFromAddr(String fromAddr) {
		this.fromAddr = fromAddr;
	}
	public void setToAddr(String toAddr) {
		this.toAddr = toAddr;
	}
	public void setSentAt(Date sentAt) {
		this.sentAt = sentAt;
	}

	public void setSizeBytes(long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}
	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	public void setHasAttachments(boolean hasAttachments) {
		this.hasAttachments = hasAttachments;
	}
	public void setBodyCached(boolean bodyCached) {
		this.bodyCached = bodyCached;
	}
	public boolean isStarred() {
		return starred;
	}
	public boolean isRead() {
		return read;
	}
	public void setStarred(boolean starred) {
		this.starred = starred;
	}
	public void setRead(boolean read) {
		this.read = read;
	}
	public List<String> getLabels() {
		return labels;
	}
	public void setLabels(List<String> labels) {
		this.labels = labels;
	}
	public boolean getDeleteFlag() {
		return deleteFlag;
	}
	public void setDeleteFlag(boolean deleteFlag) {
		this.deleteFlag = deleteFlag;
	}
	public String getMimeType() {
		return mimeType;
	}
	public String getBody() {
		return body;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public boolean getSnoozed() {
		return snoozed;
	}
	public void setSnoozed(boolean snoozed) {
		this.snoozed = snoozed;
	}
	public LocalDateTime getSnoozedTime() {
		return snoozedTime;
	}
	public void setSnoozedTime(LocalDateTime snoozedTime) {
		this.snoozedTime = snoozedTime;
	}
	public LocalDateTime getSnoozedSetTime() {
		return snoozedSetTime;
	}
	public void setSnoozedSetTime(LocalDateTime snoozedSetTime) {
		this.snoozedSetTime = snoozedSetTime;
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
	public List<String> getFlags() {
		return flags;
	}
	public void setFlags(List<String> flags) {
		this.flags = flags;
	}
	public boolean isInboxUnique() {
		return inboxUnique;
	}
	public void setInboxUnique(boolean inboxUnique) {
		this.inboxUnique = inboxUnique;
	}
	public List<InitialFileDetails> getFileList() {
		return fileList;
	}
	public void setFileList(List<InitialFileDetails> fileList) {
		this.fileList = fileList;
	}
	@Override
	public String toString() {
		return "EmailMetadata [id=" + id + ", folder=" + folder + ", uid=" + uid + ", messageId=" + messageId
				+ ", subject=" + subject + ", fromAddr=" + fromAddr + ", toAddr=" + toAddr + ", sentAt=" + sentAt
				+ ", flags=" + flags + ", hasAttachments=" + hasAttachments + ", labels=" + labels + ", deleteFlag="
				+ deleteFlag + "]";
	}
	

}
