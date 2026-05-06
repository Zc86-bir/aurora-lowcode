// frontend/src/composables/CrdtSyncEngine.ts
// Yjs-based real-time collaborative editing for the low-code designer
// Features: multi-cursor, conflict auto-merge, offline sync, no central lock

import * as Y from 'yjs'
import { WebsocketProvider } from 'y-websocket'
import { ref, type Ref } from 'vue'

interface OfflineOperation {
  type: 'set' | 'layout-set'
  field: string
  value: unknown
}

export interface CursorPosition {
  userId: string
  userName: string
  color: string
  x: number
  y: number
  selectionStart?: number
  selectionEnd?: number
}

export interface SyncState {
  connected: boolean
  pendingChanges: number
  peers: number
  lastSyncedAt: Date | null
}

export class CrdtSyncEngine {
  // Yjs document
  private doc: Y.Doc
  // Websocket provider
  private provider: WebsocketProvider | null = null
  // Shared types
  private formData: Y.Map<unknown>
  private layoutData: Y.Map<unknown>
  private cursors: Y.Map<Y.Map<unknown>>
  // Vue refs for reactivity
  private connected: Ref<boolean> = ref(false)
  private peers: Ref<number> = ref(0)
  private pendingChanges: Ref<number> = ref(0)
  private lastSyncedAt: Ref<Date | null> = ref(null)
  // Offline queue
  private offlineQueue: OfflineOperation[] = []
  private isOnline = typeof navigator !== 'undefined' ? navigator.onLine : true

  constructor(roomId: string, serverUrl?: string) {
    this.doc = new Y.Doc()

    // Initialize shared types
    this.formData = this.doc.getMap('formData')
    this.layoutData = this.doc.getMap('layoutData')
    this.cursors = this.doc.getMap('cursors')

    // Use environment variable for WebSocket URL, fallback to default
    const wsUrl = serverUrl ?? import.meta.env.VITE_WS_URL ?? 'ws://localhost:1234'

    // Connect to websocket provider (browser-only)
    if (typeof window !== 'undefined') {
      this.provider = new WebsocketProvider(wsUrl, roomId, this.doc, {
        connect: true,
      })
    }

    // Event listeners
    this.provider?.on('sync', (isSynced: boolean) => {
      this.connected.value = isSynced
      if (isSynced) {
        this.lastSyncedAt.value = new Date()
        this.flushOfflineQueue()
      }
    })

    this.provider?.on('status', ({ status }: { status: string }) => {
      this.connected.value = status === 'connected'
    })

    this.provider?.awareness.on('change', () => {
      this.peers.value = this.provider?.awareness.getStates().size ?? 0
    })

    // Offline detection (browser-only)
    if (typeof window !== 'undefined') {
      window.addEventListener('online', this.handleOnline)
      window.addEventListener('offline', this.handleOffline)
    }

    // Track pending changes
    this.doc.on('update', () => {
      const encodedState = Y.encodeStateAsUpdate(this.doc)
      this.pendingChanges.value = encodedState.byteLength
    })
  }

  // ─── Form Data Operations ───

  setFormField(field: string, value: unknown): void {
    const op: OfflineOperation = { type: 'set', field, value }
    if (!this.isOnline || !this.connected.value) {
      this.offlineQueue.push(op)
      return
    }
    this.formData.set(field, value)
  }

  getFormField(field: string): unknown {
    return this.formData.get(field)
  }

  getFormState(): Record<string, unknown> {
    return this.formData.toJSON()
  }

  // ─── Layout Data Operations ───

  setLayoutField(field: string, value: unknown): void {
    if (!this.isOnline || !this.connected.value) {
      this.offlineQueue.push({ type: 'layout-set', field, value })
      return
    }
    this.layoutData.set(field, value)
  }

  getLayoutState(): Record<string, unknown> {
    return this.layoutData.toJSON()
  }

  // ─── Cursor Awareness ───

  updateCursor(userId: string, cursor: CursorPosition): void {
    if (!this.provider) return
    const awareness = this.provider.awareness
    awareness.setLocalState({
      ...awareness.getLocalState(),
      cursor,
    })
  }

  getPeers(): Map<number, CursorPosition> {
    const states = this.provider?.awareness.getStates() ?? new Map()
    const peers = new Map<number, CursorPosition>()
    for (const [clientId, state] of states.entries()) {
      if (state.cursor) {
        peers.set(clientId, state.cursor as CursorPosition)
      }
    }
    return peers
  }

  // ─── Undo/Redo (per-user, local) ───

  private undoManager: Y.UndoManager | null = null

  initUndoManager(trackedTypes: Y.AbstractType<any>[]): void {
    this.undoManager = new Y.UndoManager(trackedTypes, {
      trackedOrigins: new Set(['user-action']),
      captureTimeout: 500,
    })
  }

  undo(): void {
    this.undoManager?.undo()
  }

  redo(): void {
    this.undoManager?.redo()
  }

  // ─── Offline Sync ───

  private handleOnline = () => {
    this.isOnline = true
    this.flushOfflineQueue()
  }

  private handleOffline = () => {
    this.isOnline = false
  }

  private flushOfflineQueue(): void {
    if (!this.isOnline || !this.connected.value) return

    // Deduplicate offline queue by field (last write wins)
    const deduped = new Map<string, OfflineOperation>()
    for (const op of this.offlineQueue) {
      deduped.set(`${op.type}:${op.field}`, op)
    }

    for (const op of deduped.values()) {
      switch (op.type) {
        case 'set':
          this.formData.set(op.field, op.value)
          break
        case 'layout-set':
          this.layoutData.set(op.field, op.value)
          break
      }
    }
    this.offlineQueue = []
  }

  // ─── State ───

  getSyncState(): SyncState {
    return {
      connected: this.connected.value,
      pendingChanges: this.pendingChanges.value,
      peers: this.peers.value,
      lastSyncedAt: this.lastSyncedAt.value,
    }
  }

  // ─── Lifecycle ───

  destroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('online', this.handleOnline)
      window.removeEventListener('offline', this.handleOffline)
    }
    this.provider?.destroy()
    this.doc.destroy()
  }
}
