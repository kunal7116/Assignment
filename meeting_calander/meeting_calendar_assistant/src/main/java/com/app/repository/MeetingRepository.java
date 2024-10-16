package com.app.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.entities.Employee;
import com.app.entities.Meeting;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
	 List<Meeting> findAllByOwnerAndStartTimeBetween(Employee owner, LocalDateTime start, LocalDateTime end);
}
	