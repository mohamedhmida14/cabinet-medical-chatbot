package com.essai.cabinet.config;

import com.essai.cabinet.model.*;
import com.essai.cabinet.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ConsultationRepository consultationRepository;
    private final ChatbotQuestionRepository chatbotQuestionRepository;

    public DataInitializer(UserRepository userRepository, PatientRepository patientRepository, DoctorRepository doctorRepository,
                           AppointmentRepository appointmentRepository, ConsultationRepository consultationRepository,
                           ChatbotQuestionRepository chatbotQuestionRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.consultationRepository = consultationRepository;
        this.chatbotQuestionRepository = chatbotQuestionRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        userRepository.save(new User("Administrateur", "admin@cabinet.tn", "admin", Role.ADMIN));

        Doctor d1 = doctorRepository.save(new Doctor(new User("Dr Ahmed Mansouri", "doctor@cabinet.tn", "doctor", Role.DOCTOR), "Médecine générale", "22111222", "Lundi - Vendredi, 08:00 - 14:00"));
        Doctor d2 = doctorRepository.save(new Doctor(new User("Dr Mariem Ben Ali", "cardio@cabinet.tn", "doctor", Role.DOCTOR), "Cardiologie", "22333444", "Mardi - Jeudi, 09:00 - 15:00"));

        Patient p1 = patientRepository.save(new Patient(new User("Mohamed Patient", "patient@cabinet.tn", "patient", Role.PATIENT), "55111222", LocalDate.of(2000, 3, 15), "Tunis"));
        Patient p2 = patientRepository.save(new Patient(new User("Sarra Trabelsi", "sarra@cabinet.tn", "patient", Role.PATIENT), "55999888", LocalDate.of(1998, 9, 22), "Ariana"));

        Appointment a1 = appointmentRepository.save(new Appointment(p1, d1, LocalDateTime.now().plusDays(1).withHour(10).withMinute(0), "Douleur abdominale", AppointmentStatus.CONFIRMED));
        appointmentRepository.save(new Appointment(p2, d2, LocalDateTime.now().plusDays(2).withHour(11).withMinute(30), "Contrôle cardiaque", AppointmentStatus.PENDING));
        consultationRepository.save(new Consultation(a1, "Trouble digestif léger", "Repos + traitement symptomatique", "Patient en bon état général."));

        chatbotQuestionRepository.save(new ChatbotQuestion("comment prendre un rendez vous", "Pour prendre un rendez-vous, connectez-vous comme patient, choisissez un médecin, une date et indiquez le motif."));
        chatbotQuestionRepository.save(new ChatbotQuestion("quels sont les horaires du cabinet", "Le cabinet est ouvert du lundi au vendredi de 8h à 17h."));
        chatbotQuestionRepository.save(new ChatbotQuestion("comment annuler un rendez vous", "Vous pouvez annuler un rendez-vous depuis votre tableau de bord patient ou contacter l'administration."));
        chatbotQuestionRepository.save(new ChatbotQuestion("quels medecins sont disponibles", "Les médecins disponibles sont affichés dans la page Médecins avec leurs spécialités et horaires."));
        chatbotQuestionRepository.save(new ChatbotQuestion("comment consulter mon dossier medical", "Après connexion, ouvrez la page Consultations pour voir votre historique médical."));
    }
}
