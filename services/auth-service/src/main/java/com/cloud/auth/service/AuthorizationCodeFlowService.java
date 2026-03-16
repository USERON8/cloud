package com.cloud.auth.service;

import cn.hutool.core.util.StrUtil;
import com.cloud.common.domain.dto.auth.AuthorizationRequestDTO;
import com.cloud.common.domain.dto.auth.AuthorizationSessionResponseDTO;
import com.cloud.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AuthorizationCodeFlowService {

  private final RegisteredClientRepository registeredClientRepository;
  private final AuthorizationServerSettings authorizationServerSettings;

  private final HttpSessionSecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  public AuthorizationSessionResponseDTO createAuthorizationSession(
      AuthorizationRequestDTO authorizationRequest,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    return createAuthorizationSession(
        authorizationRequest.getClientId(),
        authorizationRequest.getRedirectUri(),
        authorizationRequest.getScope(),
        authorizationRequest.getState(),
        authorizationRequest.getCodeChallenge(),
        authorizationRequest.getCodeChallengeMethod(),
        authorizationRequest.getNonce(),
        authentication,
        request,
        response);
  }

  private AuthorizationSessionResponseDTO createAuthorizationSession(
      String clientId,
      String redirectUri,
      String scope,
      String state,
      String codeChallenge,
      String codeChallengeMethod,
      String nonce,
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    RegisteredClient registeredClient =
        validateAuthorizationRequest(
            clientId, redirectUri, state, scope, codeChallenge, codeChallengeMethod);

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    request.getSession(true);
    securityContextRepository.saveContext(context, request, response);

    Set<String> requestedScopes = parseScopes(scope);
    String scopeValue = String.join(" ", requestedScopes);
    String normalizedCodeChallengeMethod = resolveCodeChallengeMethod(codeChallengeMethod);

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(resolveAuthorizationEndpoint(request))
            .queryParam("response_type", "code")
            .queryParam("client_id", registeredClient.getClientId())
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", scopeValue)
            .queryParam("state", state)
            .queryParam("code_challenge", codeChallenge)
            .queryParam("code_challenge_method", normalizedCodeChallengeMethod);

    if (StrUtil.isNotBlank(nonce)) {
      builder.queryParam("nonce", nonce);
    }

    try {
      return new AuthorizationSessionResponseDTO(
          builder.build(true).toUriString(),
          registeredClient.getClientId(),
          redirectUri,
          scopeValue,
          state,
          normalizedCodeChallengeMethod,
          true);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private RegisteredClient validateAuthorizationRequest(
      String clientId,
      String redirectUri,
      String state,
      String scope,
      String codeChallenge,
      String codeChallengeMethod) {
    if (StrUtil.isBlank(clientId)) {
      throw new ValidationException("client_id", clientId, "client_id cannot be blank");
    }
    if (StrUtil.isBlank(redirectUri)) {
      throw new ValidationException("redirect_uri", redirectUri, "redirect_uri cannot be blank");
    }
    if (StrUtil.isBlank(state)) {
      throw new ValidationException("state", state, "state cannot be blank");
    }
    if (StrUtil.isBlank(scope)) {
      throw new ValidationException("scope", scope, "scope cannot be blank");
    }

    RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
    if (registeredClient == null) {
      throw new ValidationException("client_id", clientId, "unknown client_id");
    }
    if (!registeredClient
        .getAuthorizationGrantTypes()
        .contains(AuthorizationGrantType.AUTHORIZATION_CODE)) {
      throw new ValidationException(
          "client_id", clientId, "client does not support authorization_code");
    }
    if (!registeredClient.getRedirectUris().contains(redirectUri)) {
      throw new ValidationException("redirect_uri", redirectUri, "redirect_uri is not registered");
    }

    Set<String> requestedScopes = parseScopes(scope);
    if (requestedScopes.isEmpty()) {
      throw new ValidationException("scope", scope, "scope cannot be empty");
    }
    if (!registeredClient.getScopes().containsAll(requestedScopes)) {
      throw new ValidationException("scope", scope, "scope is not allowed for this client");
    }

    if (registeredClient.getClientSettings().isRequireProofKey()) {
      if (StrUtil.isBlank(codeChallenge)) {
        throw new ValidationException(
            "code_challenge", codeChallenge, "code_challenge cannot be blank");
      }
      String method = resolveCodeChallengeMethod(codeChallengeMethod);
      if (!"S256".equalsIgnoreCase(method)) {
        throw new ValidationException(
            "code_challenge_method", method, "code_challenge_method must be S256");
      }
    }

    return registeredClient;
  }

  private Set<String> parseScopes(String scope) {
    return Arrays.stream(scope.trim().split("\\s+"))
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private String resolveCodeChallengeMethod(String codeChallengeMethod) {
    return StrUtil.isBlank(codeChallengeMethod) ? "S256" : codeChallengeMethod.trim();
  }

  private String resolveAuthorizationEndpoint(HttpServletRequest request) {
    String endpoint = authorizationServerSettings.getAuthorizationEndpoint();
    if (request != null) {
      return ServletUriComponentsBuilder.fromRequestUri(request)
          .replacePath(endpoint)
          .replaceQuery(null)
          .build()
          .toUriString();
    }
    String issuer = authorizationServerSettings.getIssuer();
    if (StrUtil.isBlank(issuer)) {
      return endpoint;
    }
    return UriComponentsBuilder.fromUriString(issuer).path(endpoint).build().toUriString();
  }
}
