<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NDivider,
  NEmpty,
  NForm,
  NFormItem,
  NGrid,
  NGi,
  NInput,
  NInputNumber,
  NSelect,
  NSpace
} from 'naive-ui';
import { fetchCreateApp, fetchDeleteApp, fetchGetAppDetail, fetchGetApps, fetchUpdateApp } from '@/service/api';
import { $t } from '@/locales';
import {
  formatStatus,
  formatText,
  getAppTypeOptions,
  getDeployModeOptions,
  getRecordIdKey,
  getStatusOptions
} from '../shared';

defineOptions({
  name: 'AppDefinitionPage'
});

const loading = ref(false);
const submitting = ref(false);
const apps = ref<Api.App.AppDefinition[]>([]);
const activeAppId = ref<string | null>(null);
const activeApp = ref<Api.App.AppDefinition | null>(null);
const formVisible = ref(false);
const editingAppId = ref<Api.App.RecordId | null>(null);

const appTypeOptions = computed(() => getAppTypeOptions());
const deployModeOptions = computed(() => getDeployModeOptions());
const statusOptions = computed(() => getStatusOptions());

const formModel = reactive<Api.App.CreateAppPayload>(createDefaultFormModel());

function createDefaultFormModel(): Api.App.CreateAppPayload {
  return {
    appCode: '',
    appName: '',
    appType: 'JAVA',
    runtimeType: '',
    deployMode: 'SYSTEMD',
    defaultPort: null,
    healthCheckPath: '',
    description: '',
    status: 1
  };
}

function resetFormModel() {
  Object.assign(formModel, createDefaultFormModel());
}

function normalizeOptionalText(value: string | null | undefined) {
  const trimmed = value?.trim() ?? '';

  return trimmed ? trimmed : null;
}

function buildPayload(): Api.App.CreateAppPayload {
  return {
    appCode: formModel.appCode.trim(),
    appName: formModel.appName.trim(),
    appType: formModel.appType,
    runtimeType: normalizeOptionalText(formModel.runtimeType),
    deployMode: formModel.deployMode ?? null,
    defaultPort: formModel.defaultPort ?? null,
    healthCheckPath: normalizeOptionalText(formModel.healthCheckPath),
    description: normalizeOptionalText(formModel.description),
    status: formModel.status ?? null
  };
}

function fillFormModel(app: Api.App.AppDefinition) {
  Object.assign(formModel, {
    appCode: app.appCode,
    appName: app.appName,
    appType: app.appType,
    runtimeType: app.runtimeType ?? '',
    deployMode: app.deployMode ?? null,
    defaultPort: app.defaultPort ?? null,
    healthCheckPath: app.healthCheckPath ?? '',
    description: app.description ?? '',
    status: app.status ?? 1
  } satisfies Api.App.CreateAppPayload);
}

function isActive(id: Api.App.RecordId) {
  return activeAppId.value === getRecordIdKey(id);
}

async function loadAppDetail(id: Api.App.RecordId) {
  const { data, error } = await fetchGetAppDetail(id);

  if (!error && data) {
    activeApp.value = data;
    activeAppId.value = getRecordIdKey(data.id);
  }
}

async function loadApps(preferredId?: Api.App.RecordId | null) {
  loading.value = true;

  const { data, error } = await fetchGetApps();

  if (!error && data) {
    apps.value = data;

    const targetId = preferredId === undefined ? activeAppId.value : getRecordIdKey(preferredId);
    const matchedApp = data.find(item => getRecordIdKey(item.id) === targetId) ?? data[0] ?? null;

    if (matchedApp) {
      await loadAppDetail(matchedApp.id);
    } else {
      activeAppId.value = null;
      activeApp.value = null;
    }
  }

  loading.value = false;
}

function handleAdd() {
  editingAppId.value = null;
  resetFormModel();
  formVisible.value = true;
}

function handleEdit(app: Api.App.AppDefinition) {
  editingAppId.value = app.id;
  fillFormModel(app);
  formVisible.value = true;
}

function handleCancel() {
  formVisible.value = false;
  editingAppId.value = null;
  resetFormModel();
}

async function handleSubmit() {
  submitting.value = true;

  const payload = buildPayload();

  const response =
    editingAppId.value === null ? await fetchCreateApp(payload) : await fetchUpdateApp(editingAppId.value, payload);

  if (!response.error && response.data) {
    window.$message?.success($t(editingAppId.value === null ? 'common.addSuccess' : 'common.updateSuccess'));
    formVisible.value = false;
    editingAppId.value = null;
    resetFormModel();
    await loadApps(response.data.id);
  }

  submitting.value = false;
}

async function handleDelete(app: Api.App.AppDefinition) {
  window.$dialog?.warning({
    title: $t('common.warning'),
    content: `${$t('common.confirmDelete')} ${app.appName}?`,
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    async onPositiveClick() {
      const { error } = await fetchDeleteApp(app.id);

      if (!error) {
        window.$message?.success($t('common.deleteSuccess'));
        await loadApps(activeAppId.value === getRecordIdKey(app.id) ? null : activeAppId.value);
      }
    }
  });
}

onMounted(() => {
  void loadApps();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper" :title="$t('route.app_definition')">
      <NSpace justify="space-between" align="center" wrap>
        <span class="text-#666">{{ $t('page.app.definition.desc') }}</span>
        <NSpace>
          <NButton type="primary" @click="handleAdd">
            {{ $t('common.add') }}
          </NButton>
          <NButton :loading="loading" @click="loadApps()">
            {{ $t('common.refresh') }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NGrid responsive="screen" item-responsive :x-gap="16" :y-gap="16">
      <NGi span="24 m:11 l:10">
        <NCard :bordered="false" class="card-wrapper" :title="$t('page.app.definition.listTitle')">
          <div v-if="apps.length" class="flex-col-stretch gap-12px">
            <div
              v-for="item in apps"
              :key="item.id"
              class="cursor-pointer rounded-12px border border-solid p-16px transition-colors"
              :class="isActive(item.id) ? 'border-primary text-primary' : 'border-transparent bg-#f8f8f8 text-#333'"
              @click="loadAppDetail(item.id)"
            >
              <div class="flex items-start justify-between gap-12px">
                <div>
                  <div class="text-16px font-semibold">{{ item.appName }}</div>
                  <div class="mt-8px text-13px text-#666">{{ item.appCode }}</div>
                </div>
                <div class="text-right text-12px">
                  <div>{{ formatText(item.appType) }}</div>
                  <div class="mt-6px">{{ formatText(item.deployMode) }}</div>
                </div>
              </div>
              <div class="mt-12px flex flex-wrap items-center justify-between gap-12px text-12px text-#666">
                <span>{{ $t('page.app.common.defaultPort') }}: {{ formatText(item.defaultPort) }}</span>
                <span>{{ $t('page.app.common.status') }}: {{ formatStatus(item.status) }}</span>
              </div>
              <div class="mt-12px flex justify-end gap-8px">
                <NButton text type="primary" @click.stop="handleEdit(item)">
                  {{ $t('common.edit') }}
                </NButton>
                <NButton text type="error" @click.stop="handleDelete(item)">
                  {{ $t('common.delete') }}
                </NButton>
              </div>
            </div>
          </div>
          <NEmpty v-else :description="$t('common.noData')" />
        </NCard>
      </NGi>

      <NGi span="24 m:13 l:14">
        <NCard :bordered="false" class="card-wrapper" :title="$t('page.app.definition.detailTitle')">
          <template v-if="activeApp">
            <div class="grid gap-12px text-14px text-#333">
              <div>
                <div class="text-#999">{{ $t('page.app.common.appName') }}</div>
                <div class="mt-4px font-medium">{{ activeApp.appName }}</div>
              </div>
              <div>
                <div class="text-#999">{{ $t('page.app.common.appCode') }}</div>
                <div class="mt-4px">{{ activeApp.appCode }}</div>
              </div>
              <div class="grid gap-12px md:grid-cols-2">
                <div>
                  <div class="text-#999">{{ $t('page.app.common.appType') }}</div>
                  <div class="mt-4px">{{ formatText(activeApp.appType) }}</div>
                </div>
                <div>
                  <div class="text-#999">{{ $t('page.app.common.deployMode') }}</div>
                  <div class="mt-4px">{{ formatText(activeApp.deployMode) }}</div>
                </div>
                <div>
                  <div class="text-#999">{{ $t('page.app.common.runtimeType') }}</div>
                  <div class="mt-4px">{{ formatText(activeApp.runtimeType) }}</div>
                </div>
                <div>
                  <div class="text-#999">{{ $t('page.app.common.defaultPort') }}</div>
                  <div class="mt-4px">{{ formatText(activeApp.defaultPort) }}</div>
                </div>
              </div>
              <div>
                <div class="text-#999">{{ $t('page.app.common.healthCheckPath') }}</div>
                <div class="mt-4px break-all">{{ formatText(activeApp.healthCheckPath) }}</div>
              </div>
              <div>
                <div class="text-#999">{{ $t('page.app.common.description') }}</div>
                <div class="mt-4px whitespace-pre-wrap">{{ formatText(activeApp.description) }}</div>
              </div>
              <div class="grid gap-12px md:grid-cols-2">
                <div>
                  <div class="text-#999">{{ $t('page.app.common.status') }}</div>
                  <div class="mt-4px">{{ formatStatus(activeApp.status) }}</div>
                </div>
                <div>
                  <div class="text-#999">{{ $t('page.app.common.updatedAt') }}</div>
                  <div class="mt-4px">{{ formatText(activeApp.updateTime) }}</div>
                </div>
              </div>
            </div>
          </template>
          <NEmpty v-else :description="$t('common.noData')" />
        </NCard>
      </NGi>
    </NGrid>

    <NCard
      v-if="formVisible"
      :bordered="false"
      class="card-wrapper"
      :title="
        editingAppId === null ? $t('page.app.definition.formTitleCreate') : $t('page.app.definition.formTitleEdit')
      "
    >
      <NForm label-placement="top">
        <NGrid responsive="screen" item-responsive :x-gap="16">
          <NGi span="24 m:12">
            <NFormItem :label="$t('page.app.common.appCode')">
              <NInput v-model:value="formModel.appCode" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:12">
            <NFormItem :label="$t('page.app.common.appName')">
              <NInput v-model:value="formModel.appName" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:8">
            <NFormItem :label="$t('page.app.common.appType')">
              <NSelect v-model:value="formModel.appType" :options="appTypeOptions" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:8">
            <NFormItem :label="$t('page.app.common.deployMode')">
              <NSelect v-model:value="formModel.deployMode" clearable :options="deployModeOptions" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:8">
            <NFormItem :label="$t('page.app.common.defaultPort')">
              <NInputNumber v-model:value="formModel.defaultPort" clearable class="w-full" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:12">
            <NFormItem :label="$t('page.app.common.runtimeType')">
              <NInput v-model:value="formModel.runtimeType" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:12">
            <NFormItem :label="$t('page.app.common.healthCheckPath')">
              <NInput v-model:value="formModel.healthCheckPath" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:12">
            <NFormItem :label="$t('page.app.common.status')">
              <NSelect v-model:value="formModel.status" clearable :options="statusOptions" />
            </NFormItem>
          </NGi>
          <NGi span="24">
            <NFormItem :label="$t('page.app.common.description')">
              <NInput v-model:value="formModel.description" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" />
            </NFormItem>
          </NGi>
        </NGrid>
      </NForm>

      <NDivider />

      <NSpace justify="end">
        <NButton @click="handleCancel">
          {{ $t('common.cancel') }}
        </NButton>
        <NButton type="primary" :loading="submitting" @click="handleSubmit">
          {{ $t('page.app.common.save') }}
        </NButton>
      </NSpace>
    </NCard>
  </NSpace>
</template>
