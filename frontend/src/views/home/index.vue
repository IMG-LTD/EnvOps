<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useAppStore } from '@/store/modules/app';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({
  name: 'HomePage'
});

const { t } = useI18n();
const appStore = useAppStore();
const authStore = useAuthStore();

const gap = computed(() => (appStore.isMobile ? 0 : 16));

const heroDescription = computed(() => {
  if (authStore.userInfo.userName) {
    return t('page.envops.home.hero.descriptionWithUser', { userName: authStore.userInfo.userName });
  }

  return t('page.envops.home.hero.description');
});

const summaryCards = computed(() => [
  {
    key: 'managedApplications',
    label: t('page.envops.home.summary.managedApplications.label'),
    value: '37',
    desc: t('page.envops.home.summary.managedApplications.desc')
  },
  {
    key: 'onlineHosts',
    label: t('page.envops.home.summary.onlineHosts.label'),
    value: '142',
    desc: t('page.envops.home.summary.onlineHosts.desc')
  },
  {
    key: 'runningTasks',
    label: t('page.envops.home.summary.runningTasks.label'),
    value: '6',
    desc: t('page.envops.home.summary.runningTasks.desc')
  },
  {
    key: 'trafficPolicies',
    label: t('page.envops.home.summary.trafficPolicies.label'),
    value: '14',
    desc: t('page.envops.home.summary.trafficPolicies.desc')
  }
]);

const deliveryItems = computed(() => [
  {
    taskId: 'DEP-20260415-001',
    app: 'payment-gateway',
    env: t('page.envops.common.environment.production'),
    batch: t('page.envops.common.batch.canary20'),
    owner: t('page.envops.common.owner.envops'),
    status: t('page.envops.common.status.running'),
    statusType: 'warning' as const
  },
  {
    taskId: 'DEP-20260415-002',
    app: 'traffic-admin',
    env: t('page.envops.common.environment.staging'),
    batch: t('page.envops.common.batch.fullRelease'),
    owner: t('page.envops.common.owner.release'),
    status: t('page.envops.common.status.pendingApproval'),
    statusType: 'info' as const
  },
  {
    taskId: 'DEP-20260415-003',
    app: 'asset-sync',
    env: t('page.envops.common.environment.production'),
    batch: t('page.envops.common.batch.canary10'),
    owner: t('page.envops.common.owner.sre'),
    status: t('page.envops.common.status.rollbackRequired'),
    statusType: 'error' as const
  }
]);

const inspectionItems = computed(() => [
  {
    task: 'node-baseline-check',
    target: 'prd-k8s-cluster',
    schedule: t('page.envops.common.schedule.every10Min'),
    lastResult: t('page.envops.common.status.success'),
    resultType: 'success' as const
  },
  {
    task: 'nginx-config-diff',
    target: 'edge-gateway',
    schedule: t('page.envops.common.schedule.everyHour'),
    lastResult: t('page.envops.common.status.warning'),
    resultType: 'warning' as const
  },
  {
    task: 'traffic-canary-guard',
    target: 'traffic-controller',
    schedule: t('page.envops.common.schedule.every15Min'),
    lastResult: t('page.envops.common.status.timeout'),
    resultType: 'error' as const
  }
]);

const focusList = computed(() => [
  {
    key: 'item1',
    title: t('page.envops.home.focusList.item1')
  },
  {
    key: 'item2',
    title: t('page.envops.home.focusList.item2')
  },
  {
    key: 'item3',
    title: t('page.envops.home.focusList.item3')
  },
  {
    key: 'item4',
    title: t('page.envops.home.focusList.item4')
  }
]);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-16px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 class="text-24px font-semibold">{{ t('page.envops.home.hero.title') }}</h2>
          <p class="mt-8px text-14px text-#666">{{ heroDescription }}</p>
        </div>
        <NSpace>
          <NTag type="success">{{ t('page.envops.home.tags.dynamicRoutesReady') }}</NTag>
          <NTag type="info">{{ t('page.envops.home.tags.vitestHarnessEnabled') }}</NTag>
        </NSpace>
      </div>
    </NCard>

    <NGrid :x-gap="gap" :y-gap="16" cols="1 s:2 l:4" responsive="screen">
      <NGi v-for="item in summaryCards" :key="item.key">
        <NCard :bordered="false" class="card-wrapper">
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NGrid :x-gap="gap" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 s:24 m:14">
        <NCard :title="t('page.envops.home.sections.releasePipelineFocus')" :bordered="false" class="card-wrapper">
          <NTable :bordered="false" :single-line="false">
            <thead>
              <tr>
                <th>{{ t('page.envops.home.releaseTable.taskId') }}</th>
                <th>{{ t('page.envops.home.releaseTable.application') }}</th>
                <th>{{ t('page.envops.home.releaseTable.environment') }}</th>
                <th>{{ t('page.envops.home.releaseTable.batch') }}</th>
                <th>{{ t('page.envops.home.releaseTable.owner') }}</th>
                <th>{{ t('page.envops.home.releaseTable.status') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in deliveryItems" :key="item.taskId">
                <td>{{ item.taskId }}</td>
                <td>{{ item.app }}</td>
                <td>{{ item.env }}</td>
                <td>{{ item.batch }}</td>
                <td>{{ item.owner }}</td>
                <td>
                  <NTag :type="item.statusType" size="small">{{ item.status }}</NTag>
                </td>
              </tr>
            </tbody>
          </NTable>
        </NCard>
      </NGi>
      <NGi span="24 s:24 m:10">
        <NCard :title="t('page.envops.home.sections.operatorFocus')" :bordered="false" class="card-wrapper">
          <NList>
            <NListItem v-for="item in focusList" :key="item.key">
              <NThing :title="item.title" :description="t('page.envops.home.operatorFocusDesc')" />
            </NListItem>
          </NList>
        </NCard>
      </NGi>
    </NGrid>

    <NGrid :x-gap="gap" :y-gap="16" responsive="screen" item-responsive>
      <NGi span="24 s:24 m:12">
        <NCard :title="t('page.envops.home.sections.inspectionHealth')" :bordered="false" class="card-wrapper">
          <NTable :bordered="false" :single-line="false">
            <thead>
              <tr>
                <th>{{ t('page.envops.home.inspectionTable.task') }}</th>
                <th>{{ t('page.envops.home.inspectionTable.target') }}</th>
                <th>{{ t('page.envops.home.inspectionTable.schedule') }}</th>
                <th>{{ t('page.envops.home.inspectionTable.lastResult') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in inspectionItems" :key="item.task">
                <td>{{ item.task }}</td>
                <td>{{ item.target }}</td>
                <td>{{ item.schedule }}</td>
                <td>
                  <NTag :type="item.resultType" size="small">{{ item.lastResult }}</NTag>
                </td>
              </tr>
            </tbody>
          </NTable>
        </NCard>
      </NGi>
      <NGi span="24 s:24 m:12">
        <NCard :title="t('page.envops.home.sections.platformReadiness')" :bordered="false" class="card-wrapper">
          <NDescriptions :column="1" label-placement="left" bordered>
            <NDescriptionsItem :label="t('page.envops.home.readiness.frontendShell.label')">
              {{ t('page.envops.home.readiness.frontendShell.value') }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.home.readiness.routingMode.label')">
              {{ t('page.envops.home.readiness.routingMode.value') }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.home.readiness.qualityGate.label')">
              {{ t('page.envops.home.readiness.qualityGate.value') }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.home.readiness.nextSlice.label')">
              {{ t('page.envops.home.readiness.nextSlice.value') }}
            </NDescriptionsItem>
          </NDescriptions>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

<style scoped></style>
