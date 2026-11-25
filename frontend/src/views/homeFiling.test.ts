import { describe, it, expect } from 'vitest'
import { parse } from '@vue/compiler-sfc'
import home from './HomeView.vue?raw'

describe('网站首页备案标识', () => {
  it('places the exact website filing link in the last footer for all visitors', () => {
    const template = parse(home).descriptor.template!.content
    expect(template).toMatch(/<footer class="home-filing">\s*<small>Copyright © 2026 蜗壳同路 CampusHub\. All Rights Reserved\.<\/small>\s*<a href="http:\/\/beian\.miit\.gov\.cn\/"[^>]*>浙ICP备2026099873号-2<\/a>\s*<\/footer>\s*<\/section>\s*$/)
  })
})
