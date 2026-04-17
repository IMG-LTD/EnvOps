<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

defineOptions({
  name: 'TrafficControllerPage'
});

const { t } = useI18n();

const metrics = computed(() => [
  {
    key: 'policiesEnabled',
    label: t('page.envops.trafficController.summary.policiesEnabled.label'),
    value: '14',
    desc: t('page.envops.trafficController.summary.policiesEnabled.desc')
  },
  {
    key: 'canaryReleases',
    label: t('page.envops.trafficController.summary.canaryReleases.label'),
    value: '3',
    desc: t('page.envops.trafficController.summary.canaryReleases.desc')
  },
  {
    key: 'rollbackReady',
    label: t('page.envops.trafficController.summary.rollbackReady.label'),
    value: '100%',
    desc: t('page.envops.trafficController.summary.rollbackReady.desc')
  }
]);

const trafficPolicies = computed(() => [
  {
    app: 'payment-gateway',
    strategy: t('page.envops.common.strategy.headerCanary'),
    scope: 'prod / cn-shanghai-a',
    ratio: '20%',
    owner: t('page.envops.common.team.traffic'),
    status: t('page.envops.common.status.enabled'),
    statusType: 'success' as const
  },
  {
    app: 'traffic-admin',
    strategy: t('page.envops.common.strategy.blueGreen'),
    scope: 'staging / all',
    ratio: '100%',
    owner: t('page.envops.common.team.release'),
    status: t('page.envops.common.status.preview'),
    statusType: 'info' as const
  },
  {
    app: 'asset-sync',
    strategy: t('page.envops.common.strategy.weightedRouting'),
    scope: 'prod / cn-beijing-b',
    ratio: '10%',
    owner: t('page.envops.common.team.platform'),
    status: t('page.envops.common.status.review'),
    statusType: 'warning' as const
  },
  {
    app: 'ops-worker',
    strategy: t('page.envops.common.strategy.emergencyRollback'),
    scope: 'prod / all',
    ratio: '0%',
    owner: t('page.envops.common.team.sre'),
    status: t('page.envops.common.status.standby'),
    statusType: 'default' as const
  }
]);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.trafficController.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.trafficController.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="success">{{ t('page.envops.trafficController.tags.policiesLive') }}</NTag>
          <NTag type="info">{{ t('page.envops.trafficController.tags.previewPolicy') }}</NTag>
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

    <NCard :title="t('page.envops.trafficController.table.title')" :bordered="false" class="card-wrapper">
      <NTable :bordered="false" :single-line="false">
        <thead>
          <tr>
            <th>{{ t('page.envops.trafficController.table.application') }}</th>
            <th>{{ t('page.envops.trafficController.table.strategy') }}</th>
            <th>{{ t('page.envops.trafficController.table.scope') }}</th>
            <th>{{ t('page.envops.trafficController.table.trafficRatio') }}</th>
            <th>{{ t('page.envops.trafficController.table.owner') }}</th>
            <th>{{ t('page.envops.trafficController.table.status') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in trafficPolicies" :key="`${item.app}-${item.strategy}`">
            <td>{{ item.app }}</td>
            <td>{{ item.strategy }}</td>
            <td>{{ item.scope }}</td>
            <td>{{ item.ratio }}</td>
            <td>{{ item.owner }}</td>
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
