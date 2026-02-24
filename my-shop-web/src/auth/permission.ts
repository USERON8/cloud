import { computed } from 'vue'
import { sessionState } from './session'

export type UserRole = 'USER' | 'MERCHANT' | 'ADMIN'

const DEFAULT_ROLE: UserRole = 'USER'

function normalizeRole(role?: string): UserRole {
  if (role === 'ADMIN' || role === 'MERCHANT' || role === 'USER') {
    return role
  }
  return DEFAULT_ROLE
}

export function getCurrentRole(): UserRole {
  return normalizeRole(sessionState.user?.userType)
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
