package com.gmail.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class InitialFileDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String name;
	private String location;
	private String date; 
	private char status = '1';
	private String extention;
	private String ogname;
	private long size;
	
	@ManyToOne
	@JoinColumn(name="emailId",nullable = false)
	@JsonIgnore
	private EmailMetadata email;
	
	
	public InitialFileDetails() {
		super();
	}

	public InitialFileDetails(String name, String location, String date, String extention, String ogname,long size) {
		super();
		this.name = name;
		this.location = location;
		this.date = date;
		this.extention = extention;
		this.ogname = ogname;
		this.size = size;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public char getStatus() {
		return status;
	}

	public void setStatus(char status) {
		this.status = status;
	}

	public String getExtention() {
		return extention;
	}

	public void setExtention(String extention) {
		this.extention = extention;
	}

	public String getOgname() {
		return ogname;
	}

	public void setOgname(String ogname) {
		this.ogname = ogname;
	}

	public EmailMetadata getEmail() {
		return email;
	}

	public void setEmail(EmailMetadata email) {
		this.email = email;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	@Override
	public String toString() {
		return "InitialFileDetails [id=" + id + ", name=" + name + ", location=" + location + ", date=" + date
				+ ", status=" + status + ", extention=" + extention + ", ogname=" + ogname + ", size=" + size+ "]";
	}

	
	

}
