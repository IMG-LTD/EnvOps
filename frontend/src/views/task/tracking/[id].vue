<script setup lang="ts">
import { NAlert, NText, NTimeline, NTimelineItem } from 'naive-ui';
import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { fetchGetTaskCenterTaskTracking } from '@/service/api';

defineOptions({
  name: 'TaskTrackingPage'
});

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const loading = ref(false);
const trackingDetail = ref<Api.Task.TaskCenterTrackingDetail | null>(null);
const requestToken = ref(0);

const taskId = computed(() => {
  const id = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id;
  const parsed = Number(id);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
});

watch(
  taskId,
  value => {
    if (value) {
      void loadTrackingDetail(value);
    } else {
      trackingDetail.value = null;
    }
  },
  { immediate: true }
);

async function loadTrackingDetail(id: number) {
  const token = ++requestToken.value;
  loading.value = true;

  try {
    const { data, error } = await fetchGetTaskCenterTaskTracking(id);
    if (token !== requestToken.value) {
      return;
    }
    if (!error) {
      trackingDetail.value = data ?? null;
    }
  } finally {
    if (token === requestToken.value) {
      loading.value = false;
    }
  }
}

async function openRoute(routePath: string) {
  await router.push(routePath);
}
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.taskCenter.tracking.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.taskCenter.tracking.hero.description') }}</p>
        </div>
      </div>
    </NCard>

    <NSpin :show="loading">
      <NSpace v-if="trackingDetail" vertical :size="16">
        <NCard :title="t('page.envops.taskCenter.tracking.basicInfo.title')" :bordered="false" class="card-wrapper">
          <NDescriptions bordered :column="1">
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.taskName')">
              {{ trackingDetail.basicInfo.taskName || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.taskType')">
              {{ trackingDetail.basicInfo.taskType }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.triggeredBy')">
              {{ trackingDetail.basicInfo.triggeredBy || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.startedAt')">
              {{ trackingDetail.basicInfo.startedAt || '-' }}
            </NDescriptionsItem>
            <NDescriptionsItem :label="t('page.envops.taskCenter.drawer.finishedAt')">
              {{ trackingDetail.basicInfo.finishedAt || '-' }}
            </NDescriptionsItem>
          </NDescriptions>
          <NAlert v-if="trackingDetail.degraded" type="warning" class="mt-16px">
            {{ t('page.envops.taskCenter.tracking.degraded') }}
          </NAlert>
        </NCard>

        <NCard :title="t('page.envops.taskCenter.tracking.timeline.title')" :bordered="false" class="card-wrapper">
          <NTimeline>
            <NTimelineItem
              v-for="item in trackingDetail.timeline"
              :key="`${item.label}-${item.occurredAt || item.description || ''}`"
              :title="item.label"
              :content="item.description || '-'"
              :time="item.occurredAt || undefined"
            />
          </NTimeline>
        </NCard>

        <NCard :title="t('page.envops.taskCenter.tracking.logSummary.title')" :bordered="false" class="card-wrapper">
          <NText>{{ trackingDetail.logSummary || '-' }}</NText>
        </NCard>

        <NCard :title="t('page.envops.taskCenter.tracking.sourceLinks.title')" :bordered="false" class="card-wrapper">
          <NSpace>
            <NButton
              v-for="link in trackingDetail.sourceLinks"
              :key="`${link.type}-${link.route}`"
              secondary
              type="primary"
              @click="openRoute(link.route)"
            >
              {{ link.label }}
            </NButton>
          </NSpace>
        </NCard>
      </NSpace>
      <NEmpty v-else class="py-24px" :description="t('common.noData')" />
    </NSpin>
  </NSpace>
</template>

<style scoped></style>
