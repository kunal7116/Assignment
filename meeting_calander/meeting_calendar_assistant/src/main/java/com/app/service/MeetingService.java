package com.app.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.custom_exceptions.ConflictException;
import com.app.custom_exceptions.NotFoundException;
import com.app.entities.Employee;
import com.app.entities.Meeting;
import com.app.repository.EmployeeRepository;
import com.app.repository.MeetingRepository;

@Service
@Transactional
public class MeetingService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    // Method to book a new meeting for an employee
    public Meeting bookMeeting(Employee owner, LocalDateTime start, LocalDateTime end, List<Employee> participants) {
        // Validate owner existence
        if (!employeeRepository.findById(owner.getId()).isPresent()) {
            throw new NotFoundException("Owner not found");
        }

        // Check for conflict in owner's schedule
        if (hasConflicts(owner, start, end)) {
            throw new ConflictException("Meeting time conflicts with an existing meeting for the owner");
        }

        // Check conflicts for each participant
        for (Employee participant : participants) {
            if (hasConflicts(participant, start, end)) {
                throw new ConflictException("Participant " + participant.getName() + " has a meeting conflict");
            }
        }

        // Create and save new meeting
        Meeting meeting = new Meeting(start, end, owner, participants);
        return meetingRepository.save(meeting);
    }

    // Method to check conflicts for a given employee
    private boolean hasConflicts(Employee employee, LocalDateTime start, LocalDateTime end) {
        List<Meeting> conflictingMeetings = meetingRepository.findAllByOwnerAndStartTimeBetween(employee, start, end);
        return !conflictingMeetings.isEmpty();
    }

    // Method to find free time slots between two employees
    public List<LocalDateTime> findFreeSlots(Employee emp1, Employee emp2, Duration duration, LocalDateTime from, LocalDateTime to) {
        List<Meeting> emp1Meetings = meetingRepository.findAllByOwnerAndStartTimeBetween(emp1, from, to);
        List<Meeting> emp2Meetings = meetingRepository.findAllByOwnerAndStartTimeBetween(emp2, from, to);

        return findCommonFreeSlots(emp1Meetings, emp2Meetings, duration, from, to);
    }

    // Helper method to find common free slots between two sets of meetings
    private List<LocalDateTime> findCommonFreeSlots(List<Meeting> meetings1, List<Meeting> meetings2, Duration duration, LocalDateTime from, LocalDateTime to) {
        List<LocalDateTime> busyTimes = new ArrayList<>();

        // Collect busy times for first employee
        for (Meeting meeting : meetings1) {
            busyTimes.add(meeting.getStartTime());
            busyTimes.add(meeting.getEndTime());
        }

        // Collect busy times for second employee
        for (Meeting meeting : meetings2) {
            busyTimes.add(meeting.getStartTime());
            busyTimes.add(meeting.getEndTime());
        }

        // Sort and merge overlapping intervals
        busyTimes.sort(LocalDateTime::compareTo);
        List<LocalDateTime> mergedBusyTimes = mergeBusyTimes(busyTimes);

        // Find free slots
        return findFreeTimeSlots(mergedBusyTimes, duration, from, to);
    }

    // Helper method to merge overlapping busy times
    private List<LocalDateTime> mergeBusyTimes(List<LocalDateTime> busyTimes) {
        List<LocalDateTime> mergedBusyTimes = new ArrayList<>();
        LocalDateTime start = null;
        LocalDateTime end = null;

        for (LocalDateTime time : busyTimes) {
            if (start == null) {
                start = time;
                end = time;
            } else if (time.isBefore(end) || time.equals(end)) {
                end = time.isAfter(end) ? time : end;
            } else {
                mergedBusyTimes.add(start);
                mergedBusyTimes.add(end);
                start = time;
                end = time;
            }
        }

        if (start != null && end != null) {
            mergedBusyTimes.add(start);
            mergedBusyTimes.add(end);
        }

        return mergedBusyTimes;
    }

    // Helper method to find available time slots between meetings
    private List<LocalDateTime> findFreeTimeSlots(List<LocalDateTime> mergedBusyTimes, Duration duration, LocalDateTime from, LocalDateTime to) {
        List<LocalDateTime> freeSlots = new ArrayList<>();
        LocalDateTime slotStart = from;

        // Free time before first meeting
        if (mergedBusyTimes.isEmpty() || slotStart.isBefore(mergedBusyTimes.get(0))) {
            LocalDateTime firstBusyStart = mergedBusyTimes.isEmpty() ? to : mergedBusyTimes.get(0);
            while (slotStart.plus(duration).isBefore(firstBusyStart)) {
                freeSlots.add(slotStart);
                slotStart = slotStart.plus(duration);
            }
        }

        // Free time between meetings
        for (int i = 0; i < mergedBusyTimes.size() - 2; i += 2) {
            LocalDateTime busyEnd = mergedBusyTimes.get(i + 1);
            LocalDateTime nextBusyStart = mergedBusyTimes.get(i + 2);

            while (busyEnd.plus(duration).isBefore(nextBusyStart)) {
                freeSlots.add(busyEnd);
                busyEnd = busyEnd.plus(duration);
            }
        }

        // Free time after last meeting
        if (!mergedBusyTimes.isEmpty() && mergedBusyTimes.get(mergedBusyTimes.size() - 1).isBefore(to)) {
            LocalDateTime lastBusyEnd = mergedBusyTimes.get(mergedBusyTimes.size() - 1);
            while (lastBusyEnd.plus(duration).isBefore(to)) {
                freeSlots.add(lastBusyEnd);
                lastBusyEnd = lastBusyEnd.plus(duration);
            }
        }

        return freeSlots;
    }

    // Method to find participants with meeting conflicts
    public List<Employee> findParticipantsWithConflicts(Meeting newMeeting) {
        List<Employee> participantsWithConflicts = new ArrayList<>();

        for (Employee participant : newMeeting.getParticipants()) {
            if (hasConflicts(participant, newMeeting.getStartTime(), newMeeting.getEndTime())) {
                participantsWithConflicts.add(participant);
            }
        }

        return participantsWithConflicts;
    }
}
