package com.soham.realty.service;

import com.soham.realty.dto.request.ContactRequest;
import com.soham.realty.dto.response.ContactResponse;
import com.soham.realty.dto.response.PaginatedResponse;

public interface ContactService {
    PaginatedResponse<ContactResponse> getAllContacts(Integer page, Integer limit);
    ContactResponse createContact(ContactRequest request);
    void deleteContact(Long id);
    ContactResponse updateContactStatus(Long id, String status);
}
