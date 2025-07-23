package com.soham.realty.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.soham.realty.dto.request.ContactRequest;
import com.soham.realty.dto.response.ApiResponse;
import com.soham.realty.dto.response.ContactResponse;
import com.soham.realty.dto.response.PaginatedResponse;
import com.soham.realty.service.ContactService;

import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "${app.cors.allowed-origins}")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    public ResponseEntity<PaginatedResponse<ContactResponse>> getAllContacts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit) {
        PaginatedResponse<ContactResponse> response = contactService.getAllContacts(page, limit);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> createContact(@Valid @RequestBody ContactRequest request) {
        ContactResponse contact = contactService.createContact(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(contact, "Contact message sent successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Contact deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContactStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String status = statusUpdate.get("status");
        ContactResponse contact = contactService.updateContactStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(contact, "Contact status updated successfully"));
    }
}