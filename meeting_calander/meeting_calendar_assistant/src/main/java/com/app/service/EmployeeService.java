package com.app.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.custom_exceptions.NotFoundException;
import com.app.entities.Employee;
import com.app.repository.EmployeeRepository;

@Service
@Transactional
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // Create a new employee
    public Employee createEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    // Retrieve all employees
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    // Find an employee by ID
    public Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with id: " + id));
    }
    
    public List<Employee> findByNames(List<String> names) {
        return employeeRepository.findByNameIn(names);
    }
    
    
    // Find an employee by name
    public Employee findByName(String name) {
        return employeeRepository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Employee not found with name: " + name));
    }

    // Update an existing employee
    public Employee updateEmployee(Long id, Employee employeeDetails) {
        Employee employee = findEmployeeById(id);
        employee.setName(employeeDetails.getName());
//        employee.setEmail(employeeDetails.getEmail());
        // Set other fields as needed
        return employeeRepository.save(employee);
    }

    // Delete an employee by ID
    public void deleteEmployee(Long id) {
        Employee employee = findEmployeeById(id);
        employeeRepository.delete(employee);
    }
}
