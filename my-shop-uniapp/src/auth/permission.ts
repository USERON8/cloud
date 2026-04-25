import { computed } from 'vue'
import { sessionState } from './session'

export type UserRole = 'USER' | 'MERCHANT' | 'ADMIN'

const DEFAULT_ROLE: UserRole = 'USER'

function matchesRole(rawRole: string, targetRole: UserRole): boolean {
  const normalized = rawRole.trim().toUpperCase()
  return normalized === targetRole || normalized === `ROLE_${targetRole}`
}

export function getCurrentRole(): UserRole {
  const roles = Array.isArray(sessionState.user?.roles) ? sessionState.user.roles : []
  if (roles.some((role) => matchesRole(role, 'ADMIN'))) {
    return 'ADMIN'
  }
  if (roles.some((role) => matchesRole(role, 'MERCHANT'))) {
    return 'MERCHANT'
  }
  if (roles.some((role) => matchesRole(role, 'USER'))) {
    return 'USER'
  }
  return DEFAULT_ROLE
}

export function hasAnyRole(...roles: UserRole[]): boolean {
  return roles.includes(getCurrentRole())
}

export function useRole() {
  const role = computed<UserRole>(() => getCurrentRole())
  const isUser = computed(() => role.value === 'USER')
  const isMerchant = computed(() => role.value === 'MERCHANT')
  const isAdmin = computed(() => role.value === 'ADMIN')

  return {
    role,
    isUser,
    isMerchant,
    isAdmin,
    hasAnyRole
  }
}
