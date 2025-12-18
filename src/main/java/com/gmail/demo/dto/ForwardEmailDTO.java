package com.gmail.demo.dto;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class ForwardEmailDTO {
	
	private List<Long> ids;
	private String folderName;
	private String[] to;
	private String[] cc;
	private String[] bcc;
	private String content;// initial content
	private MultipartFile[] Files;
	
	
	public List<Long> getIds() {
		return ids;
	}
	public void setIds(List<Long> ids) {
		this.ids = ids;
	}
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
	public String[] getTo() {
		return to;
	}
	public void setTo(String[] to) {
		this.to = to;
	}
	public String[] getCc() {
		return cc;
	}
	public void setCc(String[] cc) {
		this.cc = cc;
	}
	public String[] getBcc() {
		return bcc;
	}
	public void setBcc(String[] bcc) {
		this.bcc = bcc;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public MultipartFile[] getFiles() {
		return Files;
	}
	public void setFiles(MultipartFile[] files) {
		Files = files;
	}
	@Override
	public String toString() {
		return "ForwardEmailDTO [ids=" + ids + ", folderName=" + folderName + ", to=" + Arrays.toString(to) + ", cc="
				+ Arrays.toString(cc) + ", bcc=" + Arrays.toString(bcc) + ", content=" + content + ", Files="
				+ Arrays.toString(Files) + "]";
	}
	
	

}
