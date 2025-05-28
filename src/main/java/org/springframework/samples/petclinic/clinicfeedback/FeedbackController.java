package org.springframework.samples.petclinic.clinicfeedback;

import org.springframework.samples.petclinic.model.ClinicFeedback;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clinic-feedback")
public class FeedbackController {

	private final FeedbackService service;

	public FeedbackController(FeedbackService service) {
		this.service = service;
	}

	@GetMapping
	public String getFeedback() {
		var results = service.list();
		return String.valueOf(results.size());
	}

	@PostMapping
	public String submitFeedback() {
		service.submit(new ClinicFeedback("achen@dig.ai", "bla bla"));
		return "redirect:/";
	}
}
