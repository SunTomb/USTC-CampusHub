import { NodeTypes, type ElementNode } from '@vue/compiler-dom'
import { parse } from '@vue/compiler-sfc'
import { describe, expect, it } from 'vitest'
import home from './HomeView.vue?raw'

function attribute(node: ElementNode, name: string) {
  for (const prop of node.props) {
    if (prop.type === NodeTypes.ATTRIBUTE && prop.name === name) return prop.value?.content
  }
}

function text(node: ElementNode): string {
  return node.children.map(child => child.type === NodeTypes.TEXT ? child.content : child.type === NodeTypes.ELEMENT ? text(child) : '').join('').trim()
}

describe('网站首页页脚', () => {
  it('ends the homepage with the copyright, filing and repository links', () => {
    const section = parse(home).descriptor.template!.ast!.children[0]
    if (section.type !== NodeTypes.ELEMENT || section.tag !== 'section') throw new Error('Missing homepage section')
    const footer = section.children[section.children.length - 1]
    if (footer.type !== NodeTypes.ELEMENT || footer.tag !== 'footer') throw new Error('Footer must end the homepage section')
    expect(attribute(footer, 'class')).toBe('home-filing')

    const elements = footer.children.filter((child): child is ElementNode => child.type === NodeTypes.ELEMENT)
    expect(elements.filter(node => node.tag === 'small').map(text)).toContain('Copyright © 2026 蜗壳同路 CampusHub. All Rights Reserved.')
    for (const [href, label] of [
      ['http://beian.miit.gov.cn/', '浙ICP备2026099873号-2'],
      ['https://github.com/SunTomb/USTC-CampusHub', 'GitHub 开源 · 欢迎点个 Star'],
    ]) {
      const link = elements.find(node => node.tag === 'a' && attribute(node, 'href') === href)
      if (!link) throw new Error(`Missing footer link: ${href}`)
      expect(attribute(link, 'target')).toBe('_blank')
      expect(attribute(link, 'rel')).toBe('noopener noreferrer')
      expect(text(link)).toBe(label)
    }
  })
})
