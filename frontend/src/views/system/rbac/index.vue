<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  fetchCreateSystemRbacRole,
  fetchGetSystemRbacPermissions,
  fetchGetSystemRbacRolePermissions,
  fetchGetSystemRbacRoles,
  fetchUpdateSystemRbacRole,
  fetchUpdateSystemRbacRolePermissions
} from '@/service/api';
import { useAuth } from '@/hooks/business/auth';

defineOptions({
  name: 'SystemRbacPage'
});

type RoleFormModel = {
  roleKey: string;
  roleName: string;
  description: string;
  enabled: boolean;
};

const ROLE_MANAGE_PERMISSION = 'system:role:manage';

const { t } = useI18n();
const { hasAuth } = useAuth();

const roles = ref<Api.SystemRbac.RoleRecord[]>([]);
const permissionModules = ref<Api.SystemRbac.PermissionModule[]>([]);
const selectedRoleId = ref<number | null>(null);
const assignedPermissionKeys = ref<string[]>([]);
const keyword = ref('');
const loading = ref(false);
const savingRole = ref(false);
const savingPermissions = ref(false);
const loadingRolePermissions = ref(false);
const permissionsLoadedForRoleId = ref<number | null>(null);
const roleForm = reactive<RoleFormModel>(createDefaultRoleForm());

const canManageRole = computed(() => hasAuth(ROLE_MANAGE_PERMISSION));

const filteredRoles = computed(() => {
  const value = keyword.value.trim().toLowerCase();

  if (!value) {
    return roles.value;
  }

  return roles.value.filter(role => {
    return role.roleKey.toLowerCase().includes(value) || role.roleName.toLowerCase().includes(value);
  });
});

const selectedRole = computed(() => roles.value.find(role => role.id === selectedRoleId.value) || null);

function createDefaultRoleForm(): RoleFormModel {
  return {
    roleKey: '',
    roleName: '',
    description: '',
    enabled: true
  };
}

function resetRoleForm() {
  Object.assign(roleForm, createDefaultRoleForm());
}

function fillRoleForm(role: Api.SystemRbac.RoleRecord) {
  Object.assign(roleForm, {
    roleKey: role.roleKey,
    roleName: role.roleName,
    description: role.description || '',
    enabled: role.enabled
  } satisfies RoleFormModel);
}

function collectActionKeys(permission: Api.SystemRbac.PermissionNode) {
  return (permission.children || [])
    .filter(child => child.permissionType === 'action')
    .map(child => child.permissionKey);
}

function isPermissionChecked(permissionKey: string) {
  return assignedPermissionKeys.value.includes(permissionKey);
}

function setPermissionChecked(permission: Api.SystemRbac.PermissionNode, checked: boolean) {
  const next = new Set(assignedPermissionKeys.value);

  if (checked) {
    next.add(permission.permissionKey);
  } else {
    next.delete(permission.permissionKey);

    if (permission.permissionType === 'menu') {
      collectActionKeys(permission).forEach(actionKey => next.delete(actionKey));
    }
  }

  assignedPermissionKeys.value = Array.from(next);
}

function isActionDisabled(permission: Api.SystemRbac.PermissionNode) {
  if (permission.permissionType !== 'action' || !permission.parentKey) {
    return false;
  }

  return !assignedPermissionKeys.value.includes(permission.parentKey);
}

async function loadRoles() {
  const response = await fetchGetSystemRbacRoles();

  if (!response.error) {
    roles.value = response.data;

    if (selectedRoleId.value === null && response.data.length > 0) {
      await selectRole(response.data[0]);
    }
  }
}

async function loadPermissions() {
  const response = await fetchGetSystemRbacPermissions();

  if (!response.error) {
    permissionModules.value = response.data;
  }
}

async function selectRole(role: Api.SystemRbac.RoleRecord) {
  selectedRoleId.value = role.id;
  fillRoleForm(role);
  assignedPermissionKeys.value = [];
  permissionsLoadedForRoleId.value = null;
  loadingRolePermissions.value = true;

  try {
    const response = await fetchGetSystemRbacRolePermissions(role.id);

    if (!response.error && selectedRoleId.value === role.id) {
      assignedPermissionKeys.value = response.data.permissionKeys;
      permissionsLoadedForRoleId.value = role.id;
    }
  } finally {
    if (selectedRoleId.value === role.id) {
      loadingRolePermissions.value = false;
    }
  }
}

function handleCreateRole() {
  selectedRoleId.value = null;
  assignedPermissionKeys.value = [];
  permissionsLoadedForRoleId.value = null;
  loadingRolePermissions.value = false;
  resetRoleForm();
}

async function handleSaveRole() {
  if (!canManageRole.value || !roleForm.roleName.trim()) {
    return;
  }

  savingRole.value = true;

  try {
    const response =
      selectedRoleId.value === null
        ? await fetchCreateSystemRbacRole({
            roleKey: roleForm.roleKey.trim(),
            roleName: roleForm.roleName.trim(),
            description: roleForm.description.trim() || null,
            enabled: roleForm.enabled
          })
        : await fetchUpdateSystemRbacRole(selectedRoleId.value, {
            roleName: roleForm.roleName.trim(),
            description: roleForm.description.trim() || null,
            enabled: roleForm.enabled
          });

    if (!response.error) {
      window.$message?.success(
        t(
          selectedRoleId.value === null
            ? 'page.envops.systemRbac.messages.createSuccess'
            : 'page.envops.systemRbac.messages.updateSuccess'
        )
      );
      await loadRoles();
      await selectRole(response.data);
    }
  } finally {
    savingRole.value = false;
  }
}

async function handleSavePermissions() {
  if (
    !canManageRole.value ||
    selectedRoleId.value === null ||
    loadingRolePermissions.value ||
    permissionsLoadedForRoleId.value !== selectedRoleId.value ||
    savingPermissions.value
  ) {
    return;
  }

  const roleId = selectedRoleId.value;

  savingPermissions.value = true;

  try {
    const response = await fetchUpdateSystemRbacRolePermissions(roleId, {
      permissionKeys: assignedPermissionKeys.value
    });

    if (!response.error && selectedRoleId.value === roleId) {
      assignedPermissionKeys.value = response.data.permissionKeys;
      window.$message?.success(t('page.envops.systemRbac.messages.permissionSaveSuccess'));
    }
  } finally {
    savingPermissions.value = false;
  }
}

async function loadPageData() {
  loading.value = true;

  try {
    await Promise.all([loadPermissions(), loadRoles()]);
  } finally {
    loading.value = false;
  }
}

onMounted(loadPageData);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false">
      <NSpace justify="space-between" align="center">
        <div>
          <h2 class="m-0 text-20px font-semibold">{{ t('page.envops.systemRbac.hero.title') }}</h2>
          <p class="m-t-8px text-#666">{{ t('page.envops.systemRbac.hero.description') }}</p>
        </div>
        <NSpace>
          <NButton @click="loadPageData">{{ t('page.envops.systemRbac.actions.refresh') }}</NButton>
          <NButton type="primary" :disabled="!canManageRole" @click="handleCreateRole">
            {{ t('page.envops.systemRbac.actions.createRole') }}
          </NButton>
        </NSpace>
      </NSpace>
    </NCard>

    <NSpin :show="loading">
      <div class="grid grid-cols-[320px_1fr] gap-16px">
        <NCard :title="t('page.envops.systemRbac.roleList.title')" :bordered="false">
          <NInput
            v-model:value="keyword"
            clearable
            :placeholder="t('page.envops.systemRbac.roleList.searchPlaceholder')"
          />
          <NEmpty
            v-if="filteredRoles.length === 0"
            class="m-t-16px"
            :description="t('page.envops.systemRbac.roleList.empty')"
          />
          <NSpace v-else class="m-t-16px" vertical>
            <NButton
              v-for="role in filteredRoles"
              :key="role.id"
              block
              :type="role.id === selectedRoleId ? 'primary' : 'default'"
              @click="selectRole(role)"
            >
              <div class="w-full flex justify-between">
                <span>{{ role.roleName }}</span>
                <NTag size="small" :type="role.enabled ? 'success' : 'default'">
                  {{
                    role.enabled
                      ? t('page.envops.systemRbac.roleList.enabled')
                      : t('page.envops.systemRbac.roleList.disabled')
                  }}
                </NTag>
              </div>
            </NButton>
          </NSpace>
        </NCard>

        <NSpace vertical :size="16">
          <NCard :title="t('page.envops.systemRbac.detail.title')" :bordered="false">
            <NForm label-placement="top">
              <NGrid :cols="2" :x-gap="16">
                <NFormItem :label="t('page.envops.systemRbac.detail.roleKey')">
                  <NInput v-model:value="roleForm.roleKey" :disabled="selectedRoleId !== null || !canManageRole" />
                </NFormItem>
                <NFormItem :label="t('page.envops.systemRbac.detail.roleName')">
                  <NInput v-model:value="roleForm.roleName" :disabled="!canManageRole" />
                </NFormItem>
              </NGrid>
              <NFormItem :label="t('page.envops.systemRbac.detail.description')">
                <NInput v-model:value="roleForm.description" type="textarea" :disabled="!canManageRole" />
              </NFormItem>
              <NFormItem :label="t('page.envops.systemRbac.detail.enabled')">
                <NSwitch v-model:value="roleForm.enabled" :disabled="!canManageRole" />
              </NFormItem>
              <NAlert v-if="selectedRole?.builtIn" type="info" :show-icon="false">
                {{ t('page.envops.systemRbac.detail.builtInHint') }}
              </NAlert>
              <div class="m-t-16px flex justify-end">
                <NButton type="primary" :loading="savingRole" :disabled="!canManageRole" @click="handleSaveRole">
                  {{ t('page.envops.systemRbac.actions.saveRole') }}
                </NButton>
              </div>
            </NForm>
          </NCard>

          <NCard :title="t('page.envops.systemRbac.permissions.title')" :bordered="false">
            <NSpace vertical :size="16">
              <NCard
                v-for="module in permissionModules"
                :key="module.moduleKey"
                size="small"
                :title="module.moduleName"
              >
                <NSpace vertical>
                  <div v-for="permission in module.permissions" :key="permission.permissionKey">
                    <NCheckbox
                      :checked="isPermissionChecked(permission.permissionKey)"
                      :disabled="!canManageRole || loadingRolePermissions"
                      @update:checked="checked => setPermissionChecked(permission, checked === true)"
                    >
                      {{ permission.permissionName }}
                    </NCheckbox>
                    <div v-if="permission.children?.length" class="m-l-24px m-t-8px flex flex-wrap gap-12px">
                      <NCheckbox
                        v-for="child in permission.children"
                        :key="child.permissionKey"
                        :checked="isPermissionChecked(child.permissionKey)"
                        :disabled="!canManageRole || loadingRolePermissions || isActionDisabled(child)"
                        @update:checked="checked => setPermissionChecked(child, checked === true)"
                      >
                        {{ child.permissionName }}
                      </NCheckbox>
                    </div>
                  </div>
                </NSpace>
              </NCard>
              <div class="flex justify-end">
                <NButton
                  type="primary"
                  :loading="savingPermissions"
                  :disabled="
                    !canManageRole ||
                    savingPermissions ||
                    selectedRoleId === null ||
                    loadingRolePermissions ||
                    permissionsLoadedForRoleId !== selectedRoleId
                  "
                  @click="handleSavePermissions"
                >
                  {{ t('page.envops.systemRbac.actions.savePermissions') }}
                </NButton>
              </div>
            </NSpace>
          </NCard>
        </NSpace>
      </div>
    </NSpin>
  </NSpace>
</template>
