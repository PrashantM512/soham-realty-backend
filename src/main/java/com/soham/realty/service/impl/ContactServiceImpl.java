package com.soham.realty.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soham.realty.dto.request.ContactRequest;
import com.soham.realty.dto.response.ContactResponse;
import com.soham.realty.dto.response.PaginatedResponse;
import com.soham.realty.entity.Contact;
import com.soham.realty.entity.Property;
import com.soham.realty.exception.BadRequestException;
import com.soham.realty.exception.ResourceNotFoundException;
import com.soham.realty.repository.ContactRepository;
import com.soham.realty.repository.PropertyRepository;
import com.soham.realty.service.ContactService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final PropertyRepository propertyRepository;

    @Override
    public PaginatedResponse<ContactResponse> getAllContacts(Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        
        // OPTIMIZED: Use single query with LEFT JOIN to fetch property details
        Page<Contact> contactPage = contactRepository.findAllWithPropertyDetails(pageable);
        
        List<ContactResponse> contactResponses = contactPage.getContent().stream()
                .map(this::mapToContactResponse)
                .collect(Collectors.toList());
        
        return PaginatedResponse.of(
                contactResponses,
                contactPage.getTotalElements(),
                page,
                limit
        );
    }

    @Override
    public ContactResponse createContact(ContactRequest request) {
        Contact contact = new Contact();
        contact.setName(request.getName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setMessage(request.getMessage());
        contact.setStatus("New");
        
        // OPTIMIZED: Handle property reference safely
        if (request.getPropertyId() != null) {
            try {
                Property property = propertyRepository.findById(request.getPropertyId())
                        .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + request.getPropertyId()));
                contact.setProperty(property);
                log.debug("Contact created for property: {}", property.getTitle());
            } catch (ResourceNotFoundException e) {
                log.warn("Property with id {} not found, creating general enquiry contact", request.getPropertyId());
                // Property doesn't exist, create as general enquiry (property = null)
                contact.setProperty(null);
            }
        }
        
        Contact savedContact = contactRepository.save(contact);
        return mapToContactResponse(savedContact);
    }

    @Override
    public void deleteContact(Long id) {
        if (!contactRepository.existsById(id)) {
            throw new ResourceNotFoundException("Contact not found with id: " + id);
        }
        contactRepository.deleteById(id);
        log.debug("Deleted contact with id: {}", id);
    }

    @Override
    public ContactResponse updateContactStatus(Long id, String status) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));
        
        // Validate status
        if (!List.of("New", "Contacted", "Resolved").contains(status)) {
            throw new BadRequestException("Invalid status. Must be one of: New, Contacted, Resolved");
        }
        
        contact.setStatus(status);
        Contact updatedContact = contactRepository.save(contact);
        log.debug("Updated contact {} status to: {}", id, status);
        return mapToContactResponse(updatedContact);
    }

    // OPTIMIZED: Handle deleted properties gracefully
    private ContactResponse mapToContactResponse(Contact contact) {
        ContactResponse response = new ContactResponse();
        response.setId(contact.getId());
        response.setName(contact.getName());
        response.setEmail(contact.getEmail());
        response.setPhone(contact.getPhone());
        response.setMessage(contact.getMessage());
        response.setStatus(contact.getStatus());
        response.setCreatedAt(contact.getCreatedAt());
        
        // FIXED: Handle case where property might be deleted
        if (contact.getProperty() != null) {
            try {
                Property property = contact.getProperty();
                // Check if property still exists in database (in case of lazy loading issues)
                if (property.getId() != null) {
                    response.setPropertyId(property.getId());
                    response.setPropertyTitle(property.getTitle());
                } else {
                    // Property was deleted, show as general enquiry
                    response.setPropertyId(null);
                    response.setPropertyTitle("General Enquiry (Property Deleted)");
                }
            } catch (Exception e) {
                // Handle any lazy loading or database issues
                log.warn("Could not load property for contact {}: {}", contact.getId(), e.getMessage());
                response.setPropertyId(null);
                response.setPropertyTitle("General Enquiry (Property Unavailable)");
            }
        } else {
            // General enquiry (no property associated)
            response.setPropertyId(null);
            response.setPropertyTitle("General Enquiry");
        }
        
        return response;
    }
    
    // OPTIMIZED: Add method to clean up orphaned contacts (optional scheduled task)
    @Transactional
    public int cleanupOrphanedContacts() {
        // This method can be called periodically to clean up contacts 
        // that reference non-existent properties
        List<Contact> allContacts = contactRepository.findAll();
        int cleanedUp = 0;
        
        for (Contact contact : allContacts) {
            if (contact.getProperty() != null && 
                !propertyRepository.existsById(contact.getProperty().getId())) {
                // Property was deleted, set property to null (convert to general enquiry)
                contact.setProperty(null);
                contactRepository.save(contact);
                cleanedUp++;
            }
        }
        
        if (cleanedUp > 0) {
            log.info("Cleaned up {} orphaned contacts", cleanedUp);
        }
        
        return cleanedUp;
    }
}