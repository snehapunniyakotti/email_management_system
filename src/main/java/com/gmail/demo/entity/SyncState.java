package com.gmail.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sync_state")
public class SyncState {
	@Id
	private String folder;

	private long uidValidity;
	private long lastSyncedUid;

	public String getFolder() {
		return folder;
	}

	public long getUidValidity() {
		return uidValidity;
	}

	public long getLastSyncedUid() {
		return lastSyncedUid;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public void setUidValidity(long uidValidity) {
		this.uidValidity = uidValidity;
	}

	public void setLastSyncedUid(long lastSyncedUid) {
		this.lastSyncedUid = lastSyncedUid;
	}

}
