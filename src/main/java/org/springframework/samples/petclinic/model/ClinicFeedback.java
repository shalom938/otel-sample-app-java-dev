package org.springframework.samples.petclinic.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Document("feedbacks")
public class ClinicFeedback {
	@Id
	private String id;
	private String userEmail;
	private String comment;
	private LocalDate submittedAt;

	public ClinicFeedback(String userEmail, String comment) {
		this.id = UUID.randomUUID().toString();
		this.userEmail = userEmail;
		this.comment = comment;
		this.submittedAt = LocalDate.now();
	}
}
