package com.gmail.demo.dto;

import java.util.Arrays;

import org.springframework.web.multipart.MultipartFile;

public class ReplyMailDTO {

    private long id;
    private String folderName;
    private String content;
    private MultipartFile[] Files;
    
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
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
		return "ReplyMailDTO [id=" + id + ", folderName=" + folderName + ", content=" + content + ", Files="
				+ Arrays.toString(Files) + "]";
	}
    
}
