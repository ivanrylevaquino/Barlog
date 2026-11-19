package com.barlog.barlog_back.dashboard;

import com.barlog.barlog_back.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** GET /api/dashboard/practitioner — requires ROLE_PRACTITIONER. */
    @GetMapping("/practitioner")
    @PreAuthorize("hasRole('PRACTITIONER')")
    public ResponseEntity<PractitionerDashboardResponse> getPractitionerDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getPractitionerDashboard(principal.getUserId()));
    }
}
