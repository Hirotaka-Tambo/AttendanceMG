package com.example.attendance.dto;

/**
 * Servlet implementation class User
 */
public class User {
	private String username;
	private String password;
	private String role;
	private boolean enabled; //New Field
	
	
	public User(String username, String password, String role) {
        this(username, password, role, true); // Default to enabled
        
	}
	
    public User(String username, String password, String role, boolean enabled) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
        
    }
	
	public String getUsername() {
		return username;
	}
	
	
	
	public void setUsername(String username) {
		this.username = username;
	}

	
	public String getPassword() {
		return password;
	}
	
	
	public void setPassword(String password) {
		this.password = password;
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
