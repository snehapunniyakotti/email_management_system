package com.gmail.demo.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class OAuth2Token {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String userEmail;
	@Column(name = "access_token", length = 2048)
	private String accessToken;
	@Column(name = "refresh_token", length = 2048)
	private String refreshToken;

	private Instant expiresAt;
	
	public Boolean isTokenExpires() {
		return this.expiresAt.isBefore(Instant.now());
	} 

	public Long getId() {
		return id;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

}
