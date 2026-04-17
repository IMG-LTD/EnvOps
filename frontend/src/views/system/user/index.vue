<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

defineOptions({
  name: 'SystemUserPage'
});

const { t } = useI18n();

const metrics = computed(() => [
  {
    key: 'users',
    label: t('page.envops.systemUser.summary.users.label'),
    value: '28',
    desc: t('page.envops.systemUser.summary.users.desc')
  },
  {
    key: 'admins',
    label: t('page.envops.systemUser.summary.admins.label'),
    value: '6',
    desc: t('page.envops.systemUser.summary.admins.desc')
  },
  {
    key: 'activeToday',
    label: t('page.envops.systemUser.summary.activeToday.label'),
    value: '17',
    desc: t('page.envops.systemUser.summary.activeToday.desc')
  }
]);

const users = computed(() => [
  {
    userName: 'lin.sre',
    role: t('page.envops.common.role.platformAdmin'),
    team: t('page.envops.common.team.sre'),
    loginType: t('page.envops.common.loginType.passwordOtp'),
    lastLogin: '2026-04-15 09:12',
    status: t('page.envops.common.status.active'),
    statusType: 'success' as const
  },
  {
    userName: 'amy.ops',
    role: t('page.envops.common.role.releaseManager'),
    team: t('page.envops.common.team.envops'),
    loginType: t('page.envops.common.loginType.password'),
    lastLogin: '2026-04-15 08:46',
    status: t('page.envops.common.status.active'),
    statusType: 'success' as const
  },
  {
    userName: 'jack.release',
    role: t('page.envops.common.role.trafficOwner'),
    team: t('page.envops.common.team.traffic'),
    loginType: t('page.envops.common.loginType.sso'),
    lastLogin: '2026-04-15 08:20',
    status: t('page.envops.common.status.review'),
    statusType: 'warning' as const
  },
  {
    userName: 'luna.qa',
    role: t('page.envops.common.role.observer'),
    team: t('page.envops.common.team.qa'),
    loginType: t('page.envops.common.loginType.sso'),
    lastLogin: '2026-04-14 18:10',
    status: t('page.envops.common.status.disabled'),
    statusType: 'default' as const
  }
]);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.systemUser.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.systemUser.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="success">{{ t('page.envops.systemUser.tags.rbacEnabled') }}</NTag>
          <NTag type="warning">{{ t('page.envops.systemUser.tags.userUnderReview') }}</NTag>
        </NSpace>
      </div>
    </NCard>

    <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in metrics" :key="item.key">
        <NCard :bordered="false" class="card-wrapper">
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NCard :title="t('page.envops.systemUser.table.title')" :bordered="false" class="card-wrapper">
      <NTable :bordered="false" :single-line="false">
        <thead>
          <tr>
            <th>{{ t('page.envops.systemUser.table.user') }}</th>
            <th>{{ t('page.envops.systemUser.table.role') }}</th>
            <th>{{ t('page.envops.systemUser.table.team') }}</th>
            <th>{{ t('page.envops.systemUser.table.loginType') }}</th>
            <th>{{ t('page.envops.systemUser.table.lastLogin') }}</th>
            <th>{{ t('page.envops.systemUser.table.status') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in users" :key="item.userName">
            <td>{{ item.userName }}</td>
            <td>{{ item.role }}</td>
            <td>{{ item.team }}</td>
            <td>{{ item.loginType }}</td>
            <td>{{ item.lastLogin }}</td>
            <td>
              <NTag :type="item.statusType" size="small">{{ item.status }}</NTag>
            </td>
          </tr>
        </tbody>
      </NTable>
    </NCard>
  </NSpace>
</template>

<style scoped></style>
