package com.app.controller;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.custom_exceptions.ConflictException;
import com.app.custom_exceptions.NotFoundException;
import com.app.dto.MeetingRequest;
import com.app.entities.Employee;
import com.app.entities.Meeting;
import com.app.repository.EmployeeRepository;
import com.app.service.EmployeeService;
import com.app.service.MeetingService;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

	@Autowired
	private MeetingService meetingService;

	@Autowired
	private EmployeeService employeeService;
	
	@Autowired
    private EmployeeRepository employeeRepository;

	// Booking a new meeting
	@PostMapping("/book")
	public ResponseEntity<?> bookMeeting(@RequestBody MeetingRequest request) {
		try {
			Employee owner = employeeService.findByName(request.getOwnerName());
			List<Employee> participants = employeeService.findByNames(request.getParticipants());
			Meeting meeting = meetingService.bookMeeting(owner, request.getStartTime(), request.getEndTime(),
					participants);
			return ResponseEntity.ok(meeting);
		} catch (ConflictException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
		}
	}

	// Finding free slots between two employees
	@GetMapping("/free-slots")
	public ResponseEntity<List<LocalDateTime>> findFreeSlotsForDate(@RequestParam String emp1,
			@RequestParam String emp2, @RequestParam long durationMinutes, @RequestParam String date) {

		// Parse the 'date' string into LocalDate
		LocalDate parsedDate;
		try {
			parsedDate = LocalDate.parse(date); // Expecting the date in "yyyy-MM-dd" format
		} catch (DateTimeParseException e) {
			return ResponseEntity.badRequest().body(null); // Return bad request if the input is not parsable
		}

		// Define the start and end of the day based on the provided date
		LocalDateTime startOfDay = parsedDate.atStartOfDay(); // 00:00 of that day
		LocalDateTime endOfDay = parsedDate.atTime(LocalTime.MAX); // 23:59 of that day

		Employee employee1 = employeeService.findByName(emp1);
		Employee employee2 = employeeService.findByName(emp2);
		Duration duration = Duration.ofMinutes(durationMinutes);

		List<LocalDateTime> freeSlots = meetingService.findFreeSlots(employee1, employee2, duration, startOfDay,
				endOfDay);
		return ResponseEntity.ok(freeSlots);
	}

	@PostMapping("/check-conflicts")
    public ResponseEntity<?> checkMeetingConflicts(@RequestBody MeetingRequest request) {
        try {
            // Fetch the owner based on the provided name
            Employee owner = employeeRepository.findByName(request.getOwnerName())
                .orElseThrow();

            // Fetch participants based on the provided names
            List<Employee> participants = new ArrayList<>();
            for (String participantName : request.getParticipants()) {
                Employee participant = employeeRepository.findByName(participantName)
                    .orElse(null); // Can be null if not found
                if (participant != null) {
                    participants.add(participant);
                }
            }

            // Create a new Meeting object
            Meeting newMeeting = new Meeting(request.getStartTime(), request.getEndTime(), owner, participants);

            // Find participants with conflicts
            List<Employee> conflictingParticipants = meetingService.findParticipantsWithConflicts(newMeeting);
            if (conflictingParticipants.isEmpty()) {
                return ResponseEntity.ok("No conflicts found.");
            } else {
                return ResponseEntity.ok(conflictingParticipants);
            }
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error checking conflicts: " + e.getMessage());
        }
    }
}
