package com.essai.cabinet.controller;

import com.essai.cabinet.model.AppointmentStatus;
import com.essai.cabinet.repository.*;
import com.essai.cabinet.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/patient")
public class PatientController {
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final AppointmentService appointmentService;

    public PatientController(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository,
                             ConsultationRepository consultationRepository, AppointmentService appointmentService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        Long patientId = (Long) session.getAttribute("patientId");
        model.addAttribute("appointments", appointmentRepository.findByPatientId(patientId));
        return "patient-dashboard";
    }

    @GetMapping("/doctors")
    public String doctors(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        model.addAttribute("doctors", doctorRepository.findAll());
        return "patient-doctors";
    }

    @GetMapping("/appointments/new")
    public String newAppointment(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        model.addAttribute("doctors", doctorRepository.findAll());
        return "patient-new-appointment";
    }

    @PostMapping("/appointments")
    public String createAppointment(HttpSession session, @RequestParam Long doctorId,
                                    @RequestParam String dateTime, @RequestParam String reason) {
        Long patientId = (Long) session.getAttribute("patientId");
        appointmentService.createAppointment(patientId, doctorId, LocalDateTime.parse(dateTime), reason);
        return "redirect:/patient/dashboard";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        appointmentService.updateStatus(id, AppointmentStatus.CANCELLED);
        return "redirect:/patient/dashboard";
    }

    @GetMapping("/consultations")
    public String consultations(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        Long patientId = (Long) session.getAttribute("patientId");
        model.addAttribute("consultations", consultationRepository.findByAppointmentPatientId(patientId));
        return "patient-consultations";
    }
}
