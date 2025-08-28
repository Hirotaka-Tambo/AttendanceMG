package com.example.attendance.dto;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * Servlet implementation class User
 */
@WebServlet("/User")
public class User extends HttpServlet {
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
	
	
	
	public String getPassword() {
		return password;
	}
	
	
	
	public String getRole() {
		return role;
	}
	
	
	
	public boolean isEnabled() {
		return enabled;
	}
	
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	

}
