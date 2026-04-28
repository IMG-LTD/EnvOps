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
  NSpace,
  NUpload,
  type UploadFileInfo
} from 'naive-ui';
import { useAuth } from '@/hooks/business/auth';
import { createPackageUploadFormData, fetchDeletePackage, fetchGetPackages, fetchUploadPackage } from '@/service/api';
import { $t } from '@/locales';
import { formatFileSize, formatText, getPackageTypeOptions, getStorageTypeOptions } from '../shared';

defineOptions({
  name: 'AppPackagePage'
});

const { hasAuth } = useAuth();

const loading = ref(false);
const uploading = ref(false);
const packages = ref<Api.App.AppPackage[]>([]);
const fileList = ref<UploadFileInfo[]>([]);

const canManagePackages = computed(() => hasAuth('app:package:manage'));
const packageTypeOptions = computed(() => getPackageTypeOptions());
const storageTypeOptions = computed(() => getStorageTypeOptions());

const formModel = reactive<{
  packageName: string;
  packageType: Api.App.PackageType;
  storageType: Api.App.StorageType;
}>({
  packageName: '',
  packageType: 'JAR',
  storageType: 'LOCAL'
});

function handleBeforeUpload(options: { file: UploadFileInfo }) {
  if (!canManagePackages.value) {
    return false;
  }

  fileList.value = [options.file];
  return false;
}

function handleRemove() {
  fileList.value = [];
}

async function loadPackages() {
  loading.value = true;

  const { data, error } = await fetchGetPackages();

  if (!error && data) {
    packages.value = data;
  }

  loading.value = false;
}

async function handleUpload() {
  if (!canManagePackages.value) {
    return;
  }

  const selectedFile = fileList.value[0]?.file;

  if (!selectedFile) {
    window.$message?.warning($t('page.app.package.uploadFileRequired'));
    return;
  }

  uploading.value = true;

  const formData = createPackageUploadFormData({
    file: selectedFile,
    packageName: formModel.packageName.trim() || null,
    packageType: formModel.packageType,
    storageType: formModel.storageType
  });

  const { error } = await fetchUploadPackage(formData);

  if (!error) {
    window.$message?.success($t('common.addSuccess'));
    fileList.value = [];
    formModel.packageName = '';
    await loadPackages();
  }

  uploading.value = false;
}

async function handleDelete(item: Api.App.AppPackage) {
  if (!canManagePackages.value) {
    return;
  }

  window.$dialog?.warning({
    title: $t('common.warning'),
    content: `${$t('common.confirmDelete')} ${item.packageName}?`,
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    async onPositiveClick() {
      if (!canManagePackages.value) {
        return;
      }

      const { error } = await fetchDeletePackage(item.id);

      if (!error) {
        window.$message?.success($t('common.deleteSuccess'));
        await loadPackages();
      }
    }
  });
}

onMounted(() => {
  void loadPackages();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper" :title="$t('route.app_package')">
      <span class="text-#666">{{ $t('page.app.package.desc') }}</span>
    </NCard>

    <NCard :bordered="false" class="card-wrapper" :title="$t('page.app.package.uploadTitle')">
      <NForm label-placement="top">
        <div class="grid gap-16px md:grid-cols-3">
          <NFormItem :label="$t('page.app.package.packageName')">
            <NInput v-model:value="formModel.packageName" />
          </NFormItem>
          <NFormItem :label="$t('page.app.package.packageType')">
            <NSelect v-model:value="formModel.packageType" :options="packageTypeOptions" />
          </NFormItem>
          <NFormItem :label="$t('page.app.package.storageType')">
            <NSelect v-model:value="formModel.storageType" :options="storageTypeOptions" />
          </NFormItem>
        </div>

        <NFormItem :label="$t('page.app.package.file')">
          <NUpload
            :file-list="fileList"
            :default-upload="false"
            :max="1"
            :disabled="!canManagePackages"
            @before-upload="handleBeforeUpload"
            @remove="handleRemove"
          >
            <NButton :disabled="!canManagePackages">{{ $t('page.app.package.selectFile') }}</NButton>
          </NUpload>
        </NFormItem>
      </NForm>

      <NDivider />

      <NSpace justify="end">
        <NButton :loading="uploading" type="primary" :disabled="!canManagePackages" @click="handleUpload">
          {{ $t('page.app.package.uploadAction') }}
        </NButton>
      </NSpace>
    </NCard>

    <NCard :bordered="false" class="card-wrapper" :title="$t('page.app.package.listTitle')">
      <template #header-extra>
        <NButton :loading="loading" @click="loadPackages()">
          {{ $t('common.refresh') }}
        </NButton>
      </template>

      <div v-if="packages.length" class="flex-col-stretch gap-12px">
        <div v-for="item in packages" :key="item.id" class="rounded-12px bg-#f8f8f8 p-16px">
          <div class="flex items-start justify-between gap-12px">
            <div>
              <div class="text-16px font-semibold">{{ item.packageName }}</div>
              <div class="mt-8px text-12px text-#666">{{ item.packageType }}</div>
            </div>
            <NButton text type="error" :disabled="!canManagePackages" @click="handleDelete(item)">
              {{ $t('common.delete') }}
            </NButton>
          </div>
          <div class="mt-12px grid gap-12px text-13px text-#666 md:grid-cols-2">
            <div>{{ $t('page.app.package.filePath') }}: {{ formatText(item.filePath) }}</div>
            <div>{{ $t('page.app.package.fileSize') }}: {{ formatFileSize(item.fileSize) }}</div>
            <div>{{ $t('page.app.package.fileHash') }}: {{ formatText(item.fileHash) }}</div>
            <div>{{ $t('page.app.package.storageType') }}: {{ formatText(item.storageType) }}</div>
          </div>
        </div>
      </div>
      <NEmpty v-else :description="$t('common.noData')" />
    </NCard>
  </NSpace>
</template>
