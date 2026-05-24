<template>
  <el-container class="app-shell">
    <el-aside width="240px" class="sidebar desktop-sidebar">
      <div class="brand">
        <strong>校集 CampusHub</strong>
        <span>校园二手交易与微服务平台</span>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">{{ item.label }}</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="mobile-nav-trigger">
          <el-button plain @click="mobileMenuOpen = true">完整目录</el-button>
        </div>
        <div>
          <h1>CampusHub 本地演示原型</h1>
          <p>二手交易 × 跑腿悬赏 × 学生技能服务 × 信用治理</p>
        </div>
        <div class="session-box">
          <span v-if="auth.currentUser">{{ auth.currentUser.nickname }}</span>
          <span v-else>未登录</span>
          <el-button v-if="auth.token" size="small" plain @click="auth.clearSession()">退出</el-button>
          <el-button v-else size="small" type="primary" plain @click="$router.push('/auth')">登录</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>

    <nav class="mobile-tabbar" aria-label="移动端主导航">
      <RouterLink v-for="item in mobileTabItems" :key="item.path" :to="item.path" class="mobile-tabbar-item">
        <span class="mobile-tabbar-icon">{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </RouterLink>
      <button type="button" class="mobile-tabbar-item mobile-tabbar-button" @click="mobileMenuOpen = true">
        <span class="mobile-tabbar-icon">☰</span>
        <span>更多</span>
      </button>
    </nav>

    <el-drawer v-model="mobileMenuOpen" title="校集 CampusHub" direction="ltr" size="82%" class="mobile-menu-drawer">
      <el-menu router :default-active="$route.path" class="menu" @select="mobileMenuOpen = false">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">{{ item.label }}</el-menu-item>
      </el-menu>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const mobileMenuOpen = ref(false)

const navItems = [
  { path: '/', label: '首页总览' },
  { path: '/auth', label: '登录注册' },
  { path: '/policy', label: '协议与风险' },
  { path: '/goods', label: '二手商品' },
  { path: '/tasks', label: '悬赏任务' },
  { path: '/shops', label: '学生店铺' },
  { path: '/project-ads', label: '项目广告' },
  { path: '/wallet', label: '钱包流水' },
  { path: '/credit', label: '信用中心' },
  { path: '/roles', label: '身份保证金' },
  { path: '/notifications', label: '站内通知' },
  { path: '/admin/review', label: '审核治理' },
  { path: '/admin/ops', label: '运营后台' },
  { path: '/admin/governance', label: '治理台' },
  { path: '/admin/payment', label: '支付监控' },
  { path: '/admin/wallet', label: '钱包运营' }
]

const mobileTabItems = [
  { path: '/', label: '首页', icon: '⌂' },
  { path: '/tasks', label: '跑腿', icon: '↗' },
  { path: '/goods', label: '二手', icon: '◈' },
  { path: '/shops', label: '店铺', icon: '✦' },
  { path: '/notifications', label: '通知', icon: '●' }
]
</script>
