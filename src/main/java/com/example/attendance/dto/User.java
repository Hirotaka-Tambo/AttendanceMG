package com.example.attendance.dto;

/**
 * Servlet implementation class User
 */
public class User {
	private String username;
	private String passwordHash;
	private String salt;
	private String role;
	private boolean enabled;
	
	
	public User(String username, String passwordHash, String salt, String role, boolean enabled) {
		super();
		this.username = username;
		this.passwordHash = passwordHash;
		this.salt = salt;
		this.role = role;
		this.enabled = enabled;
	}


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getPasswordHash() {
		return passwordHash;
	}


	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}


	public String getSalt() {
		return salt;
	}


	public void setSalt(String salt) {
		this.salt = salt;
	}


	public String getRole() {
		return role;
	}


	public void setRole(String role) {
		this.role = role;
	}


	public boolean isEnabled() {
		return enabled;
	}


	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	} 
	

}
