// frontend/src/utils/A11yTestRunner.ts
// axe-core integration — automated accessibility scanning
// CI stage: blocks build on violations
// Runtime: scans current page on route change

import axe, { type AxeResults, type RunOptions, type Spec } from 'axe-core'

// Logger utility — replace with production logger (pino/winston)
const logger = {
  error: (message: string, data?: unknown) => {
    // In production: pino.error(message, data)
    // eslint-disable-next-line no-console
    console.error(`[A11y] ${message}`, data ?? '')
  },
  warn: (message: string) => {
    // In production: pino.warn(message)
    // eslint-disable-next-line no-console
    console.warn(`[A11y] ${message}`)
  },
}

export interface A11yViolation {
  id: string
  impact: 'minor' | 'moderate' | 'serious' | 'critical'
  description: string
  help: string
  helpUrl: string
  nodes: Array<{
    html: string
    target: string[]
    failureSummary: string
  }>
}

export interface A11yReport {
  timestamp: string
  url: string
  violations: A11yViolation[]
  passes: string[]
  incomplete: string[]
  inapplicable: string[]
  duration: number
}

function normalizeAxeTarget(target: unknown): string[] {
  if (Array.isArray(target)) {
    return target.flatMap(item => normalizeAxeTarget(item))
  }
  return target == null ? [] : [String(target)]
}

export class A11yTestRunner {
  private axeConfig: Partial<Spec> = {
    branding: {
      brand: 'aurora-lowcode',
    },
  }

  private runOptions: RunOptions = {
    runOnly: {
      type: 'tag',
      values: ['wcag2a', 'wcag2aa', 'wcag21aa'],
    },
  }

  initialize(): void {
    if (typeof window === 'undefined') return
    axe.configure(this.axeConfig)
  }

  async scan(options?: RunOptions): Promise<A11yReport> {
    const start = performance.now()

    const results: AxeResults = await axe.run(
      document.body,
      { ...this.runOptions, ...options }
    )

    const violations: A11yViolation[] = results.violations.map(v => ({
      id: v.id,
      impact: v.impact as A11yViolation['impact'],
      description: v.description,
      help: v.help,
      helpUrl: v.helpUrl,
      nodes: v.nodes.map(n => ({
        html: n.html,
        target: normalizeAxeTarget(n.target),
        failureSummary: n.failureSummary ?? '',
      })),
    }))

    return {
      timestamp: new Date().toISOString(),
      url: window.location.href,
      violations,
      passes: results.passes.map(p => p.id),
      incomplete: results.incomplete.map(i => i.id),
      inapplicable: results.inapplicable.map(i => i.id),
      duration: performance.now() - start,
    }
  }

  async scanElement(selector: string): Promise<A11yReport> {
    const element = document.querySelector(selector)
    if (!element) {
      throw new Error(`Element not found: ${selector}`)
    }

    const results: AxeResults = await axe.run(element, this.runOptions)

    return {
      timestamp: new Date().toISOString(),
      url: window.location.href,
      violations: results.violations.map(v => ({
        id: v.id,
        impact: v.impact as A11yViolation['impact'],
        description: v.description,
        help: v.help,
        helpUrl: v.helpUrl,
        nodes: v.nodes.map(n => ({
          html: n.html,
          target: normalizeAxeTarget(n.target),
          failureSummary: n.failureSummary ?? '',
        })),
      })),
      passes: results.passes.map(p => p.id),
      incomplete: results.incomplete.map(i => i.id),
      inapplicable: results.inapplicable.map(i => i.id),
      duration: 0,
    }
  }

  async scanAndFailIfViolations(threshold: 'any' | 'critical' | 'serious' = 'any'): Promise<A11yReport> {
    const report = await this.scan()

    const blockingViolations = report.violations.filter(v => {
      if (threshold === 'critical') return v.impact === 'critical'
      if (threshold === 'serious') return ['critical', 'serious'].includes(v.impact)
      return true
    })

    if (blockingViolations.length > 0) {
      const details = blockingViolations.map(v => ({
        id: v.id,
        impact: v.impact,
        description: v.description,
        nodes: v.nodes.map(n => n.html),
      }))
      logger.error(`Violations found (${blockingViolations.length})`, details)
      throw new A11yViolationError(blockingViolations)
    }

    return report
  }

  static getFixSuggestions(violation: A11yViolation): string[] {
    const suggestions: string[] = []

    switch (violation.id) {
      case 'color-contrast':
        suggestions.push('Increase text/background contrast ratio to at least 4.5:1 (WCAG AA)')
        break
      case 'button-name':
        suggestions.push('Add aria-label or text content to button elements')
        break
      case 'image-alt':
        suggestions.push('Add alt attribute to img elements with descriptive text')
        break
      case 'link-name':
        suggestions.push('Add accessible name to link elements (text content or aria-label)')
        break
      case 'form-field-multiple-labels':
        suggestions.push('Use a single label per form field or use aria-labelledby')
        break
      case 'focus-order-semantics':
        suggestions.push('Ensure focusable elements are in a logical tab order')
        break
      case 'aria-required-attr':
        suggestions.push('Add required ARIA attributes to this role')
        break
      case 'landmark-one-main':
        suggestions.push('Add a <main> landmark to the page')
        break
    }

    return suggestions
  }
}

class A11yViolationError extends Error {
  constructor(public violations: A11yViolation[]) {
    super(`Accessibility violations found: ${violations.length}`)
    this.name = 'A11yViolationError'
  }
}

// Vue Router integration — scan on route change
export function setupA11yRouterGuard(router: { afterEach: (fn: () => void) => void }): void {
  const runner = new A11yTestRunner()
  runner.initialize()

  router.afterEach(async () => {
    await new Promise(resolve => setTimeout(resolve, 500))

    try {
      const report = await runner.scan()
      if (report.violations.length > 0) {
        logger.warn(`${report.violations.length} violations on ${report.url}`)
      }
    } catch {
      // Scan failed — don't block navigation
    }
  })
}
