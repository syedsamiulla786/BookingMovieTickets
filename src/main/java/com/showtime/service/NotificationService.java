package com.showtime.service;

import com.showtime.model.Notification;
import com.showtime.model.User;
import com.showtime.repository.NotificationRepository;
import com.showtime.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(30L * 60 * 1000); // 30 minutes timeout
        
        emitters.put(userId, emitter);
        
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));
        
        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Notification stream connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        
        // Schedule heartbeat
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (emitters.containsKey(userId)) {
                    emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
                }
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }, 0, 25, TimeUnit.SECONDS);
        
        return emitter;
    }
    
    @Async
    public void sendNotification(Long userId, String title, String message, String type) {
        // Save to database
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        
        // Send via SSE if user is connected
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                Map<String, Object> notificationData = Map.of(
                    "id", notification.getId(),
                    "title", title,
                    "message", message,
                    "type", type,
                    "timestamp", LocalDateTime.now().toString(),
                    "read", false
                );
                
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notificationData));
                
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
    
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
    
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }
}