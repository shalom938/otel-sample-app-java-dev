/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.domain.OwnerValidation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controllerclass OwnerController implements InitializingBean {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private OwnerValidation validator;

	@Autowired
	private OpenTelemetry openTelemetry;

	private Tracer otelTracer;
	@Autowired
	private OwnerRepository ownerRepository;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.otelTracer = openTelemetry.getTracer("OwnerController");

		validator = new OwnerValidation(this.otelTracer);
	}

	private final OwnerRepository owners;
	private final JdbcTemplate jdbcTemplate;

	public OwnerController(OwnerRepository clinicService, JdbcTemplate jdbcTemplate

	) {
		this.owners = clinicService;
		this.jdbcTemplate = jdbcTemplate;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}@ModelAttribute("owner")
	public Owner findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		try {
			if (ownerId == null) {
				return new Owner();
			}
			Owner owner = this.owners.findById(ownerId);
			if (owner == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found");
			}
			return owner;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error finding owner");
		}
	}

	@GetMapping("/owners/new")
	public String initCreationForm(Map<String, Object> model) {
		try {
			Owner owner = new Owner();
			if (owner != null) {
				validator.ValidateOwnerWithExternalService(owner);
				model.put("owner", owner);
				validator.ValidateUserAccess("admin", "pwd", "fullaccess");
			}
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error initializing form");
		}
	}

	@PostMapping("/owners/new")
	public String processCreationForm(@Valid Owner owner, BindingResult result) {
		try {
			if (owner == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot be null");
			}
			if (result.hasErrors()) {
				return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
			}
			validator.ValidateOwnerWithExternalService(owner);
			validator.PerformValidationFlow(owner);
			validator.checkOwnerValidity(owner);
			this.owners.save(owner);
			validator.ValidateUserAccess("admin", "pwd", "fullaccess");
			return "redirect:/owners/" + owner.getId();
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing form");
		}
	}@GetMapping("/owners/find")
	public String initFindForm() {
		return "owners/findOwners";
	}

	@GetMapping("/owners")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Owner owner, BindingResult result,
			Model model) {
		// Validate page parameter
		if (page < 1) {
			page = 1;
		}

		// Validate owner parameter
		if (owner == null) {
			return "owners/findOwners";
		}

		// Validate user access with error handling
		try {
			validator.ValidateUserAccess("admin", "pwd", "fullaccess");
		} catch (Exception e) {
			result.reject("accessDenied", "Access validation failed");
			return "owners/findOwners";
		}

		// allow parameterless GET request for /owners to return all records
		if (owner.getLastName() == null) {
			owner.setLastName(""); // empty string signifies broadest possible search
		}

		// find owners by last name
		Page<Owner> ownersResults = findPaginatedForOwnersLastName(page, owner.getLastName());
		if (ownersResults == null || ownersResults.isEmpty()) {
			// no owners found
			result.rejectValue("lastName", "notFound", "not found");
			return "owners/findOwners";
		}

		if (ownersResults.getTotalElements() == 1) {
			// 1 owner found
			owner = ownersResults.iterator().next();
			return "redirect:/owners/" + owner.getId();
		}

		return "owners/ownersList";
	}// multiple owners found
		return addPaginationModel(page, model, ownersResults);
	}

	@WithSpan
	private String addPaginationModel(int page, Model model, Page<Owner> paginated) {
		// throw new RuntimeException();
		model.addAttribute("listOwners", paginated);
		List<Owner> listOwners = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listOwners", listOwners);
		return "owners/ownersList";
	}

	@WithSpan()
	private Page<Owner> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return owners.findByLastName(lastname, pageable);
	}@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, Model model) {
		Owner owner = this.owners.findById(ownerId);
		if (owner == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found");
		}
		try {
			var petCount = ownerRepository.countPets(owner.getId());
			if (petCount > 0) {
				var totalVists = owner.getPets().stream()
					.filter(pet -> pet != null && pet.getVisits() != null)
					.mapToLong(pet -> pet.getVisits().size())
					.sum();
				var averageVisits = petCount > 0 ? totalVists/petCount : 0;
			}
			model.addAttribute(owner);
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing owner data");
		}
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	private static void delay(long millis) {
		try {
			Thread.sleep(millis);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result,
			@PathVariable("ownerId") int ownerId) {
		if (result.hasErrors()) {
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		
		try {
			owner.setId(ownerId);
			if (!validator.checkOwnerValidity(owner)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid owner data");
			}
			if (!validator.ValidateOwnerWithExternalService(owner)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner validation failed");
			}
		} catch (Exception e) {
			if (e instanceof ResponseStatusException) {
				throw e;
			}
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing owner update");
		}
		return "redirect:/owners/{ownerId}";
	}validator.PerformValidationFlow(owner);
		this.owners.save(owner);
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		try {
			validator.ValidateUserAccess("admin", "pwd", "fullaccess");
			Owner owner = this.owners.findById(ownerId);
			
			if (owner == null) {
				ModelAndView notFoundMav = new ModelAndView("errors/404");
				notFoundMav.setStatus(HttpStatus.NOT_FOUND);
				return notFoundMav;
			}

			validator.ValidateOwnerWithExternalService(owner);
			
			ModelAndView mav = new ModelAndView("owners/ownerDetails");
			mav.addObject(owner);
			return mav;
		} catch (Exception e) {
			ModelAndView errorMav = new ModelAndView("errors/error");
			errorMav.addObject("message", "Error processing request: " + e.getMessage());
			errorMav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			return errorMav;
		}
	}

	@GetMapping("/owners/{ownerId}/pets")
	@ResponseBody
	public String getOwnerPetsMap(@PathVariable("ownerId") int ownerId) {
		String sql = "SELECT p.id AS pet_id, p.owner_id AS owner_id FROM pets p JOIN owners o ON p.owner_id = o.id";Map<Integer, List<Integer>> ownerToPetsMap = rows.stream()
			.collect(Collectors.toMap(
				row -> Optional.ofNullable(row.get("owner_id"))
					.map(id -> (Integer) id)
					.orElseThrow(() -> new IllegalArgumentException("Owner ID cannot be null")),
				row -> Optional.ofNullable(row.get("pet_id"))
					.map(id -> List.of((Integer) id))
					.orElse(Collections.emptyList()),
				(existing, incoming) -> Stream.concat(
					existing.stream(), 
					incoming.stream())
					.collect(Collectors.toList())
			));

		if (ownerId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner ID cannot be null");
		}

		List<Integer> pets = ownerToPetsMap.getOrDefault(ownerId, Collections.emptyList());

		if (pets.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pets found for owner " + ownerId);
		}

		return "Pets for owner " + ownerId + ": " + pets.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(", "));

	}