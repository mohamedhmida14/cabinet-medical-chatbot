package com.essai.cabinet.controller;

import com.essai.cabinet.model.*;
import com.essai.cabinet.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;

    public AdminController(PatientRepository patientRepository, DoctorRepository doctorRepository,
                           AppointmentRepository appointmentRepository, ConsultationRepository consultationRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        model.addAttribute("patientsCount", patientRepository.count());
        model.addAttribute("doctorsCount", doctorRepository.count());
        model.addAttribute("appointmentsCount", appointmentRepository.count());
        model.addAttribute("consultationsCount", consultationRepository.count());
        return "admin-dashboard";
    }

    @GetMapping("/doctors")
    public String doctors(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        model.addAttribute("doctors", doctorRepository.findAll());
        return "admin-doctors";
    }

    @PostMapping("/doctors")
    public String addDoctor(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                            @RequestParam String specialty, @RequestParam String phone, @RequestParam String availability) {
        doctorRepository.save(new Doctor(new User(name, email, password, Role.DOCTOR), specialty, phone, availability));
        return "redirect:/admin/doctors";
    }

    @GetMapping("/patients")
    public String patients(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        model.addAttribute("patients", patientRepository.findAll());
        return "admin-patients";
    }

    @PostMapping("/patients")
    public String addPatient(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                             @RequestParam String phone, @RequestParam String dateOfBirth, @RequestParam String address) {
        patientRepository.save(new Patient(new User(name, email, password, Role.PATIENT), phone, LocalDate.parse(dateOfBirth), address));
        return "redirect:/admin/patients";
    }

    @GetMapping("/appointments")
    public String appointments(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        model.addAttribute("appointments", appointmentRepository.findAll());
        model.addAttribute("statuses", AppointmentStatus.values());
        return "admin-appointments";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateAppointmentStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
        return "redirect:/admin/appointments";
    }
}
