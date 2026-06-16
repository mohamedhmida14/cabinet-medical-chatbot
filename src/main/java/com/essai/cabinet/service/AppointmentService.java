package com.essai.cabinet.service;

import com.essai.cabinet.model.*;
import com.essai.cabinet.repository.AppointmentRepository;
import com.essai.cabinet.repository.DoctorRepository;
import com.essai.cabinet.repository.PatientRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public Appointment createAppointment(Long patientId, Long doctorId, LocalDateTime dateTime, String reason) {
        Patient patient = patientRepository.findById(patientId).orElseThrow();
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow();
        Appointment appointment = new Appointment(patient, doctor, dateTime, reason, AppointmentStatus.PENDING);
        return appointmentRepository.save(appointment);
    }

    public List<Appointment> all() { return appointmentRepository.findAll(); }
    public List<Appointment> byPatient(Long patientId) { return appointmentRepository.findByPatientId(patientId); }
    public List<Appointment> byDoctor(Long doctorId) { return appointmentRepository.findByDoctorId(doctorId); }

    public void updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }
}
