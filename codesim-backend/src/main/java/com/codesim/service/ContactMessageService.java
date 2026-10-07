package com.codesim.service;

import com.codesim.entity.ContactMessage;
import com.codesim.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Service
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;

    public ContactMessageService(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @Transactional
    public ContactMessage submit(String name, String email, String message, String type) {
        if (name == null || name.isBlank() || email == null || email.isBlank() || message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email, and message are required");
        }
        ContactMessage cm = new ContactMessage();
        cm.setName(name.trim());
        cm.setEmail(email.trim());
        cm.setMessage(message.trim());
        cm.setMessageType(type != null && !type.isBlank() ? type : "contact");
        return contactMessageRepository.save(cm);
    }

    @Transactional(readOnly = true)
    public List<ContactMessage> listAll() {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void markRead(Long id) {
        ContactMessage cm = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        cm.setIsRead(true);
        contactMessageRepository.save(cm);
    }

    public Map<String, Object> toDto(ContactMessage cm) {
        return Map.of(
                "id", cm.getId(),
                "name", cm.getName(),
                "email", cm.getEmail(),
                "message", cm.getMessage(),
                "messageType", cm.getMessageType(),
                "isRead", cm.getIsRead(),
                "createdAt", cm.getCreatedAt()
        );
    }
}
