package org.springframework.samples.petclinic.clinicfeedback;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeedbackService {
	private final FeedbackRepository repository;
	private final Lorem lorem = LoremIpsum.getInstance();

	public FeedbackService(FeedbackRepository repository) {
		this.repository = repository;
	}

	public void submit(ClinicFeedback feedback) {
		repository.save(feedback);
	}

	public void populate(int numberOfDocs){
		List<ClinicFeedback> feedbacks = new ArrayList<>(numberOfDocs);
		for (int i = 0; i < numberOfDocs; i++) {
			var feedback = new ClinicFeedback(
				lorem.getEmail(),
				lorem.getParagraphs(1, 3)
			);
			feedbacks.add(feedback);
		}
		repository.saveAll(feedbacks);
	}

	public long count() {
		return repository.count();
	}

	public List<ClinicFeedback> list(int page, int pageSize) {
		return repository.findAll(Pageable.ofSize(pageSize).withPage(page)).toList();
	}
}
