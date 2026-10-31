import { describe, expect, it } from 'vitest'
import index from '../../index.html?raw'
import shell from '../layouts/MainLayout.vue?raw'
import wallet from '../views/WalletView.vue?raw'
import goods from '../views/GoodsDetailView.vue?raw'
import adminWallet from '../views/AdminWalletView.vue?raw'
import home from '../views/HomeView.vue?raw'
import roles from '../views/RoleApplicationsView.vue?raw'

describe('visible brand and closed-finance regression', () => {
  it('uses the independent brand in the page title and shell', () => {
    expect(index).toContain('蜗壳 CampusHub')
    expect(shell).toContain('蜗壳 CampusHub')
    expect(shell).not.toMatch(/中国科大|中科大|USTC|瀚海|校集 CampusHub/)
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
