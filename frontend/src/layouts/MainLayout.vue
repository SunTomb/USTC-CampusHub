<template>
  <el-container :class="['app-shell', `theme-${currentChannelTone}`, { 'sidebar-collapsed': sidebarCollapsed }]">
    <div class="ambient-aurora" aria-hidden="true"></div>

    <el-aside width="264px" class="sidebar desktop-sidebar">
      <div class="brand brand-premium">
        <div class="brand-mark-wrap" @click="router.push('/')" style="cursor: pointer">
          <img src="/campushub-logo.png" alt="校集 CampusHub" class="brand-logo-img" />
        </div>
        <div class="brand-text" @click="router.push('/')" style="cursor: pointer">
          <strong>校集 CampusHub</strong>
          <span>中国科大 · 校园生活共同体</span>
        </div>
        <el-button class="sidebar-toggle" size="small" text title="收起侧栏" @click="sidebarCollapsed = true">
          <el-icon><Fold /></el-icon>
        </el-button>
      </div>

      <div class="sidebar-identity">
        <div class="sidebar-identity-head">
          <span class="sidebar-seal">瀚海通行证</span>
          <span class="sidebar-status-dot"></span>
        </div>
        <strong>{{ identityProfile.displayName }}</strong>
        <IdentityBadge :identities="identityProfile.identities" compact />
      </div>

      <nav class="nav-groups" aria-label="桌面端主导航">
        <section v-for="group in navGroups" :key="group.key" class="nav-group">
          <p>{{ group.label }}</p>
          <RouterLink v-for="item in group.items" :key="item.path" :to="item.path" class="nav-link" :title="item.description">
            <span class="nav-link-icon">
              <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
            </span>
            <span class="nav-link-body">
              <strong>{{ item.label }}</strong>
            </span>
          </RouterLink>
        </section>
      </nav>

      <div class="sidebar-footer">
        <span>USTC · 东区 / 西区 / 中区 / 高新</span>
      </div>
    </el-aside>

    <el-container class="content-shell">
      <el-button
        v-if="sidebarCollapsed"
        class="desktop-sidebar-toggle"
        type="primary"
        circle
        title="展开侧栏"
        @click="sidebarCollapsed = false"
      >
        <el-icon><Expand /></el-icon>
      </el-button>

      <el-header class="header premium-header">
        <div class="mobile-header-brand" @click="router.push('/')">
          <img src="/campushub-logo.png" alt="校集 CampusHub" class="brand-logo-img mobile-brand-logo-img" />
          <div>
            <strong>校集 CampusHub</strong>
            <small>{{ currentPageContext.tag }}</small>
          </div>
        </div>

        <div class="mobile-nav-trigger">
          <el-button plain size="small" @click="mobileMenuOpen = true">
            <el-icon style="margin-right: 4px"><Grid /></el-icon>目录
          </el-button>
        </div>

        <div class="header-copy">
          <span class="channel-ribbon">{{ currentPageContext.tag }}</span>
          <span class="channel-divider">·</span>
          <p class="channel-subtitle">{{ currentPageContext.subtitle }}</p>
        </div>

        <div class="session-box premium-session-box">
          <IdentityBadge :identities="identityProfile.identities" compact />
          <RouterLink v-if="auth.currentUser" to="/profile" class="session-user-link">
            {{ auth.currentUser.nickname }}
          </RouterLink>
          <span v-else class="session-guest-label">未登录访客</span>
          <el-button v-if="auth.token" size="small" plain @click="logout">退出</el-button>
          <el-button v-else size="small" type="primary" @click="router.push('/auth')">登录 / 加入</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <RouterView />
      </el-main>
    </el-container>

    <nav class="mobile-tabbar" aria-label="移动端主导航">
      <RouterLink v-for="item in mobileTabItems" :key="item.path" :to="item.path" class="mobile-tabbar-item">
        <span class="mobile-tabbar-icon">
          <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
        </span>
        <span>{{ item.label }}</span>
      </RouterLink>
      <button type="button" class="mobile-tabbar-item mobile-tabbar-button" @click="mobileMenuOpen = true">
        <span class="mobile-tabbar-icon">
          <el-icon><Grid /></el-icon>
        </span>
        <span>更多</span>
      </button>
    </nav>

    <el-drawer v-model="mobileMenuOpen" title="校集 CampusHub" direction="ltr" size="86%" class="mobile-menu-drawer">
      <div class="mobile-directory">
        <div class="mobile-directory-brand">
          <img src="/campushub-logo.png" alt="校集 CampusHub" class="brand-logo-img" />
          <div>
            <strong>校集 CampusHub</strong>
            <small>闲置流转 · 顺路跑腿 · 同窗小店 · 组队展示</small>
          </div>
        </div>
        <div class="sidebar-identity mobile-directory-identity">
          <span class="sidebar-seal">当前通行证</span>
          <strong>{{ identityProfile.displayName }}</strong>
          <IdentityBadge :identities="identityProfile.identities" compact />
        </div>
        <section v-for="group in navGroups" :key="group.key" class="mobile-directory-group">
          <p>{{ group.label }}</p>
          <RouterLink
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="mobile-directory-link"
            @click="mobileMenuOpen = false"
          >
            <span class="mobile-dir-icon">
              <el-icon><component :is="resolveIcon(item.icon)" /></el-icon>
            </span>
            <div>
              <strong>{{ item.label }}</strong>
              <small>{{ item.description }}</small>
            </div>
          </RouterLink>
        </section>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Avatar,
  Bell,
  Briefcase,
  CirclePlus,
  CreditCard,
  DataAnalysis,
  DocumentChecked,
  Expand,
  Fold,
  Goods,
  Grid,
  HomeFilled,
  Key,
  Management,
  Medal,
  Money,
  Position,
  Promotion,
  ScaleToOriginal,
  Shop,
  Tickets,
  User,
  UserFilled,
  Wallet,
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { findNavItemByPath, getMobileTabItems, getVisibleNavGroups } from '@/config/navigation'
import IdentityBadge from '@/components/common/IdentityBadge.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const mobileMenuOpen = ref(false)
const sidebarCollapsed = ref(false)
const identityProfile = computed(() => auth.identityProfile)
const navGroups = computed(() => getVisibleNavGroups(identityProfile.value))
const mobileTabItems = computed(() => getMobileTabItems(identityProfile.value))

const iconRegistry: Record<string, Component> = {
  HomeFilled,
  Position,
  Goods,
  Shop,
  Promotion,
  DocumentChecked,
  UserFilled,
  User,
  Wallet,
  Bell,
  Medal,
  Key,
  CirclePlus,
  Management,
  Briefcase,
  Avatar,
  Tickets,
  DataAnalysis,
  ScaleToOriginal,
  CreditCard,
  Money,
}

function resolveIcon(name: string): Component {
  return iconRegistry[name] ?? Grid
}

const currentChannelTone = computed(() => {
  const path = route.path
  if (path.startsWith('/tasks')) return 'teal'
  if (path.startsWith('/goods')) return 'terracotta'
  if (path.startsWith('/shops')) return 'cobalt'
  if (path.startsWith('/project-ads')) return 'indigo'
  if (path.startsWith('/wallet') || path.startsWith('/roles')) return 'amber'
  return 'navy'
})

const currentPageContext = computed(() => {
  const matched = findNavItemByPath(route.path)
  if (matched) {
    return {
      tag: matched.label,
      subtitle: matched.description,
    }
  }
  if (route.path.startsWith('/goods/')) {
    return { tag: '二手市集 · 详情', subtitle: '确认成色与交接校区，提交意向后即可查看校友联系方式' }
  }
  if (route.path.startsWith('/tasks/')) {
    return { tag: '跑腿悬赏 · 履约台', subtitle: '实时同步取件与送达节点，凭完成码或图片确认交接' }
  }
  if (route.path.startsWith('/shops/')) {
    return { tag: '学生小店 · 预约', subtitle: '浏览同学主理的技能服务，在线预约时间与需求' }
  }
  if (route.path.startsWith('/project-ads/')) {
    return { tag: '项目广场 · 展示', subtitle: '发现科大同窗的科研组队、竞赛招募与创意作品' }
  }
  return {
    tag: '瀚海校集',
    subtitle: '连接中区、西区、东区与高新园区的校园生活服务平台',
  }
})

function logout() {
  auth.clearSession()
  router.push('/auth')
}
</script>
