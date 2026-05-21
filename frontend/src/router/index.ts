import { createRouter, createWebHistory } from 'vue-router'

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
import WalletView from '@/views/WalletView.vue'
import RoleApplicationsView from '@/views/RoleApplicationsView.vue'
import NotificationsView from '@/views/NotificationsView.vue'
import AdminReviewView from '@/views/AdminReviewView.vue'
import AdminOperationsView from '@/views/AdminOperationsView.vue'
import AuthView from '@/views/AuthView.vue'

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
        { path: 'goods', name: 'goods', component: GoodsView },
        { path: 'goods/publish', name: 'goods-publish', component: GoodsPublishView },
        { path: 'goods/:id', name: 'goods-detail', component: GoodsDetailView },
        { path: 'tasks', name: 'tasks', component: TasksView },
        { path: 'tasks/:id/workspace', name: 'task-workspace', component: TaskWorkspaceView },
        { path: 'shops', name: 'shops', component: ShopsView },
        { path: 'shops/merchant', name: 'shop-merchant', component: ShopMerchantView },
        { path: 'shops/:id', name: 'shop-detail', component: ShopDetailView },
        { path: 'project-ads', name: 'project-ads', component: ProjectAdsView },
        { path: 'wallet', name: 'wallet', component: WalletView },
        { path: 'roles', name: 'roles', component: RoleApplicationsView },
        { path: 'notifications', name: 'notifications', component: NotificationsView },
        { path: 'admin/review', name: 'admin-review', component: AdminReviewView },
        { path: 'admin/ops', name: 'admin-ops', component: AdminOperationsView },
        { path: ':pathMatch(.*)*', redirect: { name: 'home' } },
      ],
    },
  ],
})

export default router
