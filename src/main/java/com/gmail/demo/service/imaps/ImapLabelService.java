package com.gmail.demo.service.imaps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.gmail.demo.config.MailConfiguration;
import com.gmail.demo.handler.ResponseHandler;

import jakarta.mail.Folder;
import jakarta.mail.Session;
import jakarta.mail.Store;

@Service
public class ImapLabelService {

	@Autowired
	private MailConfiguration props;

	@Autowired
	private ImapBasicService imapBasicService;

	private List<String> res;
	private String errMsg;
	
	public List<String> getDefaultFolders() throws InterruptedException   {
		return imapBasicService.defaultFolders();
	}

	public List<String> getLabelList() throws InterruptedException  {
		return imapBasicService.availableLabels(getDefaultFolders());
	}

	public void createLabel(Folder folder) throws Exception {
		if (!folder.exists()) {
			boolean created = folder.create(Folder.HOLDS_MESSAGES);
			if (created) {
				res = getLabelList();
			} else {
				errMsg = "Failed to create nested label ";
			}
		} else {
			errMsg = " already exists.";
		}

	}

	public void deleteLabel(Folder folder) throws Exception {
		if (folder.exists()) {
			boolean deleted = folder.delete(true);
			if (deleted) {
				res = getLabelList();
			} else {
				errMsg = "Failed to delete nested label ";
			}
		} else {
			errMsg = " folder not exists.";
		}

	}

	public void editLabel(Folder oldfolder, String newfoldername, Store store) throws Exception {
		if (oldfolder.exists()) {
			boolean edited = oldfolder.renameTo(store.getFolder(newfoldername));
			if (edited) {
				res = getLabelList();
			} else {
				errMsg = "Failed to rename folder ";
			}
		} else {
			errMsg = " folder not exists.";
		}

	}

	public ResponseEntity<Object> manipulateLabel(String labelName, String action, String newLabelName) throws InterruptedException {

		System.out.println(" labelName  :::  " + labelName);

		res = new ArrayList<String>();
		errMsg = "";

		try {

			Session session = props.mailSession();

			Store store = props.mailStore(session);
			store.connect(props.getImapHost(), props.getImapUsername(), props.getImapPassword());

			Folder defaultFolder = store.getDefaultFolder();

			Folder folder = defaultFolder.getFolder(labelName);

			action = action.toUpperCase();
			switch (action) {
			case "ADD":
				createLabel(folder);
				break;
			case "DELETE":
				deleteLabel(folder);
				break;
			case "EDIT":
				editLabel(folder, newLabelName, store);
				break;
			}
			store.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			Thread.sleep(2000);
		}
		return ResponseHandler.responseWithObject(res, errMsg, HttpStatus.OK);

	}

}
