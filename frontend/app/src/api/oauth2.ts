import { oauth2Client } from './request';
import type { AxiosPromise } from 'axios';

// OAuth2授权请求参数类型
export interface OAuth2AuthRequest {
  response_type: string;
  client_id: string;
  redirect_uri: string;
  scope?: string;
  state?: string;
}

// OAuth2令牌请求参数类型
export interface OAuth2TokenRequest {
  grant_type: string;
  code: string;
  redirect_uri: string;
  client_id: string;
  client_secret: string;
}

// OAuth2令牌响应类型
export interface OAuth2TokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token: string;
  scope: string;
}

/**
 * 发起OAuth2授权请求
 * @param data 授权请求参数
 */
export const oauth2Authorize = (data: OAuth2AuthRequest): void => {
  const params = new URLSearchParams();
  params.append('response_type', data.response_type);
  params.append('client_id', data.client_id);
  params.append('redirect_uri', data.redirect_uri);
  if (data.scope) params.append('scope', data.scope);
  if (data.state) params.append('state', data.state);
  
  window.location.href = `/api/oauth2/authorize?${params.toString()}`;
};

/**
 * 获取OAuth2访问令牌
 * @param data 令牌请求参数
 */
export const oauth2Token = (data: OAuth2TokenRequest): AxiosPromise<OAuth2TokenResponse> => {
  const params = new URLSearchParams();
  params.append('grant_type', data.grant_type);
  params.append('code', data.code);
  params.append('redirect_uri', data.redirect_uri);
  params.append('client_id', data.client_id);
  params.append('client_secret', data.client_secret);
  
  return oauth2Client.post('/oauth2/token', params, {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  });
};

/**
 * 使用OAuth2登录
 * @param clientId 客户端ID
 * @param redirectUri 重定向URI
 * @param scope 请求权限范围
 * @param state 状态参数
 */
export const oauth2Login = (
  clientId: string,
  redirectUri: string,
  scope = 'read write',
  state = ''
): void => {
  const authRequest: OAuth2AuthRequest = {
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    scope,
    state
  };
  
  oauth2Authorize(authRequest);
};