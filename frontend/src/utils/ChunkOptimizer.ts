// frontend/src/utils/ChunkOptimizer.ts
// Route-level code splitting + preload/prefetch strategy
// Automatic image format detection (AVIF → WebP → fallback)
// LCP budget enforcement < 2.0s

import type { RouteLocationNormalized, Router } from 'vue-router'

export interface ChunkBudget {
  lcpMs: number        // Largest Contentful Paint budget
  fcpMs: number        // First Contentful Paint budget
  ttfbMs: number       // Time to First Byte budget
  totalJsKb: number     // Total JS budget per route
  totalCssKb: number    // Total CSS budget per route
}

const DEFAULT_BUDGET: ChunkBudget = {
  lcpMs: 2000,
  fcpMs: 1500,
  ttfbMs: 200,
  totalJsKb: 200,
  totalCssKb: 50,
}

// Performance observer for LCP tracking
let lcpValue = 0
if (typeof window !== 'undefined' && 'PerformanceObserver' in window) {
  const lcpObserver = new PerformanceObserver((list) => {
    const entries = list.getEntries()
    const last = entries[entries.length - 1]
    if (last) lcpValue = last.startTime
  })
  lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true })
}

export class ChunkOptimizer {
  private budget: ChunkBudget
  private prefetchedChunks = new Set<string>()
  private preloadLinks: Map<string, HTMLLinkElement> = new Map()

  constructor(budget: Partial<ChunkBudget> = {}) {
    this.budget = { ...DEFAULT_BUDGET, ...budget }
  }

  // ─── Route-level prefetching ───

  prefetchRoute(router: Router, routeName: string): void {
    if (this.prefetchedChunks.has(routeName)) return

    const route = router.getRoutes().find(r => r.name === routeName)
    if (!route?.components) return

    // Use requestIdleCallback to avoid blocking main thread
    const idleCallback = window.requestIdleCallback || ((fn: () => void) => setTimeout(fn, 1))
    idleCallback(() => {
      this.loadRouteComponents(route.components as Record<string, unknown>)
      this.prefetchedChunks.add(routeName)
    })
  }

  // Prefetch on hover (for links)
  prefetchOnHover(router: Router, el: HTMLElement, routeName: string): void {
    let prefetchTimeout: ReturnType<typeof setTimeout>

    el.addEventListener('mouseenter', () => {
      prefetchTimeout = setTimeout(() => {
        this.prefetchRoute(router, routeName)
      }, 100) // 100ms hover threshold
    })

    el.addEventListener('mouseleave', () => {
      clearTimeout(prefetchTimeout)
    })

    el.addEventListener('focus', () => {
      this.prefetchRoute(router, routeName)
    })
  }

  // ─── Image format optimization ───

  private static _formatSupport: Record<string, boolean> | null = null

  private static detectFormatSupport(): Record<string, boolean> {
    const canvas = document.createElement('canvas')
    return {
      avif: canvas.toDataURL('image/avif').includes('data:image/avif'),
      webp: canvas.toDataURL('image/webp').includes('data:image/webp'),
    }
  }

  static getOptimalImageFormat(src: string): string {
    if (this._formatSupport === null) {
      this._formatSupport = this.detectFormatSupport()
    }
    if (this._formatSupport.avif) {
      return this.replaceExtension(src, '.avif')
    }
    if (this._formatSupport.webp) {
      return this.replaceExtension(src, '.webp')
    }
    return src
  }

  // Use picture element for automatic fallback
  static createPictureElement(sources: { format: string; src: string }[], fallbackSrc: string): HTMLPictureElement {
    const picture = document.createElement('picture')

    for (const source of sources) {
      const el = document.createElement('source')
      el.srcset = source.src
      el.type = `image/${source.format}`
      picture.appendChild(el)
    }

    const img = document.createElement('img')
    img.src = fallbackSrc
    img.loading = 'lazy'
    img.decoding = 'async'
    picture.appendChild(img)

    return picture
  }

  // ─── Preload critical resources ───

  preload(fontUrl: string, as: 'font' | 'script' | 'style' | 'image'): HTMLLinkElement {
    const existing = this.preloadLinks.get(fontUrl)
    if (existing) return existing

    const link = document.createElement('link')
    link.rel = 'preload'
    link.href = fontUrl
    link.as = as
    if (as === 'font') {
      link.crossOrigin = 'anonymous'
    }
    document.head.appendChild(link)
    this.preloadLinks.set(fontUrl, link)
    return link
  }

  // ─── LCP budget check ───

  checkLcpBudget(): { passed: boolean; actualMs: number; budgetMs: number } {
    return {
      passed: lcpValue <= this.budget.lcpMs,
      actualMs: lcpValue,
      budgetMs: this.budget.lcpMs,
    }
  }

  // ─── Route guard for performance tracking ───

  createPerformanceGuard(router: Router): void {
    // Track navigation start time
    router.beforeEach((to, _from, next) => {
      // Store start time on the route meta
      to.meta.navigationStartTime = performance.now()

      // Prefetch route on navigation start
      this.prefetchRoute(router, to.name as string)

      next()
    })

    // Check performance after navigation complete
    router.afterEach((to) => {
      const startTime = to.meta.navigationStartTime as number | undefined
      if (!startTime) return

      const loadTime = performance.now() - startTime
      if (loadTime > this.budget.lcpMs) {
        // eslint-disable-next-line no-console
        console.warn(`Route ${String(to.name)} load time ${loadTime.toFixed(0)}ms exceeds LCP budget`)
      }
    })
  }

  // ─── Internal ───

  private loadRouteComponents(components: Record<string, unknown>): void {
    for (const component of Object.values(components)) {
      if (typeof component === 'function') {
        ;(component as () => Promise<unknown>)()
      }
    }
  }

  private static replaceExtension(src: string, ext: string): string {
    return src.replace(/\.(png|jpg|jpeg|gif|webp|avif)$/i, ext)
  }
}
