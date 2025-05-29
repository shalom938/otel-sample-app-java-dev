package org.springframework.samples.petclinic.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ClinicFeedback {

	private String id;
	private String userEmail;
	private String comment;
	private Instant submittedAt;

	public ClinicFeedback() {
	}

	public ClinicFeedback(String userEmail, String comment) {
		this.id = UUID.randomUUID().toString();
		this.userEmail = userEmail;
		this.comment = comment;
		this.submittedAt = Instant.now();
	}
}
