package com.essai.cabinet.controller;

import com.essai.cabinet.model.Role;
import com.essai.cabinet.model.User;
import com.essai.cabinet.repository.DoctorRepository;
import com.essai.cabinet.repository.PatientRepository;
import com.essai.cabinet.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final AuthService authService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AuthController(AuthService authService, PatientRepository patientRepository, DoctorRepository doctorRepository) {
        this.authService = authService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        return authService.login(email, password).map(user -> {
            session.setAttribute("user", user);
            if (user.getRole() == Role.ADMIN) return "redirect:/admin/dashboard";
            if (user.getRole() == Role.DOCTOR) {
                doctorRepository.findByUserId(user.getId()).ifPresent(doctor -> session.setAttribute("doctorId", doctor.getId()));
                return "redirect:/doctor/dashboard";
            }
            patientRepository.findByUserId(user.getId()).ifPresent(patient -> session.setAttribute("patientId", patient.getId()));
            return "redirect:/patient/dashboard";
        }).orElseGet(() -> {
            model.addAttribute("error", "Email ou mot de passe incorrect");
            return "login";
        });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    static boolean isLogged(HttpSession session) {
        return session.getAttribute("user") instanceof User;
    }
}
