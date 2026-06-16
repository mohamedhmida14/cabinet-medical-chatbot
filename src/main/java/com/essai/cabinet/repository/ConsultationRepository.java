package com.essai.cabinet.repository;

import com.essai.cabinet.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByAppointmentPatientId(Long patientId);
    boolean existsByAppointmentId(Long appointmentId);
}
