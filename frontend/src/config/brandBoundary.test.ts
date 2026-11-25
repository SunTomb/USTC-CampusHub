import { describe, expect, it } from 'vitest'
import index from '../../index.html?raw'
import shell from '../layouts/MainLayout.vue?raw'
import wallet from '../views/WalletView.vue?raw'
import goods from '../views/GoodsDetailView.vue?raw'
import adminWallet from '../views/AdminWalletView.vue?raw'
import home from '../views/HomeView.vue?raw'
import roles from '../views/RoleApplicationsView.vue?raw'
import auth from '../views/AuthView.vue?raw'
import profile from '../views/ProfileView.vue?raw'
import policy from '../views/PolicyView.vue?raw'
import credit from '../views/CreditCenterView.vue?raw'
import navigation from './navigation.ts?raw'
import identity from '../utils/identity.ts?raw'

describe('visible brand and closed-finance regression', () => {
  it('uses the independent brand in the page title and shell', () => {
    expect(index).toContain('蜗壳同路 CampusHub')
    expect(shell).toContain('蜗壳同路 CampusHub')
    expect(shell).not.toMatch(/中国科大|中科大|USTC|瀚海|校集 CampusHub/)
  })
  it('uses dedicated web and mobile bookmark icons instead of the old cached paths', () => {
    expect(shell).toContain('/woke-tonglu-logo-144.png')
    expect(shell).not.toContain('/campushub-logo.png')
    expect(index).toContain('/woke-tonglu-favicon-48.png')
    expect(index).toContain('/woke-tonglu-apple-touch-180.png')
    expect(index).toContain('name="apple-mobile-web-app-title" content="蜗壳同路"')
    expect(index).not.toContain('/favicon.png')
  })
  it('renames system-owned text without changing campus email domains', () => {
    for (const source of [index, shell, home, roles, auth, profile, policy, credit, navigation, identity]) {
      expect(source).not.toMatch(/蜗壳(?!同路)/)
    }
    expect(auth).toContain('@mail.ustc.edu.cn')
    expect(auth).toContain('@ustc.edu.cn')
  })
  it('keeps financial legacy views behind the backend availability flag', () => {
    for (const view of [wallet, goods, adminWallet]) expect(view).toContain('v-if="platform.financialEnabled"')
    expect(home).not.toContain("router.push('/wallet')")
  })
  it('requires explicit acceptance and reads the exact agreement from the API', () => {
    expect(roles).toContain('getCurrentRoleAgreement(roleType)')
    expect(roles).toContain('agreementAccepted = ref(false)')
    expect(roles).toContain(':disabled="!agreementAccepted"')
    expect(roles).not.toContain('createRoleDepositPayment')
  })
})
