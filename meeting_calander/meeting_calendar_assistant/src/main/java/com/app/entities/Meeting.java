package com.app.entities;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Meeting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime startTime;
	private LocalDateTime endTime;

	@ManyToOne
	@JoinColumn(name = "owner_id")
	private Employee owner;

	@ManyToMany
	@JoinTable(name = "meeting_participants", joinColumns = @JoinColumn(name = "meeting_id"), inverseJoinColumns = @JoinColumn(name = "participant_id"))
	private List<Employee> participants;

	// Constructors, Getters, Setters
	public Meeting() {
	}

	public Meeting(LocalDateTime startTime, LocalDateTime endTime, Employee ownerName, List<Employee> participants) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.owner = ownerName; // Correct reference to Employee
		this.participants = participants;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public Employee getOwner() {
		return owner;
	}

	public void setOwner(Employee owner) {
		this.owner = owner;
	}

	public List<Employee> getParticipants() {
		return participants;
	}

	public void setParticipants(List<Employee> participants) {
		this.participants = participants;
	}

	@Override
	public String toString() {
		return "Meeting [id=" + id + ", startTime=" + startTime + ", endTime=" + endTime + ", owner=" + owner
				+ ", participants=" + participants + "]";
	}
}
