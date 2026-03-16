package com.cloud.auth.service;

import com.cloud.common.domain.dto.auth.AuthorizationRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationRequestSessionService {

  private static final String PENDING_AUTHORIZATION_REQUEST_ATTRIBUTE =
      AuthorizationRequestSessionService.class.getName() + ".PENDING_AUTHORIZATION_REQUEST";

  public void store(AuthorizationRequestDTO authorizationRequest, HttpServletRequest request) {
    if (authorizationRequest == null || request == null) {
      return;
    }
    request
        .getSession(true)
        .setAttribute(PENDING_AUTHORIZATION_REQUEST_ATTRIBUTE, authorizationRequest);
  }

  public AuthorizationRequestDTO consume(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    HttpSession session = request.getSession(false);
    if (session == null) {
      return null;
    }
    Object value = session.getAttribute(PENDING_AUTHORIZATION_REQUEST_ATTRIBUTE);
    session.removeAttribute(PENDING_AUTHORIZATION_REQUEST_ATTRIBUTE);
    if (value instanceof AuthorizationRequestDTO authorizationRequest) {
      return authorizationRequest;
    }
    return null;
  }

  public void clear(HttpServletRequest request) {
    if (request == null) {
      return;
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.removeAttribute(PENDING_AUTHORIZATION_REQUEST_ATTRIBUTE);
    }
  }
}
