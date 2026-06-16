package com.essai.cabinet.controller;

import com.essai.cabinet.model.*;
import com.essai.cabinet.repository.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    public ApiController(DoctorRepository doctorRepository, PatientRepository patientRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/doctors")
    public List<Doctor> doctors() { return doctorRepository.findAll(); }

    @GetMapping("/patients")
    public List<Patient> patients() { return patientRepository.findAll(); }

    @GetMapping("/appointments")
    public List<Appointment> appointments() { return appointmentRepository.findAll(); }
}
