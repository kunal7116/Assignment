package com.app.entities;

import javax.persistence.*;
import java.util.List;

@Entity
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@OneToMany(mappedBy = "owner", fetch = FetchType.EAGER)
	private List<Meeting> meetings;

	@ManyToMany(mappedBy = "participants")
	private List<Meeting> participatedMeetings;

	// Constructors, Getters, Setters
	public Employee() {
	}

	public Employee(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Meeting> getMeetings() {
		return meetings;
	}

	public void setMeetings(List<Meeting> meetings) {
		this.meetings = meetings;
	}

	@Override
	public String toString() {
		return "Employee [id=" + id + ", name=" + name + ", meetings=" + meetings + "]";
	}

	// Getters and setters...
}
