package org.springframework.samples.petclinic.clinicfeedback;

import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FeedbackService {
	private final FeedbackRepository repository;

	public FeedbackService(FeedbackRepository repository) {
		this.repository = repository;
	}

	public void submit(ClinicFeedback feedback) {
		repository.save(feedback);
	}

	public List<ClinicFeedback> list() {
		return repository.findAll();
	}
}
