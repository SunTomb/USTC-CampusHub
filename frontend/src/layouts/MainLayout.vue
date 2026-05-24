<template>
  <el-container class="app-shell">
    <el-aside width="272px" class="sidebar desktop-sidebar">
      <div class="brand brand-premium">
        <span class="brand-mark">CH</span>
        <div>
          <strong>校集 CampusHub</strong>
          <span>真实校园服务运营平台</span>
        </div>
      </div>

      <div class="sidebar-identity">
        <p class="eyebrow">Current Identity</p>
        <strong>{{ identityProfile.displayName }}</strong>
        <IdentityBadge :identities="identityProfile.identities" compact />
      </div>

      <nav class="nav-groups" aria-label="桌面端主导航">
        <section v-for="group in navGroups" :key="group.key" class="nav-group">
          <p>{{ group.label }}</p>
          <RouterLink v-for="item in group.items" :key="item.path" :to="item.path" class="nav-link">
            <span class="nav-link-icon">{{ item.icon }}</span>
            <span>
              <strong>{{ item.label }}</strong>
              <small>{{ item.description }}</small>
            </span>
          </RouterLink>
        </section>
      </nav>
    </el-aside>

    <el-container>
      <el-header class="header premium-header">
        <div class="mobile-nav-trigger">
          <el-button plain @click="mobileMenuOpen = true">完整目录</el-button>
        </div>
        <div class="header-copy">
          <p class="eyebrow">Campus Service OS</p>
          <h1>校集 CampusHub</h1>
          <p>二手交易 × 跑腿悬赏 × 学生店铺 × 信用治理 × 钱包运营</p>
        </div>
        <div class="session-box premium-session-box">
          <IdentityBadge :identities="identityProfile.identities" compact />
          <span v-if="auth.currentUser">{{ auth.currentUser.nickname }}</span>
          <span v-else>未登录游客</span>
          <el-button v-if="auth.token" size="small" plain @click="auth.clearSession()">退出</el-button>
          <el-button v-else size="small" type="primary" @click="$router.push('/auth')">登录注册</el-button>
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

    <el-drawer v-model="mobileMenuOpen" title="校集 CampusHub" direction="ltr" size="86%" class="mobile-menu-drawer">
      <div class="mobile-directory">
        <div class="sidebar-identity mobile-directory-identity">
          <p class="eyebrow">当前身份</p>
          <strong>{{ identityProfile.displayName }}</strong>
          <IdentityBadge :identities="identityProfile.identities" compact />
        </div>
        <section v-for="group in navGroups" :key="group.key" class="mobile-directory-group">
          <p>{{ group.label }}</p>
          <RouterLink v-for="item in group.items" :key="item.path" :to="item.path" class="mobile-directory-link" @click="mobileMenuOpen = false">
            <span>{{ item.icon }}</span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </RouterLink>
        </section>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMobileTabItems, getVisibleNavGroups } from '@/config/navigation'
import IdentityBadge from '@/components/common/IdentityBadge.vue'

const auth = useAuthStore()
const mobileMenuOpen = ref(false)
const identityProfile = computed(() => auth.identityProfile)
const navGroups = computed(() => getVisibleNavGroups(identityProfile.value))
const mobileTabItems = computed(() => getMobileTabItems(identityProfile.value))
</script>
