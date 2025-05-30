package org.springframework.samples.petclinic.clinicactivity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/clinic-activity")
public class ClinicActivityUiController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ClinicActivityUiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/query-logs")
    public String showQueryLogsPage() {
        return "clinicactivity/query-logs";
    }
} 