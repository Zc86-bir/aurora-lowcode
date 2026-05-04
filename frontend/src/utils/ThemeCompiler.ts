// frontend/src/utils/ThemeCompiler.ts
// Design Token → CSS Variables compiler
// Build-time: compiles token definitions to CSS
// Runtime: theme switch < 50ms, supports high-contrast / colorblind modes

export type ThemeMode = 'light' | 'dark' | 'high-contrast' | 'colorblind'
export type TokenValue = string | number
export type TokenCategory = 'color' | 'spacing' | 'typography' | 'border' | 'shadow' | 'z-index' | 'layout'

export interface DesignToken {
  name: string
  value: TokenValue
  category: TokenCategory
  description?: string
  alias?: string // Reference to another token
}

export interface ThemeDefinition {
  name: ThemeMode
  tokens: Record<string, TokenValue>
  extends?: ThemeMode
}

export class ThemeCompiler {
  private baseTokens: Map<string, DesignToken> = new Map()
  private themes: Map<ThemeMode, ThemeDefinition> = new Map()
  private cssCache = new Map<ThemeMode, string>()

  // ─── Token Registration ───

  registerToken(token: DesignToken): void {
    this.baseTokens.set(token.name, token)
  }

  registerTokens(tokens: DesignToken[]): void {
    for (const token of tokens) {
      this.baseTokens.set(token.name, token)
    }
  }

  // ─── Theme Registration ───

  registerTheme(theme: ThemeDefinition): void {
    this.themes.set(theme.name, theme)
    this.cssCache.delete(theme.name) // Invalidate cache
  }

  // ─── Compilation ───

  compile(theme: ThemeMode): string {
    // Check cache
    const cached = this.cssCache.get(theme)
    if (cached) return cached

    const themeDef = this.themes.get(theme)
    if (!themeDef) {
      // eslint-disable-next-line no-console
      console.warn(`Theme "${theme}" not found, falling back to light`)
      return this.compile('light')
    }

    // Resolve extended theme tokens
    const tokens = this.resolveInheritance(themeDef)

    // Generate CSS
    const css = this.generateCSS(theme, tokens)

    // Cache with size limit (max 10 themes)
    if (this.cssCache.size >= 10) {
      const firstKey = this.cssCache.keys().next().value
      if (firstKey) this.cssCache.delete(firstKey)
    }
    this.cssCache.set(theme, css)
    return css
  }

  compileAll(): string {
    let result = ''
    for (const [mode] of this.themes) {
      result += this.compile(mode)
      result += '\n\n'
    }
    return result
  }

  // ─── Runtime Injection ───

  inject(theme: ThemeMode, target?: HTMLElement): void {
    if (typeof document === 'undefined') return // SSR guard

    const start = performance.now()
    const el = target ?? document.documentElement
    el.setAttribute('data-theme', theme)

    const css = this.compile(theme)
    const id = `theme-${theme}`
    let styleEl = document.getElementById(id)

    if (!styleEl) {
      styleEl = document.createElement('style')
      styleEl.id = id
      styleEl.setAttribute('data-theme-token', 'true')
      document.head.appendChild(styleEl)
    }

    styleEl.textContent = css

    const elapsed = performance.now() - start
    if (elapsed > 50) {
      console.warn(`Theme switch took ${elapsed.toFixed(1)}ms (budget: 50ms)`)
    }
  }

  // ─── Token Resolution ───

  resolveInheritance(themeDef: ThemeDefinition): Record<string, TokenValue> {
    const resolved: Record<string, TokenValue> = {}

    // First, resolve parent theme
    if (themeDef.extends) {
      const parent = this.themes.get(themeDef.extends)
      if (parent) {
        Object.assign(resolved, parent.tokens)
      }
    }

    // Then override with current theme tokens
    Object.assign(resolved, themeDef.tokens)

    // Resolve aliases
    for (const [key, value] of Object.entries(resolved)) {
      if (typeof value === 'string' && value.startsWith('var(')) {
        // Already a CSS variable reference — keep as-is
        continue
      }
    }

    return resolved
  }

  // ─── Internal ───

  private generateCSS(theme: ThemeMode, tokens: Record<string, TokenValue>): string {
    const lines = [`[data-theme='${theme}'], :root {`]

    for (const [key, value] of Object.entries(tokens)) {
      const cssVar = this.toCssVarName(key)
      lines.push(`  ${cssVar}: ${value};`)
    }

    lines.push('}')
    return lines.join('\n')
  }

  private toCssVarName(name: string): string {
    return `--${name.replace(/([A-Z])/g, '-$1').toLowerCase()}`
  }

  // ─── Presets ───

  static defaultTokens(): DesignToken[] {
    return [
      { name: 'space-base', value: '4px', category: 'spacing' },
      { name: 'font-sans', value: "'Inter', system-ui, sans-serif", category: 'typography' },
      { name: 'font-mono', value: "'JetBrains Mono', monospace", category: 'typography' },
      { name: 'radius-md', value: '4px', category: 'border' },
      { name: 'radius-lg', value: '8px', category: 'border' },
      { name: 'shadow-sm', value: '0 1px 2px 0 rgb(0 0 0 / 0.05)', category: 'shadow' },
      { name: 'shadow-md', value: '0 4px 6px -1px rgb(0 0 0 / 0.1)', category: 'shadow' },
    ]
  }

  static defaultThemes(): ThemeDefinition[] {
    return [
      {
        name: 'light',
        tokens: {
          'color-bg-primary': '#ffffff',
          'color-text-primary': '#111827',
          'color-text-secondary': '#6b7280',
          'color-border-primary': '#e5e7eb',
          'color-primary-500': '#3b82f6',
          'color-success-500': '#22c55e',
          'color-warning-500': '#f59e0b',
          'color-danger-500': '#ef4444',
        },
      },
      {
        name: 'dark',
        tokens: {
          'color-bg-primary': '#111827',
          'color-text-primary': '#f9fafb',
          'color-text-secondary': '#9ca3af',
          'color-border-primary': '#374151',
          'color-primary-500': '#60a5fa',
        },
        extends: 'light',
      },
      {
        name: 'high-contrast',
        tokens: {
          'color-bg-primary': '#000000',
          'color-text-primary': '#ffffff',
          'color-text-secondary': '#e5e7eb',
          'color-border-primary': '#ffffff',
          'color-primary-500': '#4da3ff',
        },
      },
      {
        name: 'colorblind',
        tokens: {
          'color-success-500': '#f59e0b', // Amber instead of green
          'color-warning-500': '#8b5cf6', // Purple instead of orange
        },
        extends: 'light',
      },
    ]
  }
}
