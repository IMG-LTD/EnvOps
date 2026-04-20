<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  fetchGetTrafficPlugins,
  fetchGetTrafficPolicies,
  fetchPostApplyTrafficPolicy,
  fetchPostPreviewTrafficPolicy,
  fetchPostRollbackTrafficPolicy
} from '@/service/api';

defineOptions({
  name: 'TrafficControllerPage'
});

type TrafficPolicyStatusKey = 'enabled' | 'preview' | 'review' | 'standby' | 'disabled';
type TrafficPolicyTagType = 'success' | 'info' | 'warning' | 'default';
type TrafficStrategyKey = 'headerCanary' | 'blueGreen' | 'weightedRouting' | 'emergencyRollback';
type TrafficActionType = Api.Traffic.TrafficPolicyActionType;

const { t } = useI18n();

const loading = ref(false);
const requestToken = ref(0);
const actingPolicyId = ref<number | null>(null);
const trafficPolicyList = ref<Api.Traffic.TrafficPolicyRecord[]>([]);
const trafficPluginList = ref<Api.Traffic.TrafficPluginRecord[]>([]);
const latestActionResult = ref<Api.Traffic.TrafficPolicyActionRecord | null>(null);

const trafficPluginsByType = computed(() => {
  const directory = new Map<string, Api.Traffic.TrafficPluginRecord>();

  trafficPluginList.value.forEach(item => {
    directory.set(normalizeLookupValue(item.type), item);
  });

  return directory;
});

const hasNotReadyPlugin = computed(() =>
  trafficPluginList.value.some(item => normalizeLookupValue(item.status) !== 'ready')
);

const trafficPolicies = computed(() =>
  trafficPolicyList.value.map(item => {
    const statusKey = getTrafficPolicyStatusKey(item.status);
    const plugin = trafficPluginsByType.value.get(normalizeLookupValue(item.pluginType));
    const isPluginReady = normalizeLookupValue(plugin?.status) === 'ready';

    return {
      key: item.id,
      id: item.id,
      app: getDisplayText(item.app),
      strategy: getTrafficStrategyLabel(item.strategy),
      scope: getDisplayText(item.scope),
      ratio: getTrafficRatioLabel(item.trafficRatio),
      owner: getTrafficOwnerLabel(item.owner),
      status: getTrafficPolicyStatusLabel(statusKey),
      statusType: getTrafficPolicyTagType(statusKey),
      statusKey,
      canPreview: isPluginReady && Boolean(plugin?.supportsPreview),
      canApply: isPluginReady && Boolean(plugin?.supportsApply),
      canRollback:
        isPluginReady && Boolean(plugin?.supportsRollback) && Boolean(normalizeOptionalText(item.rollbackToken)),
      rollbackToken: normalizeOptionalText(item.rollbackToken)
    };
  })
);

const policyRecordCount = computed(() => trafficPolicies.value.length);
const previewRecordCount = computed(
  () => trafficPolicies.value.filter(item => item.statusKey === 'preview' || item.statusKey === 'review').length
);

const rollbackReadyRate = computed(() => {
  const totalPolicies = trafficPolicyList.value.length;

  if (!totalPolicies) {
    return '0%';
  }

  const rollbackReadyCount = trafficPolicyList.value.filter(item =>
    Boolean(normalizeOptionalText(item.rollbackToken))
  ).length;

  return `${Math.round((rollbackReadyCount / totalPolicies) * 100)}%`;
});

const metrics = computed(() => [
  {
    key: 'policiesEnabled',
    label: t('page.envops.trafficController.summary.policiesEnabled.label'),
    value: String(policyRecordCount.value),
    desc: t('page.envops.trafficController.summary.policiesEnabled.desc')
  },
  {
    key: 'canaryReleases',
    label: t('page.envops.trafficController.summary.canaryReleases.label'),
    value: String(previewRecordCount.value),
    desc: t('page.envops.trafficController.summary.canaryReleases.desc')
  },
  {
    key: 'rollbackReady',
    label: t('page.envops.trafficController.summary.rollbackReady.label'),
    value: rollbackReadyRate.value,
    desc: t('page.envops.trafficController.summary.rollbackReady.desc')
  }
]);

const policiesLiveTag = computed(
  () => `${policyRecordCount.value} / ${t('page.envops.trafficController.summary.policiesEnabled.label')}`
);

const previewRecordTag = computed(
  () => `${previewRecordCount.value} / ${t('page.envops.trafficController.summary.canaryReleases.label')}`
);

const latestActionSummary = computed(() => {
  const actionResult = latestActionResult.value;

  if (!actionResult) {
    return '';
  }

  return [getTrafficActionLabel(actionResult.action), actionResult.policy.app, actionResult.pluginResult.message]
    .filter(Boolean)
    .join(' · ');
});

async function loadTrafficData() {
  const currentRequestToken = ++requestToken.value;

  loading.value = true;

  try {
    const [trafficPoliciesResponse, trafficPluginsResponse] = await Promise.all([
      fetchGetTrafficPolicies(),
      fetchGetTrafficPlugins()
    ]);

    if (currentRequestToken !== requestToken.value) {
      return;
    }

    if (!trafficPoliciesResponse.error) {
      trafficPolicyList.value = getTrafficPolicyRecords(trafficPoliciesResponse.data);
    }

    if (!trafficPluginsResponse.error) {
      trafficPluginList.value = getTrafficPluginRecords(trafficPluginsResponse.data);
    }
  } finally {
    if (currentRequestToken === requestToken.value) {
      loading.value = false;
    }
  }
}

async function handlePolicyAction(policyId: number, action: TrafficActionType) {
  actingPolicyId.value = policyId;

  try {
    const actionRequestMap: Record<
      TrafficActionType,
      (id: number) => ReturnType<typeof fetchPostPreviewTrafficPolicy>
    > = {
      preview: fetchPostPreviewTrafficPolicy,
      apply: fetchPostApplyTrafficPolicy,
      rollback: fetchPostRollbackTrafficPolicy
    };

    const { data, error } = await actionRequestMap[action](policyId);

    if (!error) {
      latestActionResult.value = data;
      window.$message?.success(getTrafficActionSuccessMessage(action));
      await loadTrafficData();
    }
  } finally {
    actingPolicyId.value = null;
  }
}

function getTrafficPolicyRecords(data: Api.Traffic.TrafficPolicyListResponse) {
  return Array.isArray(data) ? data : [];
}

function getTrafficPluginRecords(data: Api.Traffic.TrafficPluginListResponse) {
  return Array.isArray(data) ? data : [];
}

function normalizeLookupValue(value?: string | null) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '_');
}

function normalizeOptionalText(value?: string | null) {
  if (typeof value !== 'string') {
    return null;
  }

  const normalizedText = value.trim();
  return normalizedText ? normalizedText : null;
}

function getDisplayText(value?: string | null) {
  if (typeof value === 'string' && value.trim()) {
    return value;
  }

  return '-';
}

function getTrafficRatioLabel(value?: string | null) {
  return getDisplayText(value);
}

function formatFallbackText(value?: string | null) {
  const text = getDisplayText(value);

  if (text === '-') {
    return text;
  }

  return text.replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim();
}

function getTrafficStrategyKey(value?: string | null): TrafficStrategyKey | null {
  const normalizedStrategy = normalizeLookupValue(value);

  if (normalizedStrategy === 'header_canary') {
    return 'headerCanary';
  }

  if (normalizedStrategy === 'blue_green') {
    return 'blueGreen';
  }

  if (normalizedStrategy === 'weighted_routing') {
    return 'weightedRouting';
  }

  if (normalizedStrategy === 'emergency_rollback') {
    return 'emergencyRollback';
  }

  return null;
}

function getTrafficStrategyLabel(value?: string | null) {
  const strategyKey = getTrafficStrategyKey(value);

  if (!strategyKey) {
    return formatFallbackText(value);
  }

  const labelMap: Record<TrafficStrategyKey, string> = {
    headerCanary: t('page.envops.common.strategy.headerCanary'),
    blueGreen: t('page.envops.common.strategy.blueGreen'),
    weightedRouting: t('page.envops.common.strategy.weightedRouting'),
    emergencyRollback: t('page.envops.common.strategy.emergencyRollback')
  };

  return labelMap[strategyKey];
}

function getTrafficOwnerLabel(value?: string | null) {
  const normalizedOwner = normalizeLookupValue(value);

  if (normalizedOwner.includes('traffic')) {
    return t('page.envops.common.team.traffic');
  }

  if (normalizedOwner.includes('release')) {
    return t('page.envops.common.team.release');
  }

  if (normalizedOwner.includes('platform')) {
    return t('page.envops.common.team.platform');
  }

  if (normalizedOwner.includes('sre')) {
    return t('page.envops.common.team.sre');
  }

  if (normalizedOwner.includes('envops')) {
    return t('page.envops.common.team.envops');
  }

  return formatFallbackText(value);
}

function getTrafficPolicyStatusKey(value?: string | null): TrafficPolicyStatusKey {
  const normalizedStatus = normalizeLookupValue(value);

  if (normalizedStatus.includes('enable')) {
    return 'enabled';
  }

  if (normalizedStatus.includes('preview')) {
    return 'preview';
  }

  if (normalizedStatus.includes('review')) {
    return 'review';
  }

  if (normalizedStatus.includes('standby') || normalizedStatus.includes('pending')) {
    return 'standby';
  }

  if (normalizedStatus.includes('disable') || normalizedStatus.includes('offline')) {
    return 'disabled';
  }

  return 'standby';
}

function getTrafficPolicyStatusLabel(statusKey: TrafficPolicyStatusKey) {
  const labelMap: Record<TrafficPolicyStatusKey, string> = {
    enabled: t('page.envops.common.status.enabled'),
    preview: t('page.envops.common.status.preview'),
    review: t('page.envops.common.status.review'),
    standby: t('page.envops.common.status.standby'),
    disabled: t('page.envops.common.status.disabled')
  };

  return labelMap[statusKey];
}

function getTrafficPolicyTagType(statusKey: TrafficPolicyStatusKey): TrafficPolicyTagType {
  const typeMap: Record<TrafficPolicyStatusKey, TrafficPolicyTagType> = {
    enabled: 'success',
    preview: 'info',
    review: 'warning',
    standby: 'default',
    disabled: 'default'
  };

  return typeMap[statusKey];
}

function getTrafficActionLabel(action: TrafficActionType) {
  const actionLabelMap: Record<TrafficActionType, string> = {
    preview: t('page.envops.trafficController.actions.preview'),
    apply: t('page.envops.trafficController.actions.apply'),
    rollback: t('page.envops.trafficController.actions.rollback')
  };

  return actionLabelMap[action];
}

function getTrafficActionSuccessMessage(action: TrafficActionType) {
  const actionMessageMap: Record<TrafficActionType, string> = {
    preview: t('page.envops.trafficController.messages.previewSuccess'),
    apply: t('page.envops.trafficController.messages.applySuccess'),
    rollback: t('page.envops.trafficController.messages.rollbackSuccess')
  };

  return actionMessageMap[action];
}

onMounted(() => {
  void loadTrafficData();
});
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
          <NTag type="success">{{ policiesLiveTag }}</NTag>
          <NTag type="info">{{ previewRecordTag }}</NTag>
          <NButton secondary :loading="loading" @click="loadTrafficData">
            {{ t('common.refresh') }}
          </NButton>
        </NSpace>
      </div>
    </NCard>

    <NAlert v-if="hasNotReadyPlugin" type="warning" :show-icon="false">
      {{ t('page.envops.trafficController.messages.notReadyWarning') }}
    </NAlert>

    <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in metrics" :key="item.key">
        <NCard :bordered="false" class="card-wrapper" :data-summary-key="item.key">
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NAlert v-if="latestActionResult" type="success" :show-icon="false">
      <div class="font-medium">{{ t('page.envops.trafficController.messages.latestAction') }}</div>
      <div class="mt-8px">{{ latestActionSummary }}</div>
    </NAlert>

    <NCard :title="t('page.envops.trafficController.table.title')" :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <NTable v-if="trafficPolicies.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.trafficController.table.application') }}</th>
              <th>{{ t('page.envops.trafficController.table.strategy') }}</th>
              <th>{{ t('page.envops.trafficController.table.scope') }}</th>
              <th>{{ t('page.envops.trafficController.table.trafficRatio') }}</th>
              <th>{{ t('page.envops.trafficController.table.owner') }}</th>
              <th>{{ t('page.envops.trafficController.table.status') }}</th>
              <th>{{ t('page.envops.trafficController.table.operation') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in trafficPolicies" :key="item.key">
              <td>{{ item.app }}</td>
              <td>{{ item.strategy }}</td>
              <td>{{ item.scope }}</td>
              <td>{{ item.ratio }}</td>
              <td>{{ item.owner }}</td>
              <td>
                <NTag :type="item.statusType" size="small">{{ item.status }}</NTag>
              </td>
              <td>
                <NSpace size="small">
                  <NButton
                    text
                    size="small"
                    :disabled="!item.canPreview"
                    :loading="actingPolicyId === item.id"
                    @click="handlePolicyAction(item.id, 'preview')"
                  >
                    {{ t('page.envops.trafficController.actions.preview') }}
                  </NButton>
                  <NButton
                    text
                    size="small"
                    :disabled="!item.canApply"
                    :loading="actingPolicyId === item.id"
                    @click="handlePolicyAction(item.id, 'apply')"
                  >
                    {{ t('page.envops.trafficController.actions.apply') }}
                  </NButton>
                  <NButton
                    text
                    size="small"
                    :disabled="!item.canRollback"
                    :loading="actingPolicyId === item.id"
                    @click="handlePolicyAction(item.id, 'rollback')"
                  >
                    {{ t('page.envops.trafficController.actions.rollback') }}
                  </NButton>
                </NSpace>
              </td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />
      </NSpin>
    </NCard>
  </NSpace>
</template>

<style scoped></style>
