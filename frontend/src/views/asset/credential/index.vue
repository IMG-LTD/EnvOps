<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { fetchCreateAssetCredential, fetchGetAssetCredentials } from '@/service/api';

defineOptions({
  name: 'AssetCredentialPage'
});

const { t } = useI18n();

const loading = ref(false);
const creating = ref(false);
const credentials = ref<Api.Asset.CredentialRecord[]>([]);
const formModel = reactive<Api.Asset.CreateCredentialParams>({
  name: '',
  credentialType: 'ssh_password',
  username: '',
  secret: '',
  description: ''
});

const credentialTypeOptions = computed(() => [
  {
    label: t('page.envops.assetCredential.types.sshPassword'),
    value: 'ssh_password'
  },
  {
    label: t('page.envops.assetCredential.types.sshKey'),
    value: 'ssh_key'
  },
  {
    label: t('page.envops.assetCredential.types.apiToken'),
    value: 'api_token'
  }
]);

const credentialSummary = computed(() => ({
  total: credentials.value.length,
  sshPassword: credentials.value.filter(item => item.credentialType === 'ssh_password').length,
  sshKey: credentials.value.filter(item => item.credentialType === 'ssh_key').length,
  apiToken: credentials.value.filter(item => item.credentialType === 'api_token').length
}));

function getCredentialTypeLabel(type: string) {
  const labelMap: Record<string, string> = {
    ssh_password: t('page.envops.assetCredential.types.sshPassword'),
    ssh_key: t('page.envops.assetCredential.types.sshKey'),
    api_token: t('page.envops.assetCredential.types.apiToken')
  };

  return labelMap[type] || type;
}

async function loadCredentials() {
  loading.value = true;

  const { data, error } = await fetchGetAssetCredentials();

  if (!error) {
    credentials.value = data;
  }

  loading.value = false;
}

async function handleCreateCredential() {
  if (!formModel.name?.trim() || !formModel.credentialType?.trim()) {
    window.$message?.warning(t('page.envops.assetCredential.messages.fillNameAndType'));
    return;
  }

  creating.value = true;

  const payload: Api.Asset.CreateCredentialParams = {
    name: formModel.name.trim(),
    credentialType: formModel.credentialType,
    username: formModel.username?.trim(),
    secret: formModel.secret?.trim(),
    description: formModel.description?.trim()
  };

  const { error } = await fetchCreateAssetCredential(payload);

  if (!error) {
    window.$message?.success(t('page.envops.assetCredential.messages.createSuccess'));
    resetForm();
    await loadCredentials();
  }

  creating.value = false;
}

function resetForm() {
  formModel.name = '';
  formModel.credentialType = 'ssh_password';
  formModel.username = '';
  formModel.secret = '';
  formModel.description = '';
}

function formatDateTime(value: string) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ').slice(0, 19);
}

onMounted(() => {
  void loadCredentials();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.assetCredential.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.assetCredential.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="info">{{ t('page.envops.assetCredential.tags.total', { count: credentialSummary.total }) }}</NTag>
          <NTag type="warning">
            {{ t('page.envops.assetCredential.tags.passwords', { count: credentialSummary.sshPassword }) }}
          </NTag>
          <NTag type="success">
            {{ t('page.envops.assetCredential.tags.keys', { count: credentialSummary.sshKey }) }}
          </NTag>
          <NTag type="primary">
            {{ t('page.envops.assetCredential.tags.tokens', { count: credentialSummary.apiToken }) }}
          </NTag>
        </NSpace>
      </div>
    </NCard>

    <NGrid cols="1 xl:2" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi>
        <NCard :bordered="false" class="card-wrapper" :title="t('page.envops.assetCredential.form.title')">
          <NForm label-placement="top">
            <NFormItem :label="t('page.envops.assetCredential.form.name')">
              <NInput
                v-model:value="formModel.name"
                :placeholder="t('page.envops.assetCredential.form.placeholders.name')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetCredential.form.type')">
              <NSelect v-model:value="formModel.credentialType" :options="credentialTypeOptions" />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetCredential.form.username')">
              <NInput
                v-model:value="formModel.username"
                :placeholder="t('page.envops.assetCredential.form.placeholders.username')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetCredential.form.secret')">
              <NInput
                v-model:value="formModel.secret"
                type="textarea"
                :rows="4"
                :placeholder="t('page.envops.assetCredential.form.placeholders.secret')"
              />
            </NFormItem>
            <NFormItem :label="t('page.envops.assetCredential.form.description')">
              <NInput
                v-model:value="formModel.description"
                :placeholder="t('page.envops.assetCredential.form.placeholders.description')"
              />
            </NFormItem>
            <NSpace>
              <NButton type="primary" :loading="creating" @click="handleCreateCredential">
                {{ t('page.envops.assetCredential.form.actions.create') }}
              </NButton>
              <NButton @click="resetForm">{{ t('common.reset') }}</NButton>
            </NSpace>
          </NForm>
        </NCard>
      </NGi>

      <NGi>
        <NCard :bordered="false" class="card-wrapper" :title="t('page.envops.assetCredential.table.title')">
          <NSpin :show="loading">
            <NTable v-if="credentials.length" :bordered="false" :single-line="false">
              <thead>
                <tr>
                  <th>{{ t('page.envops.assetCredential.table.name') }}</th>
                  <th>{{ t('page.envops.assetCredential.table.type') }}</th>
                  <th>{{ t('page.envops.assetCredential.table.username') }}</th>
                  <th>{{ t('page.envops.assetCredential.table.description') }}</th>
                  <th>{{ t('page.envops.assetCredential.table.createdAt') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in credentials" :key="item.id">
                  <td>{{ item.name }}</td>
                  <td>
                    <NTag size="small" :type="item.credentialType === 'ssh_key' ? 'success' : 'warning'">
                      {{ getCredentialTypeLabel(item.credentialType) }}
                    </NTag>
                  </td>
                  <td>{{ item.username || '-' }}</td>
                  <td>{{ item.description || '-' }}</td>
                  <td>{{ formatDateTime(item.createdAt) }}</td>
                </tr>
              </tbody>
            </NTable>
            <NEmpty v-else class="py-24px" :description="t('common.noData')" />
          </NSpin>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

<style scoped></style>
