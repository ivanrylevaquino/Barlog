package com.barlog.barlog_back.dashboard;

import com.barlog.barlog_back.auth.AuthResourceNotFoundException;
import com.barlog.barlog_back.auth.User;
import com.barlog.barlog_back.auth.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DashboardService {

    private final UserRepository userRepository;

    public DashboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PractitionerDashboardResponse getPractitionerDashboard(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthResourceNotFoundException("User not found: " + userId));
        return new PractitionerDashboardResponse(
                user.getUserId(), user.getDisplayName(), user.getEmail(), user.getAvatarUrl(),
                user.getRole(), user.getPreferredUnit(), user.getCreatedAt(), user.getLastLoginAt());
    }
}

