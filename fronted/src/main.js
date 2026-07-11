import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/element-override.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// Monaco Editor Worker 配置（必须在任何 Monaco 使用前加载）
import './utils/monacoSetup'

// 按需导入实际使用的图标（替代全局注册 ~200+ 图标，减少 ~300KB 打包体积）
import {
  ArrowDown, ArrowLeft, ArrowRight, Bell, Calendar, CaretRight, ChatDotRound, ChatLineSquare,
  Checked, CircleCheck, CircleCheckFilled, CircleCloseFilled, Clock, Close,
  Collection, Connection, DataAnalysis, Delete, Document, DocumentCopy, Edit, Expand,
  Fold, Folder, FullScreen, Hide, Iphone, List, Loading, Location, Notebook,
  Odometer, PictureFilled, Plus, QuestionFilled, Reading, Refresh, School,
  Search, Setting, Star, SwitchButton, Timer, Tools, TrendCharts, UploadFilled, User, VideoCamera, View, WarningFilled
} from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { installKeyboardNavigation } from './utils/keyboardNavigation'

const app = createApp(App)

// 按需注册实际使用的图标
const icons = {
  ArrowDown, ArrowLeft, ArrowRight, Bell, Calendar, CaretRight, ChatDotRound, ChatLineSquare,
  Checked, CircleCheck, CircleCheckFilled, CircleCloseFilled, Clock, Close,
  Collection, Connection, DataAnalysis, Delete, Document, DocumentCopy, Edit, Expand,
  Fold, Folder, FullScreen, Hide, Iphone, List, Loading, Location, Notebook,
  Odometer, PictureFilled, Plus, QuestionFilled, Reading, Refresh, School,
  Search, Setting, Star, SwitchButton, Timer, Tools, TrendCharts, UploadFilled, User, VideoCamera, View, WarningFilled
}
for (const [key, component] of Object.entries(icons)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
installKeyboardNavigation()
