package com.essai.cabinet.service;

import com.essai.cabinet.model.Appointment;
import com.essai.cabinet.model.ChatbotQuestion;
import com.essai.cabinet.model.Doctor;
import com.essai.cabinet.model.Patient;
import com.essai.cabinet.model.Role;
import com.essai.cabinet.model.User;
import com.essai.cabinet.repository.AppointmentRepository;
import com.essai.cabinet.repository.ChatbotQuestionRepository;
import com.essai.cabinet.repository.DoctorRepository;
import com.essai.cabinet.repository.PatientRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    private final ChatbotQuestionRepository repository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${chatbot.python.url:}")
    private String pythonChatbotUrl;

    public ChatbotService(ChatbotQuestionRepository repository,
                          DoctorRepository doctorRepository,
                          PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository) {
        this.repository = repository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public String answer(String userQuestion) {
        return answer(userQuestion, null, null, null);
    }

    public String answer(String userQuestion, User user, Long patientId, Long doctorId) {
        Optional<String> pythonAnswer = askPythonChatbot(userQuestion, user, patientId, doctorId);
        if (pythonAnswer.isPresent()) {
            return pythonAnswer.get();
        }

        String databaseAnswer = answerFromDatabase(userQuestion, user, patientId, doctorId);
        if (databaseAnswer != null) {
            return databaseAnswer;
        }

        String normalized = normalize(userQuestion);
        List<ChatbotQuestion> entries = repository.findAll();

        return entries.stream()
                .max(Comparator.comparingInt(entry -> score(normalized, normalize(entry.getQuestion()))))
                .filter(entry -> score(normalized, normalize(entry.getQuestion())) > 0)
                .map(ChatbotQuestion::getAnswer)
                .orElse("Désolé, je n'ai pas bien compris votre question. Vous pouvez demander : rendez-vous, horaires, médecin disponible, annulation ou consultation.");
    }

    private Optional<String> askPythonChatbot(String userQuestion, User user, Long patientId, Long doctorId) {
        String chatbotUrl = configuredPythonChatbotUrl();
        if (chatbotUrl.isBlank()) {
            logger.warn("Python chatbot URL is not configured. Falling back to Java chatbot.");
            return Optional.empty();
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("question", userQuestion);
            if (user != null) {
                payload.put("userName", user.getName());
                payload.put("role", user.getRole().name());
            }
            payload.put("patientId", patientId);
            payload.put("doctorId", doctorId);

            HttpRequest request = HttpRequest.newBuilder(URI.create(chatbotUrl))
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("Python chatbot returned HTTP {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.hasNonNull("answer") && !root.get("answer").asText().isBlank()) {
                return Optional.of(root.get("answer").asText());
            }
            logger.warn("Python chatbot response did not contain a usable answer: {}", response.body());
        } catch (IOException e) {
            logger.warn("Could not call Python chatbot at {}: {}", chatbotUrl, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Python chatbot call was interrupted.");
            return Optional.empty();
        } catch (RuntimeException e) {
            logger.warn("Python chatbot call failed at {}: {}", chatbotUrl, e.getMessage());
            return Optional.empty();
        }

        return Optional.empty();
    }

    private String configuredPythonChatbotUrl() {
        if (pythonChatbotUrl != null && !pythonChatbotUrl.isBlank()) {
            return pythonChatbotUrl;
        }
        String environmentUrl = System.getenv("CHATBOT_PYTHON_URL");
        if (environmentUrl != null && !environmentUrl.isBlank()) {
            return environmentUrl;
        }
        String renderHostPort = System.getenv("CHATBOT_PYTHON_HOSTPORT");
        if (renderHostPort != null && !renderHostPort.isBlank()) {
            return "http://" + renderHostPort + "/ask";
        }
        return "";
    }

    private String answerFromDatabase(String userQuestion, User user, Long patientId, Long doctorId) {
        String normalized = normalize(userQuestion);

        if (containsAny(normalized, "medecin", "docteur", "doctors", "disponible", "disponibilite", "specialite")) {
            return doctorsAnswer();
        }

        if (containsAny(normalized, "patient", "patients")) {
            return patientsAnswer(user);
        }

        if (containsAny(normalized, "rendez", "rdv", "appointment")) {
            return appointmentsAnswer(user, patientId, doctorId);
        }

        return null;
    }

    private String doctorsAnswer() {
        List<Doctor> doctors = doctorRepository.findAll();
        if (doctors.isEmpty()) {
            return "Aucun médecin n'est enregistré pour le moment.";
        }

        StringBuilder answer = new StringBuilder("Voici les médecins du cabinet :");
        for (Doctor doctor : doctors) {
            answer.append("\n- ")
                    .append(doctor.getUser().getName())
                    .append(" (")
                    .append(doctor.getSpecialty())
                    .append("), disponibilité : ")
                    .append(doctor.getAvailability())
                    .append(", téléphone : ")
                    .append(doctor.getPhone());
        }
        return answer.toString();
    }

    private String patientsAnswer(User user) {
        if (user == null || user.getRole() != Role.ADMIN) {
            return "Je peux afficher la liste des patients uniquement pour un administrateur connecté.";
        }

        List<Patient> patients = patientRepository.findAll();
        if (patients.isEmpty()) {
            return "Aucun patient n'est enregistré pour le moment.";
        }

        StringBuilder answer = new StringBuilder("Patients enregistrés :");
        for (Patient patient : patients) {
            answer.append("\n- ")
                    .append(patient.getUser().getName())
                    .append(", téléphone : ")
                    .append(patient.getPhone())
                    .append(", adresse : ")
                    .append(patient.getAddress());
        }
        return answer.toString();
    }

    private String appointmentsAnswer(User user, Long patientId, Long doctorId) {
        if (user == null) {
            return "Connectez-vous pour consulter vos rendez-vous. Pour prendre un rendez-vous, ouvrez l'espace patient, choisissez un médecin et une date.";
        }

        List<Appointment> appointments;
        String title;
        if (user.getRole() == Role.PATIENT && patientId != null) {
            appointments = appointmentRepository.findByPatientId(patientId);
            title = "Vos rendez-vous :";
        } else if (user.getRole() == Role.DOCTOR && doctorId != null) {
            appointments = appointmentRepository.findByDoctorId(doctorId);
            title = "Vos rendez-vous médecin :";
        } else if (user.getRole() == Role.ADMIN) {
            appointments = appointmentRepository.findAll();
            title = "Rendez-vous enregistrés :";
        } else {
            return "Je n'arrive pas à identifier votre profil pour afficher les rendez-vous.";
        }

        if (appointments.isEmpty()) {
            return "Aucun rendez-vous trouvé.";
        }

        StringBuilder answer = new StringBuilder(title);
        for (Appointment appointment : appointments) {
            answer.append("\n- ")
                    .append(appointment.getDateTime().format(dateTimeFormatter))
                    .append(" avec ")
                    .append(appointment.getDoctor().getUser().getName())
                    .append(" pour ")
                    .append(appointment.getPatient().getUser().getName())
                    .append(" (")
                    .append(appointment.getStatus())
                    .append(") : ")
                    .append(appointment.getReason());
        }
        return answer.toString();
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private int score(String userQuestion, String storedQuestion) {
        int score = 0;
        for (String token : storedQuestion.split(" ")) {
            if (token.length() > 3 && userQuestion.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private String normalize(String text) {
        if (text == null) return "";
        String noAccent = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccent.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
