// Wraps CrdtSyncEngine into a Vue composable
import { ref, onUnmounted, provide, inject, type InjectionKey } from 'vue'
import { CrdtSyncEngine } from '@/composables/CrdtSyncEngine'

const COLLABORATION_KEY: InjectionKey<CrdtSyncEngine> = Symbol('collaboration')

export function useCollaboration(roomId: string, serverUrl?: string) {
  const engine = new CrdtSyncEngine(roomId, serverUrl)
  provide(COLLABORATION_KEY, engine)

  onUnmounted(() => {
    engine.destroy()
  })

  return { engine }
}

export function useCollaborationEngine(): CrdtSyncEngine | null {
  return inject(COLLABORATION_KEY, null)
}
