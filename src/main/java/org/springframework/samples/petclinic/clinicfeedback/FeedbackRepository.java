package org.springframework.samples.petclinic.clinicfeedback;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.samples.petclinic.model.ClinicFeedback;

import java.util.List;

public interface FeedbackRepository extends MongoRepository<ClinicFeedback, String> {
	List<ClinicFeedback> findByUserEmail(String userEmail);
}
