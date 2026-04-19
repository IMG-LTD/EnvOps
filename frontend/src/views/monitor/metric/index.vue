<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { fetchGetAssetHosts, fetchGetMonitorHostFactsLatest } from '@/service/api';

defineOptions({
  name: 'MonitorMetricPage'
});

const route = useRoute();
const { t } = useI18n();

const loading = ref(false);
const requestToken = ref(0);
const defaultHostId = ref<number | null>(null);
const hostFacts = ref<Api.Monitor.HostFactRecord | null>(null);
const hostId = computed(() => normalizeHostId(route.query.hostId) ?? defaultHostId.value);

const summary = computed(() => [
  {
    key: 'cpuCores',
    label: t('page.envops.monitorMetric.summary.cpuCores.label'),
    value: String(hostFacts.value?.cpuCores ?? 0),
    desc: t('page.envops.monitorMetric.summary.cpuCores.desc')
  },
  {
    key: 'memory',
    label: t('page.envops.monitorMetric.summary.memory.label'),
    value: formatMemory(hostFacts.value?.memoryMb),
    desc: t('page.envops.monitorMetric.summary.memory.desc')
  },
  {
    key: 'osName',
    label: t('page.envops.monitorMetric.summary.osName.label'),
    value: hostFacts.value?.osName || '-',
    desc: t('page.envops.monitorMetric.summary.osName.desc')
  },
  {
    key: 'agentVersion',
    label: t('page.envops.monitorMetric.summary.agentVersion.label'),
    value: hostFacts.value?.agentVersion || '-',
    desc: t('page.envops.monitorMetric.summary.agentVersion.desc')
  }
]);

const detailRows = computed(() => [
  {
    key: 'hostName',
    label: t('page.envops.monitorMetric.detail.hostname'),
    value: hostFacts.value?.hostName || '-'
  },
  {
    key: 'osName',
    label: t('page.envops.monitorMetric.detail.osName'),
    value: hostFacts.value?.osName || '-'
  },
  {
    key: 'kernelVersion',
    label: t('page.envops.monitorMetric.detail.kernel'),
    value: hostFacts.value?.kernelVersion || '-'
  },
  {
    key: 'cpuCores',
    label: t('page.envops.monitorMetric.detail.cpuCores'),
    value: hostFacts.value?.cpuCores ?? '-'
  },
  {
    key: 'memoryMb',
    label: t('page.envops.monitorMetric.detail.memory'),
    value: formatMemory(hostFacts.value?.memoryMb)
  },
  {
    key: 'agentVersion',
    label: t('page.envops.monitorMetric.detail.agentVersion'),
    value: hostFacts.value?.agentVersion || '-'
  },
  {
    key: 'collectedAt',
    label: t('page.envops.monitorMetric.detail.collectedAt'),
    value: formatDateTime(hostFacts.value?.collectedAt)
  }
]);

async function loadHostFacts() {
  const currentRequestToken = ++requestToken.value;

  loading.value = true;

  if (defaultHostId.value === null) {
    const { data, error } = await fetchGetAssetHosts({ current: 1, size: 100 });

    if (currentRequestToken !== requestToken.value) {
      return;
    }

    if (!error) {
      const hostRecords = Array.isArray(data?.records) ? data.records : [];
      const firstHostWithFacts = hostRecords.find(item => item.hasMonitorFacts);
      defaultHostId.value = firstHostWithFacts?.id ?? null;
    }
  }

  const currentHostId = hostId.value;

  if (currentHostId === null) {
    hostFacts.value = null;
    loading.value = false;
    return;
  }

  const { data, error } = await fetchGetMonitorHostFactsLatest(currentHostId);

  if (currentRequestToken !== requestToken.value) {
    return;
  }

  if (!error) {
    hostFacts.value = data;
  } else {
    hostFacts.value = null;
  }

  loading.value = false;
}

function formatMemory(value?: number | null) {
  if (value === null || value === undefined) {
    return '-';
  }

  if (value >= 1024) {
    const memoryInGb = value / 1024;
    const fractionDigits = Number.isInteger(memoryInGb) ? 0 : 1;

    return `${memoryInGb.toFixed(fractionDigits)} GB`;
  }

  return `${value} MB`;
}

function normalizeHostId(value: unknown) {
  const normalizedValue = Array.isArray(value) ? value[0] : value;
  const numericValue = Number(normalizedValue);

  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    return null;
  }

  return numericValue;
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ').slice(0, 19);
}

watch(hostId, () => {
  void loadHostFacts();
});

onMounted(() => {
  void loadHostFacts();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.monitorMetric.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.monitorMetric.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="info">{{ t('page.envops.monitorMetric.tags.host', { id: hostId ?? '-' }) }}</NTag>
          <NButton secondary :loading="loading" @click="loadHostFacts">
            {{ t('common.refresh') }}
          </NButton>
        </NSpace>
      </div>
    </NCard>

    <NGrid cols="1 s:2 xl:4" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in summary" :key="item.key">
        <NCard :bordered="false" class="card-wrapper">
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NCard :title="t('page.envops.monitorMetric.detail.title')" :bordered="false" class="card-wrapper">
      <NSpin :show="loading">
        <NTable v-if="hostFacts" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.monitorMetric.detail.item') }}</th>
              <th>{{ t('page.envops.monitorMetric.detail.value') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detailRows" :key="item.key">
              <td>{{ item.label }}</td>
              <td>{{ item.value }}</td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />
      </NSpin>
    </NCard>
  </NSpace>
</template>

<style scoped></style>
