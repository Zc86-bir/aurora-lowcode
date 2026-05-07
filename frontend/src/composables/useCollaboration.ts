// Wraps CrdtSyncEngine into a Vue composable with provide/inject for app-wide access
import { ref, computed, provide, inject, watch, onUnmounted, type Ref, type InjectionKey } from 'vue'
import { CrdtSyncEngine, type SyncState } from '@/composables/CrdtSyncEngine'

const COLLABORATION_KEY: InjectionKey<CrdtSyncEngine> = Symbol('collaboration')

export interface UseCollaborationOptions {
  roomId: string
  serverUrl?: string
  autoConnect?: boolean
}

export function useCollaboration(options: UseCollaborationOptions) {
  const engine = new CrdtSyncEngine(options.roomId, options.serverUrl)
  const state = ref<SyncState>({ connected: false, pendingChanges: 0, peers: 0, lastSyncedAt: null })

  const updateState = () => {
    state.value = engine.getState()
  }

  const timer = setInterval(updateState, 2000)

  if (options.autoConnect !== false) {
    engine.connect()
  }

  provide(COLLABORATION_KEY, engine)

  onUnmounted(() => {
    clearInterval(timer)
    engine.disconnect()
  })

  return { engine, state: computed(() => state.value) }
}

export function useCollaborationState(): Ref<SyncState> | null {
  const engine = inject(COLLABORATION_KEY, null)
  if (!engine) return null
  const s = ref(engine.getState())
  const timer = setInterval(() => { s.value = engine.getState() }, 2000)
  onUnmounted(() => clearInterval(timer))
  return s
}
