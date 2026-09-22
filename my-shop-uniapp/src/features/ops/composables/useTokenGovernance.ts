import { ref } from 'vue'
import { getGitHubAuthStatus, getGitHubUserInfo, logoutAllSessions, validateToken } from '../../../api/auth'
import {
  addTokenToBlacklist,
  checkBlacklist,
  cleanupBlacklist,
  cleanupExpiredTokens,
  getAuthorizationDetails,
  getBlacklistStats,
  getStorageStructure,
  getTokenStats,
  revokeAuthorization
} from '../../../api/auth-tokens'
import type { TokenBlacklistStats, UserInfo } from '../../../types/domain'
import { confirm, toast } from '../../../utils/ui'

export function useTokenGovernance() {
  const tokenStats = ref<Record<string, unknown> | null>(null)
  const storageStructure = ref<Record<string, unknown> | null>(null)
  const authorizationId = ref('')
  const authorizationDetail = ref<Record<string, unknown> | null>(null)
  const cleanupResult = ref<Record<string, unknown> | null>(null)
  const tokenValidationMessage = ref<string | null>(null)
  const blacklistStats = ref<TokenBlacklistStats | null>(null)
  const blacklistToken = ref('')
  const blacklistReason = ref('')
  const blacklistCheckResult = ref<Record<string, unknown> | null>(null)
  const blacklistCleanupResult = ref<Record<string, unknown> | null>(null)
  const logoutUsername = ref('')
  const gitHubStatus = ref<boolean | null>(null)
  const gitHubUserInfo = ref<UserInfo | null>(null)

  async function run(action: () => Promise<void>, fallback: string): Promise<void> {
    try {
      await action()
    } catch (error) {
      toast(error instanceof Error ? error.message : fallback)
    }
  }

  const loadTokenStats = () => run(async () => {
    tokenStats.value = await getTokenStats()
    toast('Token statistics loaded', 'success')
  }, 'Load failed')

  const loadAuthorizationDetail = () => run(async () => {
    const id = authorizationId.value.trim()
    if (!id) {
      toast('Please enter the authorization ID')
      return
    }
    authorizationDetail.value = await getAuthorizationDetails(id)
    toast('Authorization details loaded', 'success')
  }, 'Load failed')

  async function revokeAuthorizationRow(): Promise<void> {
    const id = authorizationId.value.trim()
    if (!id) {
      toast('Please enter the authorization ID')
      return
    }
    if (!(await confirm('Revoke this authorization?'))) return
    await run(async () => {
      await revokeAuthorization(id)
      toast('Authorization revoked', 'success')
    }, 'Revoke failed')
  }

  const runTokenCleanup = () => run(async () => {
    cleanupResult.value = await cleanupExpiredTokens()
    toast('Cleanup triggered', 'success')
  }, 'Cleanup failed')

  const validateCurrentToken = () => run(async () => {
    tokenValidationMessage.value = await validateToken()
    toast('Token validation succeeded', 'success')
  }, 'Validation failed')

  const loadStorageStructure = () => run(async () => {
    storageStructure.value = await getStorageStructure()
    toast('Storage structure loaded', 'success')
  }, 'Load failed')

  const loadBlacklistStats = () => run(async () => {
    blacklistStats.value = await getBlacklistStats()
    toast('Blacklist statistics loaded', 'success')
  }, 'Load failed')

  const addBlacklist = () => run(async () => {
    const token = blacklistToken.value.trim()
    if (!token) {
      toast('Please enter a token')
      return
    }
    await addTokenToBlacklist(token, blacklistReason.value.trim() || undefined)
    toast('Token added to the blacklist', 'success')
  }, 'Add failed')

  const checkBlacklistStatus = () => run(async () => {
    const token = blacklistToken.value.trim()
    if (!token) {
      toast('Please enter a token')
      return
    }
    blacklistCheckResult.value = await checkBlacklist(token)
    toast('Blacklist status loaded', 'success')
  }, 'Query failed')

  const cleanupBlacklistEntries = () => run(async () => {
    blacklistCleanupResult.value = await cleanupBlacklist()
    toast('Blacklist cleanup triggered', 'success')
  }, 'Cleanup failed')

  const loadGitHubStatus = () => run(async () => {
    gitHubStatus.value = await getGitHubAuthStatus()
    toast('GitHub authorization status loaded', 'success')
  }, 'Load failed')

  const loadGitHubUser = () => run(async () => {
    gitHubUserInfo.value = await getGitHubUserInfo()
    toast('GitHub user loaded', 'success')
  }, 'Load failed')

  const logoutAll = () => run(async () => {
    const username = logoutUsername.value.trim()
    if (!username) {
      toast('Please enter a username')
      return
    }
    await logoutAllSessions(username)
    toast('Global logout triggered', 'success')
  }, 'Operation failed')

  return {
    addBlacklist,
    authorizationDetail,
    authorizationId,
    blacklistCheckResult,
    blacklistCleanupResult,
    blacklistReason,
    blacklistStats,
    blacklistToken,
    checkBlacklistStatus,
    cleanupBlacklistEntries,
    cleanupResult,
    gitHubStatus,
    gitHubUserInfo,
    loadAuthorizationDetail,
    loadBlacklistStats,
    loadGitHubStatus,
    loadGitHubUser,
    loadStorageStructure,
    loadTokenStats,
    logoutAll,
    logoutUsername,
    revokeAuthorizationRow,
    runTokenCleanup,
    storageStructure,
    tokenStats,
    tokenValidationMessage,
    validateCurrentToken
  }
}
