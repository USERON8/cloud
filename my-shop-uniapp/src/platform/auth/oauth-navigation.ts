export interface OidcLogoutRequest {
  idToken: string
  clientId: string
  postLogoutRedirectUri: string
}

export function buildOidcLogoutPath(request: OidcLogoutRequest): string {
  const params = new URLSearchParams({
    id_token_hint: request.idToken,
    client_id: request.clientId,
    post_logout_redirect_uri: request.postLogoutRedirectUri
  })
  return `/connect/logout?${params.toString()}`
}
