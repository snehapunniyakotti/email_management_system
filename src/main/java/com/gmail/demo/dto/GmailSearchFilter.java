package com.gmail.demo.dto;


import com.gmail.demo.util.MailUtil;

public class GmailSearchFilter {

	private String fromAdd = "";
	private String toAdd = "";
	private String subject = "";
	private String hasTheWord = "";
	private String doesNotHave = "";
	private int sizeValue = 0;
	private MailUtil.Range sizeRange;
	private MailUtil.Size size;
	private int dateWithinValue = 0;
	private MailUtil.Period dateWithinPeriod;
	private String date = "";
	private String folder = "";
	private boolean hasAttachment;
	private boolean read;
	private boolean star;
	

	public String getFromAdd() {
		return fromAdd;
	}

	public String getToAdd() {
		return toAdd;
	}

	public String getSubject() {
		return subject;
	}

	public String getHasTheWord() {
		return hasTheWord;
	}

	public String getDoesNotHave() {
		return doesNotHave;
	}

	public String getDate() {
		return date;
	}

	public String getFolder() {
		return folder;
	}

	public boolean isHasAttachment() {
		return hasAttachment;
	}

	public void setFromAdd(String fromAdd) {
		this.fromAdd = fromAdd;
	}

	public void setToAdd(String toAdd) {
		this.toAdd = toAdd;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setHasTheWord(String hasTheWord) {
		this.hasTheWord = hasTheWord;
	}

	public void setDoesNotHave(String doesNotHave) {
		this.doesNotHave = doesNotHave;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public void setHasAttachment(boolean hasAttachment) {
		this.hasAttachment = hasAttachment;
	}

	public int getSizeValue() {
		return sizeValue;
	}

	public MailUtil.Range getSizeRange() {
		return sizeRange;
	}

	public MailUtil.Size getSize() {
		return size;
	}

	public int getDateWithinValue() {
		return dateWithinValue;
	}

	public MailUtil.Period getDateWithinPeriod() {
		return dateWithinPeriod;
	}

	public void setSizeValue(int sizeValue) {
		this.sizeValue = sizeValue;
	}

	public void setSizeRange(MailUtil.Range sizeRange) {
		this.sizeRange = sizeRange;
	}

	public void setSize(MailUtil.Size size) {
		this.size = size;
	}

	public void setDateWithinValue(int dateWithinValue) {
		this.dateWithinValue = dateWithinValue;
	}

	public void setDateWithinPeriod(MailUtil.Period dateWithinPeriod) {
		this.dateWithinPeriod = dateWithinPeriod;
	}

	public boolean getRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public boolean getStar() {  
		return star;
	}

	public void setStar(boolean star) {
		this.star = star;
	}

	@Override
	public String toString() {
		return "GmailSearchFilter [fromAdd=" + fromAdd + ", toAdd=" + toAdd + ", subject=" + subject + ", hasTheWord="
				+ hasTheWord + ", doesNotHave=" + doesNotHave + ", sizeValue=" + sizeValue + ", sizeRange=" + sizeRange
				+ ", size=" + size + ", dateWithinValue=" + dateWithinValue + ", dateWithinPeriod=" + dateWithinPeriod
				+ ", date=" + date + ", folder=" + folder + ", hasAttachment=" + hasAttachment + ", read=" + read
				+ ", star=" + star + "]";
	}


	
	

}
