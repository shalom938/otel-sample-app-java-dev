package org.springframework.samples.petclinic.clinicfeedback;

import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinic-feedback")
public class FeedbackController {

	private final FeedbackService service;

	public FeedbackController(FeedbackService service) {
		this.service = service;
	}

	@GetMapping()
	public List<ClinicFeedback> list(
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "pageSize", defaultValue = "10") int pageSize
	) {
		var results = service.list(page, pageSize);
		return results;
	}

	@GetMapping("count")
	public String count() {
		return String.valueOf(service.count());
	}

	@PostMapping("clear")
	public String clear() {
		var deleted = service.clear();
		return deleted + " feedbacks deleted";
	}

	@PostMapping("populate")
	public String populateFeedbacks(@RequestParam(name = "count", defaultValue = "10000") int count) {
		service.populate(count);
		return "done";
	}
}
