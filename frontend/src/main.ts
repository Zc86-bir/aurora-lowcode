import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import i18n from './i18n'
import App from './App.vue'
import './plugins/api-interceptor'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(i18n)

// Global error handler
app.config.errorHandler = (err, instance, info) => {
  console.error('[Aurora Global Error]', err, info)
}

app.mount('#app')
