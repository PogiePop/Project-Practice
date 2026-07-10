import { createRouter, createWebHistory } from "vue-router";

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/Login.vue'),
        meta: { noAuth: true }
    },
    {
        path: '/',
        component: () => import('@/views/Index.vue'),
        redirect: '/plan/form',
        meta: { requiresAuth: true },
        children: [
            {
                path: '/plan/form',
                name: 'form',
                component: () => import('@/views/plan/Form.vue'),
                meta: { title: '计划清单', requiresAuth: true }
            },
            {
                path: '/plan/track',
                name: 'track',
                component: () => import('@/views/plan/Track.vue'),
                meta: { title: '审批跟踪', requiresAuth: true }
            },
            {
                path: '/object/unit',
                name: 'unit',
                component: () => import('@/views/object/Unit.vue'),
                meta: { title: '被审计单位库', requiresAuth: true }
            },
            {
                path: '/object/lead',
                name: 'lead',
                component: () => import('@/views/object/Lead.vue'),
                meta: { title: '领导干部库', requiresAuth: true }
            },
            {
                path: '/statistic',
                name: 'statistic',
                component: () => import('@/views/statistic/Analysis.vue'),
                meta: { title: '审计进度可视化', requiresAuth: true }
            },
            {
                path: '/settings/profile',
                name: 'profile',
                component: () => import('@/views/settings/Profile.vue'),
                meta: { title: '个人信息', requiresAuth: true }
            },
            {
                path: '/settings/system',
                name: 'system',
                component: () => import('@/views/settings/System.vue'),
                meta: { title: '系统设置', requiresAuth: true }
            },
            {
                path: '/settings/users',
                name: 'users',
                component: () => import('@/views/settings/UserManage.vue'),
                meta: { title: '用户管理', requiresAuth: true, superAdmin: true }
            },
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;
