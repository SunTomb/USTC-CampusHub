<template>
  <el-container class="app-shell">
    <el-aside width="240px" class="sidebar">
      <div class="brand">
        <strong>校集 CampusHub</strong>
        <span>校园二手交易与微服务平台</span>
      </div>
      <el-menu router :default-active="$route.path" class="menu">
        <el-menu-item index="/">首页总览</el-menu-item>
        <el-menu-item index="/auth">登录注册</el-menu-item>
        <el-menu-item index="/goods">二手商品</el-menu-item>
        <el-menu-item index="/tasks">悬赏任务</el-menu-item>
        <el-menu-item index="/shops">学生店铺</el-menu-item>
        <el-menu-item index="/project-ads">项目广告</el-menu-item>
        <el-menu-item index="/wallet">钱包流水</el-menu-item>
        <el-menu-item index="/roles">身份保证金</el-menu-item>
        <el-menu-item index="/notifications">站内通知</el-menu-item>
        <el-menu-item index="/admin/review">审核治理</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
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
      <el-main>
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
</script>
