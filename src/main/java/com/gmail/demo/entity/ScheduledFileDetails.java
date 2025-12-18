package com.gmail.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ScheduledFileDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;
	private String location;
	private String date; 
	private char status = '1';
	private String extention;
	private String ogname;
	
	@ManyToOne
	@JoinColumn(name="emailId",nullable = false)
	@JsonIgnore
	private ScheduledEmail email;

	public ScheduledFileDetails() {
		super();
	}

	public ScheduledFileDetails(String name, String location, String date, String extention, String ogname) {
		super();
		this.name = name;
		this.location = location;
		this.date = date;
		this.extention = extention;
		this.ogname = ogname;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getLocation() {
		return location;
	}

	public String getDate() {
		return date;
	}

	public char getStatus() {
		return status;
	}

	public String getExtention() {
		return extention;
	}

	public String getOgname() {
		return ogname;
	}

	public ScheduledEmail getEmail() {
		return email;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setStatus(char status) {
		this.status = status;
	}

	public void setExtention(String extention) {
		this.extention = extention;
	}

	public void setOgname(String ogname) {
		this.ogname = ogname;
	}

	public void setEmail(ScheduledEmail email) {
		this.email = email;
	}
	
	

}
