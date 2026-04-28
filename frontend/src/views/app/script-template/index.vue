<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { NButton, NCard, NDivider, NEmpty, NForm, NFormItem, NInput, NSelect, NSpace } from 'naive-ui';
import { useAuth } from '@/hooks/business/auth';
import {
  fetchCreateScriptTemplate,
  fetchDeleteScriptTemplate,
  fetchGetScriptTemplates,
  fetchUpdateScriptTemplate
} from '@/service/api';
import { $t } from '@/locales';
import { formatStatus, formatText, getScriptTypeOptions, getStatusOptions } from '../shared';

defineOptions({
  name: 'AppScriptTemplatePage'
});

const { hasAuth } = useAuth();

const loading = ref(false);
const submitting = ref(false);
const templates = ref<Api.App.ScriptTemplate[]>([]);
const formVisible = ref(false);
const editingTemplateId = ref<Api.App.RecordId | null>(null);

const canManageScriptTemplates = computed(() => hasAuth('app:script-template:manage'));
const scriptTypeOptions = computed(() => getScriptTypeOptions());
const statusOptions = computed(() => getStatusOptions());

const formModel = reactive<Api.App.CreateScriptTemplatePayload>(createDefaultFormModel());

function createDefaultFormModel(): Api.App.CreateScriptTemplatePayload {
  return {
    templateCode: '',
    templateName: '',
    scriptType: 'BASH',
    scriptContent: '',
    status: 1
  };
}

function resetFormModel() {
  Object.assign(formModel, createDefaultFormModel());
}

function buildPayload(): Api.App.CreateScriptTemplatePayload {
  return {
    templateCode: formModel.templateCode.trim(),
    templateName: formModel.templateName.trim(),
    scriptType: formModel.scriptType,
    scriptContent: formModel.scriptContent,
    status: formModel.status ?? null
  };
}

function fillFormModel(item: Api.App.ScriptTemplate) {
  Object.assign(formModel, {
    templateCode: item.templateCode,
    templateName: item.templateName,
    scriptType: item.scriptType,
    scriptContent: item.scriptContent,
    status: item.status ?? 1
  } satisfies Api.App.CreateScriptTemplatePayload);
}

async function loadTemplates() {
  loading.value = true;

  const { data, error } = await fetchGetScriptTemplates();

  if (!error && data) {
    templates.value = data;
  }

  loading.value = false;
}

function handleAdd() {
  if (!canManageScriptTemplates.value) {
    return;
  }

  editingTemplateId.value = null;
  resetFormModel();
  formVisible.value = true;
}

function handleEdit(item: Api.App.ScriptTemplate) {
  if (!canManageScriptTemplates.value) {
    return;
  }

  editingTemplateId.value = item.id;
  fillFormModel(item);
  formVisible.value = true;
}

function handleCancel() {
  editingTemplateId.value = null;
  resetFormModel();
  formVisible.value = false;
}

async function handleSubmit() {
  if (!canManageScriptTemplates.value) {
    return;
  }

  submitting.value = true;

  const payload = buildPayload();

  const response =
    editingTemplateId.value === null
      ? await fetchCreateScriptTemplate(payload)
      : await fetchUpdateScriptTemplate(editingTemplateId.value, payload);

  if (!response.error) {
    window.$message?.success($t(editingTemplateId.value === null ? 'common.addSuccess' : 'common.updateSuccess'));
    formVisible.value = false;
    editingTemplateId.value = null;
    resetFormModel();
    await loadTemplates();
  }

  submitting.value = false;
}

async function handleDelete(item: Api.App.ScriptTemplate) {
  if (!canManageScriptTemplates.value) {
    return;
  }

  window.$dialog?.warning({
    title: $t('common.warning'),
    content: `${$t('common.confirmDelete')} ${item.templateName}?`,
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    async onPositiveClick() {
      if (!canManageScriptTemplates.value) {
        return;
      }

      const { error } = await fetchDeleteScriptTemplate(item.id);

      if (!error) {
        window.$message?.success($t('common.deleteSuccess'));
        await loadTemplates();
      }
    }
  });
}

onMounted(() => {
  void loadTemplates();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper" :title="$t('route.app_script-template')">
      <NSpace justify="space-between" align="center" wrap>
        <span class="text-#666">{{ $t('page.app.scriptTemplate.desc') }}</span>
        <NSpace>
          <NButton type="primary" :disabled="!canManageScriptTemplates" @click="handleAdd">
            {{ $t('common.add') }}
          </NButton>
          <NButton :loading="loading" @click="loadTemplates()">
            {{ $t('common.refresh') }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NCard :bordered="false" class="card-wrapper" :title="$t('page.app.scriptTemplate.listTitle')">
      <div v-if="templates.length" class="flex-col-stretch gap-12px">
        <div v-for="item in templates" :key="item.id" class="rounded-12px bg-#f8f8f8 p-16px">
          <div class="flex items-start justify-between gap-12px">
            <div>
              <div class="text-16px font-semibold">{{ item.templateName }}</div>
              <div class="mt-8px text-12px text-#666">{{ item.templateCode }}</div>
            </div>
            <div class="flex gap-8px">
              <NButton text type="primary" :disabled="!canManageScriptTemplates" @click="handleEdit(item)">
                {{ $t('common.edit') }}
              </NButton>
              <NButton text type="error" :disabled="!canManageScriptTemplates" @click="handleDelete(item)">
                {{ $t('common.delete') }}
              </NButton>
            </div>
          </div>
          <div class="mt-12px grid gap-12px text-13px text-#666 md:grid-cols-2">
            <div>{{ $t('page.app.scriptTemplate.scriptType') }}: {{ formatText(item.scriptType) }}</div>
            <div>{{ $t('page.app.common.status') }}: {{ formatStatus(item.status) }}</div>
          </div>
          <div class="mt-12px whitespace-pre-wrap rounded-8px bg-white p-12px text-13px text-#333">
            {{ item.scriptContent }}
          </div>
        </div>
      </div>
      <NEmpty v-else :description="$t('common.noData')" />
    </NCard>

    <NCard
      v-if="formVisible"
      :bordered="false"
      class="card-wrapper"
      :title="
        editingTemplateId === null
          ? $t('page.app.scriptTemplate.formTitleCreate')
          : $t('page.app.scriptTemplate.formTitleEdit')
      "
    >
      <NForm label-placement="top">
        <div class="grid gap-16px md:grid-cols-2">
          <NFormItem :label="$t('page.app.scriptTemplate.templateCode')">
            <NInput v-model:value="formModel.templateCode" />
          </NFormItem>
          <NFormItem :label="$t('page.app.scriptTemplate.templateName')">
            <NInput v-model:value="formModel.templateName" />
          </NFormItem>
          <NFormItem :label="$t('page.app.scriptTemplate.scriptType')">
            <NSelect v-model:value="formModel.scriptType" :options="scriptTypeOptions" />
          </NFormItem>
          <NFormItem :label="$t('page.app.common.status')">
            <NSelect v-model:value="formModel.status" clearable :options="statusOptions" />
          </NFormItem>
        </div>
        <NFormItem :label="$t('page.app.scriptTemplate.scriptContent')">
          <NInput v-model:value="formModel.scriptContent" type="textarea" :autosize="{ minRows: 6, maxRows: 12 }" />
        </NFormItem>
      </NForm>

      <NDivider />

      <NSpace justify="end">
        <NButton @click="handleCancel">
          {{ $t('common.cancel') }}
        </NButton>
        <NButton type="primary" :loading="submitting" :disabled="!canManageScriptTemplates" @click="handleSubmit">
          {{ $t('page.app.common.save') }}
        </NButton>
      </NSpace>
    </NCard>
  </NSpace>
</template>
