<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { fetchGetAssetGroups } from '@/service/api';

defineOptions({
  name: 'AssetGroupPage'
});

const { t } = useI18n();

const loading = ref(false);
const groups = ref<Api.Asset.GroupRecord[]>([]);

const totalHosts = computed(() => groups.value.reduce((sum, item) => sum + item.hostCount, 0));

async function loadGroups() {
  loading.value = true;

  const { data, error } = await fetchGetAssetGroups();

  if (!error) {
    groups.value = data;
  }

  loading.value = false;
}

onMounted(() => {
  void loadGroups();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.assetGroup.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.assetGroup.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="info">{{ t('page.envops.assetGroup.tags.groups', { count: groups.length }) }}</NTag>
          <NTag type="success">{{ t('page.envops.assetGroup.tags.hosts', { count: totalHosts }) }}</NTag>
        </NSpace>
      </div>
    </NCard>

    <NCard :bordered="false" class="card-wrapper" :title="t('page.envops.assetGroup.table.title')">
      <NSpin :show="loading">
        <NTable v-if="groups.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.assetGroup.table.group') }}</th>
              <th>{{ t('page.envops.assetGroup.table.description') }}</th>
              <th>{{ t('page.envops.assetGroup.table.hostCount') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in groups" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ item.description || '-' }}</td>
              <td>
                <NTag type="success" size="small">{{ item.hostCount }}</NTag>
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
