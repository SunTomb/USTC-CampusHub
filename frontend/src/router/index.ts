import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'
import { hasAnyRole, type IdentityKey } from '@/utils/identity'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    requiredRole?: IdentityKey
    unlockRoute?: string
    lockedTitle?: string
  }
}

import MainLayout from '@/layouts/MainLayout.vue'
import HomeView from '@/views/HomeView.vue'
import GoodsView from '@/views/GoodsView.vue'
import GoodsPublishView from '@/views/GoodsPublishView.vue'
import GoodsDetailView from '@/views/GoodsDetailView.vue'
import TasksView from '@/views/TasksView.vue'
import TaskWorkspaceView from '@/views/TaskWorkspaceView.vue'
import ShopsView from '@/views/ShopsView.vue'
import ShopDetailView from '@/views/ShopDetailView.vue'
import ShopMerchantView from '@/views/ShopMerchantView.vue'
import ProjectAdsView from '@/views/ProjectAdsView.vue'
import ProjectAdDetailView from '@/views/ProjectAdDetailView.vue'
import ProjectAdManageView from '@/views/ProjectAdManageView.vue'
import WalletView from '@/views/WalletView.vue'
import ProfileView from '@/views/ProfileView.vue'
import CreditCenterView from '@/views/CreditCenterView.vue'
import RoleApplicationsView from '@/views/RoleApplicationsView.vue'
import NotificationsView from '@/views/NotificationsView.vue'
import AdminReviewView from '@/views/AdminReviewView.vue'
import AdminOperationsView from '@/views/AdminOperationsView.vue'
import AdminGovernanceView from '@/views/AdminGovernanceView.vue'
import AdminPaymentView from '@/views/AdminPaymentView.vue'
import AdminWalletView from '@/views/AdminWalletView.vue'
import AuthView from '@/views/AuthView.vue'
import PolicyView from '@/views/PolicyView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'login', redirect: { name: 'auth' } },
        { path: 'auth', name: 'auth', component: AuthView },
        { path: 'policy', name: 'policy', component: PolicyView },
        { path: 'goods', name: 'goods', component: GoodsView },
        { path: 'goods/publish', name: 'goods-publish', component: GoodsPublishView, meta: { requiresAuth: true, requiredRole: 'goodsPublisher', unlockRoute: '/roles', lockedTitle: '需要二手发布者身份' } },
        { path: 'goods/:id', name: 'goods-detail', component: GoodsDetailView },
        { path: 'tasks', name: 'tasks', component: TasksView },
        { path: 'tasks/:id/workspace', name: 'task-workspace', component: TaskWorkspaceView, meta: { requiresAuth: true, requiredRole: 'runner', unlockRoute: '/roles', lockedTitle: '需要跑腿接单者身份' } },
        { path: 'shops', name: 'shops', component: ShopsView },
        { path: 'shops/merchant', name: 'shop-merchant', component: ShopMerchantView, meta: { requiresAuth: true, requiredRole: 'shopMerchant', unlockRoute: '/roles', lockedTitle: '需要店铺商家身份' } },
        { path: 'shops/:id', name: 'shop-detail', component: ShopDetailView },
        { path: 'project-ads', name: 'project-ads', component: ProjectAdsView },
        { path: 'project-ads/manage', name: 'project-ad-manage', component: ProjectAdManageView, meta: { requiresAuth: true } },
        { path: 'project-ads/:id', name: 'project-ad-detail', component: ProjectAdDetailView },
        { path: 'wallet', name: 'wallet', component: WalletView, meta: { requiresAuth: true } },
        { path: 'profile', name: 'profile', component: ProfileView, meta: { requiresAuth: true } },
        { path: 'credit', name: 'credit-center', component: CreditCenterView, meta: { requiresAuth: true } },
        { path: 'roles', name: 'roles', component: RoleApplicationsView, meta: { requiresAuth: true } },
        { path: 'notifications', name: 'notifications', component: NotificationsView, meta: { requiresAuth: true } },
        { path: 'admin/review', name: 'admin-review', component: AdminReviewView, meta: { requiresAdmin: true } },
        { path: 'admin/ops', name: 'admin-ops', component: AdminOperationsView, meta: { requiresAdmin: true } },
        { path: 'admin/governance', name: 'admin-governance', component: AdminGovernanceView, meta: { requiresAdmin: true } },
        { path: 'admin/payment', name: 'admin-payment', component: AdminPaymentView, meta: { requiresAdmin: true } },
        { path: 'admin/wallet', name: 'admin-wallet', component: AdminWalletView, meta: { requiresAdmin: true } },
        { path: ':pathMatch(.*)*', redirect: { name: 'home' } },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.token && !auth.currentUser && !auth.sessionLoaded) {
    await auth.loadCurrentUser()
  }

  const redirect = to.fullPath

  if (to.meta.requiresAdmin) {
    if (!auth.isAuthenticated) {
      ElMessage.warning('请先登录')
      return { name: 'auth', query: { redirect } }
    }
    if (!hasAnyRole(auth.currentUser?.roles, ['admin'])) {
      ElMessage.error('当前账号无权限访问管理页面')
      return { name: 'home', query: { locked: 'admin' } }
    }
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    ElMessage.warning('请先登录')
    return { name: 'auth', query: { redirect } }
  }

  if (to.meta.requiredRole && !hasAnyRole(auth.currentUser?.roles, [to.meta.requiredRole]) && !auth.isAdmin) {
    ElMessage.warning(to.meta.lockedTitle || '请先解锁对应身份')
    return {
      path: to.meta.unlockRoute || '/roles',
      query: { locked: to.meta.requiredRole, redirect },
    }
  }

  return true
})

export default router
