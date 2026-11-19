import { describe, expect, it, vi } from 'vitest'
import * as handoff from './taskHandoff'
import hall from '@/views/TasksView.vue?raw'
import workspace from '@/views/TaskWorkspaceView.vue?raw'
import routes from '@/router/index.ts?raw'

const jpeg = { type: 'image/jpeg', size: 5 * 1024 * 1024 } as File

describe('task handoff', () => {
  it('converts kg exactly to positive whole grams without rounding bad input', () => {
    for (const [kg, grams] of [['0.001', 1], ['1.001', 1001], ['100', 100000]] as const) expect(handoff.weightGrams(kg)).toBe(grams)
    for (const kg of ['', '0', '-1', '0.0009', '100.001', 'NaN', '1e1']) expect(handoff.weightGrams(kg)).toBeNull()
    expect(handoff.weightLabel(null)).toBe('未填写')
  })
  it('only accepts up to three non-empty JPEG or PNG files within 5 MiB', () => {
    expect(handoff.photoProblem([jpeg], 2)).toBe('')
    for (const files of [[jpeg, jpeg], [{ ...jpeg, size: jpeg.size + 1 }], [{ ...jpeg, size: 0 }], [{ ...jpeg, type: 'image/svg+xml' }]]) expect(handoff.photoProblem(files, 2)).not.toBe('')
  })
  it('retries only missing uploads with their original ids and stops after cancellation', async () => {
    const queue = ['first', 'second', 'third'].map(clientRequestId => ({ clientRequestId, file: jpeg, uploaded: false, url: '' }))
    const ctrl = new AbortController()
    const calls: string[] = []
    const upload = vi.fn(async (photo: typeof queue[number]) => { calls.push(photo.clientRequestId); if (calls.length === 2) throw Error('network'); if (calls.length === 3) ctrl.abort() })
    await expect(handoff.uploadMissing(queue, ctrl.signal, upload)).rejects.toThrow('network')
    await expect(handoff.uploadMissing(queue, ctrl.signal, upload)).rejects.toThrow()
    expect(calls).toEqual(['first', 'second', 'second'])
  })
  it('builds relative binary routes only for positive integer ids', () => {
    expect(handoff.evidencePath(12, 34)).toBe('/tasks/12/evidence/34')
    for (const id of [0, -1, 1.5, Infinity, '//other.host' as unknown as number]) expect(() => handoff.evidencePath(id, 1)).toThrow()
  })
  it('does not reveal pickup codes or image metadata in public task cards and opens workspaces for authenticated participants', () => {
    const publicCards = hall.split('<el-drawer')[0]
    expect(publicCards).toContain('weightLabel(task.weightGrams)')
    expect(publicCards).toContain('task.notes')
    expect(publicCards).not.toMatch(/task\.(pickupCode|evidence|requestImages)/)
    expect(routes.split('\n').find(l => l.includes("name: 'task-workspace'"))).not.toContain('requiredRole')
    expect(workspace).toContain('v-if="isPublisher"')
    expect(workspace).not.toContain('v-if="isPhotoMode && isPublisher"')
    expect(workspace).toContain('evidenceTypeLabel(photo.evidenceType)')
    expect(workspace).not.toContain("'123456'")
    expect(hall).toContain('type="file" accept="image/jpeg,image/png" multiple')
    expect(hall).toContain('publishPhotos')
    expect(hall).toContain("uploadTaskEvidence(created.id, 'REQUEST_IMAGE'")
    expect(hall).not.toContain('title="起止路线与时限"')
  })
})
