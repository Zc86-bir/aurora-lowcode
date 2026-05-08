export type AiCapabilityStatus = 'Live' | 'Entry' | 'Planned'

export interface AiCapabilityCard {
  title: string
  description: string
  status: AiCapabilityStatus
  to: string
  ctaLabel: string
}

export interface AiStatusPageContent {
  title: string
  description: string
  status: AiCapabilityStatus
  nextMilestone: string
}

export interface AiEntryPageContent {
  title: string
  description: string
  primaryCardTitle: string
  primaryCardBody: string
  supportCopy?: string
  ctaLabel?: string
  ctaTo?: string
}

export function getAiPlatformCards(t: (key: string) => string): AiCapabilityCard[] {
  return [
    { title: t('ai.models'), description: t('ai.modelsDescription'), status: 'Live', to: '/ai/models', ctaLabel: t('ai.openCapability') },
    { title: t('ai.assistant'), description: t('ai.assistantDescription'), status: 'Entry', to: '/ai/assistant', ctaLabel: t('ai.openCapability') },
    { title: t('ai.generation'), description: t('ai.generationDescription'), status: 'Entry', to: '/ai/generation', ctaLabel: t('ai.openCapability') },
    { title: t('ai.ocr'), description: t('ai.ocrDescription'), status: 'Entry', to: '/ai/ocr', ctaLabel: t('ai.openCapability') },
    { title: t('ai.knowledge'), description: t('ai.knowledgeDescription'), status: 'Planned', to: '/ai/knowledge', ctaLabel: t('ai.viewStatus') },
    { title: t('ai.workflows'), description: t('ai.workflowsDescription'), status: 'Planned', to: '/ai/workflows', ctaLabel: t('ai.viewStatus') },
    { title: t('ai.imageChat'), description: t('ai.imageChatDescription'), status: 'Planned', to: '/ai/image-chat', ctaLabel: t('ai.viewStatus') },
    { title: t('ai.embed'), description: t('ai.embedDescription'), status: 'Planned', to: '/ai/embed', ctaLabel: t('ai.viewStatus') },
    { title: t('ai.mobile'), description: t('ai.mobileDescription'), status: 'Planned', to: '/ai/mobile', ctaLabel: t('ai.viewStatus') },
  ]
}

export function getAiStatusPageContent(t: (key: string) => string, key: 'knowledge' | 'workflows' | 'imageChat' | 'embed' | 'mobile'): AiStatusPageContent {
  const map = {
    knowledge: { title: t('ai.knowledge'), description: t('ai.knowledgeStatusDescription'), status: 'Planned' as const, nextMilestone: t('ai.knowledgeNextMilestone') },
    workflows: { title: t('ai.workflows'), description: t('ai.workflowsStatusDescription'), status: 'Planned' as const, nextMilestone: t('ai.workflowsNextMilestone') },
    imageChat: { title: t('ai.imageChat'), description: t('ai.imageChatStatusDescription'), status: 'Planned' as const, nextMilestone: t('ai.imageChatNextMilestone') },
    embed: { title: t('ai.embed'), description: t('ai.embedStatusDescription'), status: 'Planned' as const, nextMilestone: t('ai.embedNextMilestone') },
    mobile: { title: t('ai.mobile'), description: t('ai.mobileStatusDescription'), status: 'Planned' as const, nextMilestone: t('ai.mobileNextMilestone') },
  }

  return map[key]
}

export function getAiEntryPageContent(t: (key: string) => string, key: 'assistant' | 'generation' | 'ocr'): AiEntryPageContent {
  const map = {
    assistant: {
      title: t('ai.assistant'),
      description: t('ai.assistantEntryDescription'),
      primaryCardTitle: t('copilot.title'),
      primaryCardBody: t('ai.assistantEntryBody'),
      supportCopy: t('ai.assistantEntryHint'),
    },
    generation: {
      title: t('ai.generation'),
      description: t('ai.generationEntryDescription'),
      primaryCardTitle: t('generate.app'),
      primaryCardBody: t('ai.generationEntryBody'),
      ctaLabel: t('ai.openCapability'),
      ctaTo: '/online/codegen',
    },
    ocr: {
      title: t('ai.ocr'),
      description: t('ai.ocrEntryDescription'),
      primaryCardTitle: t('ai.ocrSampleTitle'),
      primaryCardBody: t('ai.ocrSampleBody'),
      supportCopy: t('ai.ocrSampleHint'),
    },
  } satisfies Record<'assistant' | 'generation' | 'ocr', AiEntryPageContent>

  return map[key]
}
