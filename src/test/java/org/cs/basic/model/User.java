package org.cs.basic.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="t_user")
public class User {
	private int id;
	private String username;
	
	
	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public User(int id, String username) {
		super();
		this.id = id;
		this.username = username;
	}
	public User() {
		super();
	}
	
	
}
