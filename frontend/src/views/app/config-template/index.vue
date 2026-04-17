<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NDivider,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSpace
} from 'naive-ui';
import {
  fetchCreateConfigTemplate,
  fetchDeleteConfigTemplate,
  fetchGetConfigTemplates,
  fetchUpdateConfigTemplate
} from '@/service/api';
import { $t } from '@/locales';
import { formatStatus, formatText, getRenderEngineOptions, getStatusOptions } from '../shared';

defineOptions({
  name: 'AppConfigTemplatePage'
});

const loading = ref(false);
const submitting = ref(false);
const templates = ref<Api.App.ConfigTemplate[]>([]);
const formVisible = ref(false);
const editingTemplateId = ref<Api.App.RecordId | null>(null);

const renderEngineOptions = computed(() => getRenderEngineOptions());
const statusOptions = computed(() => getStatusOptions());

const formModel = reactive<Api.App.CreateConfigTemplatePayload>(createDefaultFormModel());

function createDefaultFormModel(): Api.App.CreateConfigTemplatePayload {
  return {
    templateCode: '',
    templateName: '',
    templateContent: '',
    renderEngine: 'PLAINTEXT',
    status: 1
  };
}

function resetFormModel() {
  Object.assign(formModel, createDefaultFormModel());
}

function buildPayload(): Api.App.CreateConfigTemplatePayload {
  return {
    templateCode: formModel.templateCode.trim(),
    templateName: formModel.templateName.trim(),
    templateContent: formModel.templateContent,
    renderEngine: formModel.renderEngine ?? null,
    status: formModel.status ?? null
  };
}

function fillFormModel(item: Api.App.ConfigTemplate) {
  Object.assign(formModel, {
    templateCode: item.templateCode,
    templateName: item.templateName,
    templateContent: item.templateContent,
    renderEngine: item.renderEngine ?? 'PLAINTEXT',
    status: item.status ?? 1
  } satisfies Api.App.CreateConfigTemplatePayload);
}

async function loadTemplates() {
  loading.value = true;

  const { data, error } = await fetchGetConfigTemplates();

  if (!error && data) {
    templates.value = data;
  }

  loading.value = false;
}

function handleAdd() {
  editingTemplateId.value = null;
  resetFormModel();
  formVisible.value = true;
}

function handleEdit(item: Api.App.ConfigTemplate) {
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
  submitting.value = true;

  const payload = buildPayload();

  const response =
    editingTemplateId.value === null
      ? await fetchCreateConfigTemplate(payload)
      : await fetchUpdateConfigTemplate(editingTemplateId.value, payload);

  if (!response.error) {
    window.$message?.success($t(editingTemplateId.value === null ? 'common.addSuccess' : 'common.updateSuccess'));
    formVisible.value = false;
    editingTemplateId.value = null;
    resetFormModel();
    await loadTemplates();
  }

  submitting.value = false;
}

async function handleDelete(item: Api.App.ConfigTemplate) {
  window.$dialog?.warning({
    title: $t('common.warning'),
    content: `${$t('common.confirmDelete')} ${item.templateName}?`,
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    async onPositiveClick() {
      const { error } = await fetchDeleteConfigTemplate(item.id);

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
    <NCard :bordered="false" class="card-wrapper" :title="$t('route.app_config-template')">
      <NSpace justify="space-between" align="center" wrap>
        <span class="text-#666">{{ $t('page.app.configTemplate.desc') }}</span>
        <NSpace>
          <NButton type="primary" @click="handleAdd">
            {{ $t('common.add') }}
          </NButton>
          <NButton :loading="loading" @click="loadTemplates()">
            {{ $t('common.refresh') }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NCard :bordered="false" class="card-wrapper" :title="$t('page.app.configTemplate.listTitle')">
      <div v-if="templates.length" class="flex-col-stretch gap-12px">
        <div v-for="item in templates" :key="item.id" class="rounded-12px bg-#f8f8f8 p-16px">
          <div class="flex items-start justify-between gap-12px">
            <div>
              <div class="text-16px font-semibold">{{ item.templateName }}</div>
              <div class="mt-8px text-12px text-#666">{{ item.templateCode }}</div>
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
          <div class="mt-12px grid gap-12px text-13px text-#666 md:grid-cols-2">
            <div>{{ $t('page.app.configTemplate.renderEngine') }}: {{ formatText(item.renderEngine) }}</div>
            <div>{{ $t('page.app.common.status') }}: {{ formatStatus(item.status) }}</div>
          </div>
          <div class="mt-12px whitespace-pre-wrap rounded-8px bg-white p-12px text-13px text-#333">
            {{ item.templateContent }}
          </div>
        </div>
      </div>
      <NEmpty v-else :description="$t('common.noData')" />
    </NCard>

    <NCard
      v-if="formVisible"
      :bordered="false"
      class="card-wrapper"
      :title="editingTemplateId === null ? $t('page.app.configTemplate.formTitleCreate') : $t('page.app.configTemplate.formTitleEdit')"
    >
      <NForm label-placement="top">
        <div class="grid gap-16px md:grid-cols-2">
          <NFormItem :label="$t('page.app.configTemplate.templateCode')">
            <NInput v-model:value="formModel.templateCode" />
          </NFormItem>
          <NFormItem :label="$t('page.app.configTemplate.templateName')">
            <NInput v-model:value="formModel.templateName" />
          </NFormItem>
          <NFormItem :label="$t('page.app.configTemplate.renderEngine')">
            <NSelect v-model:value="formModel.renderEngine" :options="renderEngineOptions" />
          </NFormItem>
          <NFormItem :label="$t('page.app.common.status')">
            <NSelect v-model:value="formModel.status" clearable :options="statusOptions" />
          </NFormItem>
        </div>
        <NFormItem :label="$t('page.app.configTemplate.templateContent')">
          <NInput v-model:value="formModel.templateContent" type="textarea" :autosize="{ minRows: 6, maxRows: 12 }" />
        </NFormItem>
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
