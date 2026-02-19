package com.oms.notificationservice.api;

import com.oms.notificationservice.domain.Notification;
import com.oms.notificationservice.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> listRecent() {
        return ResponseEntity.ok(notificationRepository.findAll());
    }
}
