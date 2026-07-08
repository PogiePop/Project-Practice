import { createApp } from 'vue'
import ELementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { zhCn } from 'element-plus/es/locales.mjs'
import App from './App.vue'
import * as ELementPlusIconsVue from '@element-plus/icons-vue'
import router from './router/router.js'

const app = createApp(App);

app.use(ELementPlus, {
    locale: zhCn,
});

for(const [key, component] of Object.entries(ELementPlusIconsVue)) {
    app.component(key, component);
}

app.use(router);

app.mount("#app");
