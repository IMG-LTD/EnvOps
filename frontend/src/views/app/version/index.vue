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
  NSelect,
  NSpace
} from 'naive-ui';
import {
  fetchCreateAppVersion,
  fetchDeleteAppVersion,
  fetchGetApps,
  fetchGetAppVersions,
  fetchGetConfigTemplates,
  fetchGetPackages,
  fetchGetScriptTemplates,
  fetchUpdateAppVersion
} from '@/service/api';
import { $t } from '@/locales';
import { formatStatus, formatText, getRecordIdKey, getStatusOptions } from '../shared';

defineOptions({
  name: 'AppVersionPage'
});

const loading = ref(false);
const submitting = ref(false);
const apps = ref<Api.App.AppDefinition[]>([]);
const versions = ref<Api.App.AppVersion[]>([]);
const packages = ref<Api.App.AppPackage[]>([]);
const configTemplates = ref<Api.App.ConfigTemplate[]>([]);
const scriptTemplates = ref<Api.App.ScriptTemplate[]>([]);
const selectedAppId = ref<Api.App.RecordId | null>(null);
const formVisible = ref(false);
const editingVersionId = ref<Api.App.RecordId | null>(null);

const statusOptions = computed(() => getStatusOptions());

const packageOptions = computed(() => {
  return packages.value.map(item => ({
    label: item.packageName,
    value: item.id
  }));
});

const configTemplateOptions = computed(() => {
  return configTemplates.value.map(item => ({
    label: `${item.templateName} (${item.templateCode})`,
    value: item.id
  }));
});

const scriptTemplateOptions = computed(() => {
  return scriptTemplates.value.map(item => ({
    label: `${item.templateName} (${item.templateCode})`,
    value: item.id
  }));
});

const appOptions = computed(() => {
  return apps.value.map(item => ({
    label: `${item.appName} (${item.appCode})`,
    value: item.id
  }));
});

const currentApp = computed(() => {
  const key = getRecordIdKey(selectedAppId.value);

  return apps.value.find(item => getRecordIdKey(item.id) === key) ?? null;
});

const formModel = reactive<Api.App.CreateAppVersionPayload>(createDefaultFormModel());

function createDefaultFormModel(): Api.App.CreateAppVersionPayload {
  return {
    versionNo: '',
    packageId: null,
    configTemplateId: null,
    scriptTemplateId: null,
    changelog: '',
    status: 1
  };
}

function resetFormModel() {
  Object.assign(formModel, createDefaultFormModel());
}

function buildPayload(): Api.App.CreateAppVersionPayload {
  return {
    versionNo: formModel.versionNo.trim(),
    packageId: formModel.packageId ?? null,
    configTemplateId: formModel.configTemplateId ?? null,
    scriptTemplateId: formModel.scriptTemplateId ?? null,
    changelog: formModel.changelog?.trim() ? formModel.changelog.trim() : null,
    status: formModel.status ?? null
  };
}

function fillFormModel(version: Api.App.AppVersion) {
  Object.assign(formModel, {
    versionNo: version.versionNo,
    packageId: version.packageId ?? null,
    configTemplateId: version.configTemplateId ?? null,
    scriptTemplateId: version.scriptTemplateId ?? null,
    changelog: version.changelog ?? '',
    status: version.status ?? 1
  } satisfies Api.App.CreateAppVersionPayload);
}

async function loadSupportingResources() {
  const [packageResponse, configTemplateResponse, scriptTemplateResponse] = await Promise.all([
    fetchGetPackages(),
    fetchGetConfigTemplates(),
    fetchGetScriptTemplates()
  ]);

  if (!packageResponse.error && packageResponse.data) {
    packages.value = packageResponse.data;
  }

  if (!configTemplateResponse.error && configTemplateResponse.data) {
    configTemplates.value = configTemplateResponse.data;
  }

  if (!scriptTemplateResponse.error && scriptTemplateResponse.data) {
    scriptTemplates.value = scriptTemplateResponse.data;
  }
}

async function loadApps(preferredId?: Api.App.RecordId | null) {
  const { data, error } = await fetchGetApps();

  if (!error && data) {
    apps.value = data;

    const targetId = preferredId === undefined ? selectedAppId.value : preferredId;
    const targetKey = getRecordIdKey(targetId);
    const matchedApp = data.find(item => getRecordIdKey(item.id) === targetKey) ?? data[0] ?? null;

    if (matchedApp) {
      selectedAppId.value = matchedApp.id;
      await loadVersions(matchedApp.id);
    } else {
      selectedAppId.value = null;
      versions.value = [];
    }
  }
}

async function loadVersions(appId: Api.App.RecordId | null = selectedAppId.value) {
  if (appId === null) {
    versions.value = [];
    return;
  }

  loading.value = true;

  const { data, error } = await fetchGetAppVersions(appId);

  if (!error && data) {
    selectedAppId.value = appId;
    versions.value = data;
  }

  loading.value = false;
}

function handleAdd() {
  editingVersionId.value = null;
  resetFormModel();
  formVisible.value = true;
}

function handleEdit(version: Api.App.AppVersion) {
  editingVersionId.value = version.id;
  fillFormModel(version);
  formVisible.value = true;
}

function handleCancel() {
  editingVersionId.value = null;
  resetFormModel();
  formVisible.value = false;
}

async function handleSubmit() {
  if (selectedAppId.value === null) {
    return;
  }

  submitting.value = true;

  const payload = buildPayload();

  const response =
    editingVersionId.value === null
      ? await fetchCreateAppVersion(selectedAppId.value, payload)
      : await fetchUpdateAppVersion(editingVersionId.value, payload);

  if (!response.error) {
    window.$message?.success($t(editingVersionId.value === null ? 'common.addSuccess' : 'common.updateSuccess'));
    formVisible.value = false;
    editingVersionId.value = null;
    resetFormModel();
    await loadVersions(selectedAppId.value);
  }

  submitting.value = false;
}

async function handleDelete(version: Api.App.AppVersion) {
  window.$dialog?.warning({
    title: $t('common.warning'),
    content: `${$t('common.confirmDelete')} ${version.versionNo}?`,
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    async onPositiveClick() {
      const { error } = await fetchDeleteAppVersion(version.id);

      if (!error) {
        window.$message?.success($t('common.deleteSuccess'));
        await loadVersions(selectedAppId.value);
      }
    }
  });
}

onMounted(() => {
  void Promise.all([loadSupportingResources(), loadApps()]);
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper" :title="$t('route.app_version')">
      <NSpace justify="space-between" align="center" wrap>
        <span class="text-#666">{{ $t('page.app.version.desc') }}</span>
        <NSpace align="center" wrap>
          <NSelect
            v-model:value="selectedAppId"
            class="min-w-240px"
            clearable
            :placeholder="$t('page.app.version.selectApp')"
            :options="appOptions"
            @update:value="loadVersions"
          />
          <NButton type="primary" :disabled="selectedAppId === null" @click="handleAdd">
            {{ $t('common.add') }}
          </NButton>
          <NButton :loading="loading" @click="loadVersions()">
            {{ $t('common.refresh') }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NCard :bordered="false" class="card-wrapper" :title="$t('page.app.version.listTitle')">
      <template #header-extra>
        <span class="text-#666">
          {{ currentApp ? `${$t('page.app.version.currentApp')}: ${currentApp.appName}` : $t('common.noData') }}
        </span>
      </template>

      <div v-if="versions.length" class="flex-col-stretch gap-12px">
        <div v-for="item in versions" :key="item.id" class="rounded-12px bg-#f8f8f8 p-16px">
          <div class="flex items-start justify-between gap-12px">
            <div>
              <div class="text-16px font-semibold">{{ item.versionNo }}</div>
              <div class="mt-8px text-12px text-#666">
                {{ $t('page.app.common.status') }}: {{ formatStatus(item.status) }}
              </div>
            </div>
            <div class="flex gap-8px">
              <NButton text type="primary" @click="handleEdit(item)">
                {{ $t('common.edit') }}
              </NButton>
              <NButton text type="error" @click="handleDelete(item)">
                {{ $t('common.delete') }}
              </NButton>
            </div>
          </div>
          <div class="mt-12px grid gap-12px text-13px text-#666 md:grid-cols-3">
            <div>{{ $t('page.app.version.packageId') }}: {{ formatText(item.packageId) }}</div>
            <div>{{ $t('page.app.version.configTemplateId') }}: {{ formatText(item.configTemplateId) }}</div>
            <div>{{ $t('page.app.version.scriptTemplateId') }}: {{ formatText(item.scriptTemplateId) }}</div>
          </div>
          <div class="mt-12px text-13px text-#666">
            {{ $t('page.app.version.changelog') }}: {{ formatText(item.changelog) }}
          </div>
        </div>
      </div>
      <NEmpty v-else :description="$t('common.noData')" />
    </NCard>

    <NCard
      v-if="formVisible"
      :bordered="false"
      class="card-wrapper"
      :title="editingVersionId === null ? $t('page.app.version.formTitleCreate') : $t('page.app.version.formTitleEdit')"
    >
      <NForm label-placement="top">
        <NGrid responsive="screen" item-responsive :x-gap="16">
          <NGi span="24 m:12">
            <NFormItem :label="$t('page.app.version.versionNo')">
              <NInput v-model:value="formModel.versionNo" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:12">
            <NFormItem :label="$t('page.app.common.status')">
              <NSelect v-model:value="formModel.status" clearable :options="statusOptions" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:8">
            <NFormItem :label="$t('page.app.version.packageId')">
              <NSelect v-model:value="formModel.packageId" clearable :options="packageOptions" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:8">
            <NFormItem :label="$t('page.app.version.configTemplateId')">
              <NSelect v-model:value="formModel.configTemplateId" clearable :options="configTemplateOptions" />
            </NFormItem>
          </NGi>
          <NGi span="24 m:8">
            <NFormItem :label="$t('page.app.version.scriptTemplateId')">
              <NSelect v-model:value="formModel.scriptTemplateId" clearable :options="scriptTemplateOptions" />
            </NFormItem>
          </NGi>
          <NGi span="24">
            <NFormItem :label="$t('page.app.version.changelog')">
              <NInput v-model:value="formModel.changelog" type="textarea" :autosize="{ minRows: 3, maxRows: 6 }" />
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
