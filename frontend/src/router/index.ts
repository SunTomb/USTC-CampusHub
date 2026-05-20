import { createRouter, createWebHistory } from 'vue-router'

import MainLayout from '@/layouts/MainLayout.vue'
import HomeView from '@/views/HomeView.vue'
import GoodsView from '@/views/GoodsView.vue'
import TasksView from '@/views/TasksView.vue'
import ShopsView from '@/views/ShopsView.vue'
import ProjectAdsView from '@/views/ProjectAdsView.vue'
import WalletView from '@/views/WalletView.vue'
import RoleApplicationsView from '@/views/RoleApplicationsView.vue'
import NotificationsView from '@/views/NotificationsView.vue'
import AdminReviewView from '@/views/AdminReviewView.vue'
import AuthView from '@/views/AuthView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', name: 'home', component: HomeView },
        { path: 'auth', name: 'auth', component: AuthView },
        { path: 'goods', name: 'goods', component: GoodsView },
        { path: 'tasks', name: 'tasks', component: TasksView },
        { path: 'shops', name: 'shops', component: ShopsView },
        { path: 'project-ads', name: 'project-ads', component: ProjectAdsView },
        { path: 'wallet', name: 'wallet', component: WalletView },
        { path: 'roles', name: 'roles', component: RoleApplicationsView },
        { path: 'notifications', name: 'notifications', component: NotificationsView },
        { path: 'admin/review', name: 'admin-review', component: AdminReviewView },
      ],
    },
  ],
})

export default router
