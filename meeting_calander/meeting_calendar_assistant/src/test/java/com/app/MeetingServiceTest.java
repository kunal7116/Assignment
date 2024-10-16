package com.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.app.custom_exceptions.ConflictException;
import com.app.entities.Employee;
import com.app.entities.Meeting;
import com.app.repository.EmployeeRepository;
import com.app.repository.MeetingRepository;
import com.app.service.MeetingService;

class MeetingServiceTest {

    @InjectMocks
    private MeetingService meetingService;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testBookMeetingSuccess() {
        // Mock the employee (owner)
        Employee owner = new Employee();
        owner.setId(1L);
        owner.setName("John Doe");

        // Mock the repository to find the owner by ID
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(owner));
        
        // No conflicting meetings for the owner
        when(meetingRepository.findAllByOwnerAndStartTimeBetween(any(Employee.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // Create participants
        Employee participant1 = new Employee();
        participant1.setId(2L);
        participant1.setName("Jane Doe");

        Employee participant2 = new Employee();
        participant2.setId(3L);
        participant2.setName("Bob Smith");

        List<Employee> participants = List.of(participant1, participant2);

        // Book a meeting
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(1);

        // No conflicts for participants
        when(meetingRepository.findAllByOwnerAndStartTimeBetween(participant1, start, end)).thenReturn(new ArrayList<>());
        when(meetingRepository.findAllByOwnerAndStartTimeBetween(participant2, start, end)).thenReturn(new ArrayList<>());

        // Test booking the meeting
        Meeting meeting = meetingService.bookMeeting(owner, start, end, participants);

        assertNotNull(meeting);
        assertEquals(start, meeting.getStartTime());
        assertEquals(end, meeting.getEndTime());
        assertEquals(owner, meeting.getOwner());
        assertEquals(participants, meeting.getParticipants());

        // Verify that the meeting is saved
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void testBookMeetingConflict() {
        // Mock the employee (owner)
        Employee owner = new Employee();
        owner.setId(1L);
        owner.setName("John Doe");

        // Mock the repository to find the owner by ID
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(owner));
        
        // Mock a conflict with an existing meeting for the owner
        when(meetingRepository.findAllByOwnerAndStartTimeBetween(any(Employee.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(new Meeting(LocalDateTime.now(), LocalDateTime.now().plusHours(2), owner, new ArrayList<>())));

        // Create participants
        Employee participant1 = new Employee();
        participant1.setId(2L);
        participant1.setName("Jane Doe");

        Employee participant2 = new Employee();
        participant2.setId(3L);
        participant2.setName("Bob Smith");

        List<Employee> participants = List.of(participant1, participant2);

        // Attempt to book a meeting that conflicts
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(1);

        ConflictException exception = assertThrows(ConflictException.class, () -> 
            meetingService.bookMeeting(owner, start, end, participants));
        assertEquals("Meeting time conflicts with an existing meeting for the owner", exception.getMessage());
    }

    @Test
    void testFindFreeSlots() {
        // Mock employees
        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("John Doe");

        Employee emp2 = new Employee();
        emp2.setId(2L);
        emp2.setName("Jane Smith");

        // Mock meetings for emp1 and emp2
        List<Meeting> emp1Meetings = List.of(
                new Meeting(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2), emp1, new ArrayList<>()));
        List<Meeting> emp2Meetings = List.of(
                new Meeting(LocalDateTime.now().plusHours(3), LocalDateTime.now().plusHours(4), emp2, new ArrayList<>()));

        when(meetingRepository.findAllByOwnerAndStartTimeBetween(emp1, any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(emp1Meetings);
        when(meetingRepository.findAllByOwnerAndStartTimeBetween(emp2, any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(emp2Meetings);

        Duration duration = Duration.ofHours(1);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        // Call findFreeSlots to get the available slots
        List<LocalDateTime> freeSlots = meetingService.findFreeSlots(emp1, emp2, duration, start, end);

        assertFalse(freeSlots.isEmpty());
        // Additional assertions can be added based on expected free slots
    }

    @Test
    void testFindParticipantsWithConflicts() {
        // Setup the meeting
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        // Mock the owner and participants
        Employee owner = new Employee();
        owner.setId(1L);
        owner.setName("John Doe");

        Employee participant1 = new Employee();
        participant1.setId(2L);
        participant1.setName("Jane Doe");

        Employee participant2 = new Employee();
        participant2.setId(3L);
        participant2.setName("Bob Smith");

        List<Employee> participants = List.of(participant1, participant2);
        Meeting newMeeting = new Meeting(start, end, owner, participants);

        // Mock participant1 having a conflicting meeting
        when(meetingRepository.findAllByOwnerAndStartTimeBetween(participant1, start, end))
                .thenReturn(List.of(new Meeting(start, end.plusMinutes(30), participant1, new ArrayList<>())));

        // Check for participants with conflicts
        List<Employee> conflictingParticipants = meetingService.findParticipantsWithConflicts(newMeeting);

        assertTrue(conflictingParticipants.contains(participant1));
        assertFalse(conflictingParticipants.contains(participant2));
    }
}
