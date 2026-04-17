<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { fetchGetAssetTags } from '@/service/api';

defineOptions({
  name: 'AssetTagPage'
});

const { t } = useI18n();

const loading = ref(false);
const tags = ref<Api.Asset.TagRecord[]>([]);

async function loadTags() {
  loading.value = true;

  const { data, error } = await fetchGetAssetTags();

  if (!error) {
    tags.value = data;
  }

  loading.value = false;
}

onMounted(() => {
  void loadTags();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.assetTag.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.assetTag.hero.description') }}</p>
        </div>
        <NTag type="info">{{ t('page.envops.assetTag.tags.total', { count: tags.length }) }}</NTag>
      </div>
    </NCard>

    <NCard :bordered="false" class="card-wrapper" :title="t('page.envops.assetTag.table.title')">
      <NSpin :show="loading">
        <NTable v-if="tags.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.assetTag.table.tag') }}</th>
              <th>{{ t('page.envops.assetTag.table.color') }}</th>
              <th>{{ t('page.envops.assetTag.table.description') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in tags" :key="item.id">
              <td>{{ item.name }}</td>
              <td>
                <div class="flex items-center gap-8px">
                  <span class="inline-block h-12px w-12px rounded-full" :style="{ backgroundColor: item.color || '#d9d9d9' }" />
                  <span>{{ item.color || '-' }}</span>
                </div>
              </td>
              <td>{{ item.description || '-' }}</td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />
      </NSpin>
    </NCard>
  </NSpace>
</template>

<style scoped></style>
