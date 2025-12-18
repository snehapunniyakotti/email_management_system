package com.gmail.demo.dto;

import java.util.List;
import java.util.Map;

public class UpdateLabelsInEmailDTO {

	private long id;
	private List<Map<String, Boolean>> labels;
	private String folderName;
	private  String msgId;

	public long getId() {
		return id;
	}

	public String getFolderName() {
		return folderName;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

	public List<Map<String, Boolean>> getLabels() {
		return labels;
	}

	public void setLabels(List<Map<String, Boolean>> labels) {
		this.labels = labels;
	}

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	
	

}
