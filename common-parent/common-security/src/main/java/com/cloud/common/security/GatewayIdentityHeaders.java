package com.cloud.common.security;

public final class GatewayIdentityHeaders {

  public static final String USERNAME = "X-User-Name";
  public static final String USER_ID = "X-User-Id";
  public static final String USER_NICKNAME = "X-User-Nickname";
  public static final String USER_STATUS = "X-User-Status";
  public static final String CLIENT_ID = "X-Client-Id";
  public static final String USER_SCOPES = "X-User-Scopes";
  public static final String USER_ROLES = "X-User-Roles";
  public static final String USER_PERMISSIONS = "X-User-Permissions";
  public static final String USER_AUTHORITIES = "X-User-Authorities";
  public static final String TRACE_ID = "X-Trace-Id";
  public static final String SUBJECT = "X-User-Subject";
  public static final String SIGNATURE = "X-Internal-Identity-Signature";
  public static final String TIMESTAMP = "X-Internal-Identity-Timestamp";

  private GatewayIdentityHeaders() {}
}
