package com.essai.cabinet.controller;

import com.essai.cabinet.model.*;
import com.essai.cabinet.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/doctor")
public class DoctorController {
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;

    public DoctorController(AppointmentRepository appointmentRepository, ConsultationRepository consultationRepository) {
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        Long doctorId = (Long) session.getAttribute("doctorId");
        model.addAttribute("appointments", appointmentRepository.findByDoctorId(doctorId));
        return "doctor-dashboard";
    }

    @GetMapping("/consultation/new/{appointmentId}")
    public String newConsultation(@PathVariable Long appointmentId, HttpSession session, Model model) {
        if (!AuthController.isLogged(session)) return "redirect:/login";
        model.addAttribute("appointment", appointmentRepository.findById(appointmentId).orElseThrow());
        return "doctor-new-consultation";
    }

    @PostMapping("/consultation")
    public String createConsultation(@RequestParam Long appointmentId, @RequestParam String diagnosis,
                                     @RequestParam String treatment, @RequestParam String notes) {
        Appointment appointment = appointmentRepository.findById(appointmentId).orElseThrow();
        consultationRepository.save(new Consultation(appointment, diagnosis, treatment, notes));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        return "redirect:/doctor/dashboard";
    }
}
