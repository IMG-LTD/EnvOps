<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  fetchCreateSystemUser,
  fetchGetSystemRbacRoles,
  fetchGetSystemUserRoles,
  fetchGetSystemUsers,
  fetchUpdateSystemUser,
  fetchUpdateSystemUserRoles
} from '@/service/api';
import { useAuth } from '@/hooks/business/auth';

defineOptions({
  name: 'SystemUserPage'
});

type SystemUserStatusKey = 'active' | 'review' | 'disabled';
type SystemUserTagType = 'success' | 'warning' | 'default';
type SystemUserFormModel = {
  userName: string;
  password: string;
  phone: string;
  teamKey: string;
  loginType: string;
  status: string;
  roles: string[];
};

const PHONE_PATTERN =
  /^[1](([3][0-9])|([4][01456789])|([5][012356789])|([6][2567])|([7][0-8])|([8][0-9])|([9][012356789]))[0-9]{8}$/;
const USER_MANAGE_PERMISSION = 'system:user:manage';

const { t } = useI18n();
const { hasAuth } = useAuth();

const loading = ref(false);
const submitting = ref(false);
const requestToken = ref(0);
const roleAssignmentToken = ref(0);
const drawerVisible = ref(false);
const editingUserId = ref<number | null>(null);
const systemUserList = ref<Api.SystemUser.SystemUserRecord[]>([]);
const assignableRoles = ref<Api.SystemRbac.RoleRecord[]>([]);
const roleDrawerVisible = ref(false);
const assigningUserId = ref<number | null>(null);
const selectedRoleIds = ref<number[]>([]);
const loadingUserRoles = ref(false);
const savingUserRoles = ref(false);
const formModel = reactive<SystemUserFormModel>(createDefaultFormModel());

const canManageUser = computed(() => hasAuth(USER_MANAGE_PERMISSION));

const metrics = computed(() => [
  {
    key: 'users',
    label: t('page.envops.systemUser.summary.users.label'),
    value: String(systemUserList.value.length),
    desc: t('page.envops.systemUser.summary.users.desc')
  },
  {
    key: 'admins',
    label: t('page.envops.systemUser.summary.admins.label'),
    value: String(
      systemUserList.value.filter(item => item.roles.some(role => normalizeLookupValue(role).includes('admin'))).length
    ),
    desc: t('page.envops.systemUser.summary.admins.desc')
  },
  {
    key: 'activeToday',
    label: t('page.envops.systemUser.summary.activeToday.label'),
    value: String(
      systemUserList.value.filter(item => {
        if (!item.lastLoginAt) {
          return false;
        }

        return item.lastLoginAt.startsWith(getTodayDatePrefix());
      }).length
    ),
    desc: t('page.envops.systemUser.summary.activeToday.desc')
  }
]);

const teamOptions = computed(() => [
  { label: t('page.envops.common.team.envops'), value: 'envops' },
  { label: t('page.envops.common.team.platform'), value: 'platform' },
  { label: t('page.envops.common.team.release'), value: 'release' },
  { label: t('page.envops.common.team.traffic'), value: 'traffic' },
  { label: t('page.envops.common.team.qa'), value: 'qa' },
  { label: t('page.envops.common.team.sre'), value: 'sre' },
  { label: t('page.envops.common.team.fintech'), value: 'fintech' }
]);

const loginTypeOptions = computed(() => [
  { label: t('page.envops.common.loginType.password'), value: 'PASSWORD' },
  { label: t('page.envops.common.loginType.passwordOtp'), value: 'PASSWORD_OTP' },
  { label: t('page.envops.common.loginType.sso'), value: 'SSO' }
]);

const statusOptions = computed(() => [
  { label: t('page.envops.common.status.active'), value: 'ACTIVE' },
  { label: t('page.envops.common.status.review'), value: 'REVIEW' },
  { label: t('page.envops.common.status.disabled'), value: 'DISABLED' }
]);

const roleOptions = computed(() =>
  assignableRoles.value
    .filter(role => role.enabled)
    .map(role => ({ label: `${role.roleName} (${role.roleKey})`, value: role.id }))
);

const formRoleOptions = computed(() =>
  assignableRoles.value
    .filter(role => role.enabled)
    .map(role => ({ label: `${role.roleName} (${role.roleKey})`, value: role.roleKey }))
);

const drawerTitle = computed(() => {
  return t(
    editingUserId.value === null ? 'page.envops.systemUser.form.titleCreate' : 'page.envops.systemUser.form.titleEdit'
  );
});

const users = computed(() =>
  systemUserList.value.map(item => {
    const statusKey = getSystemUserStatusKey(item.status);

    return {
      key: item.id,
      id: item.id,
      userName: getDisplayText(item.userName),
      phone: getDisplayText(item.phone),
      role: getSystemUserRoleLabels(item.roles).join(' / '),
      team: getTeamLabel(item.teamKey),
      loginType: getLoginTypeLabel(item.loginType),
      lastLogin: getDateTimeText(item.lastLoginAt),
      status: getSystemUserStatusLabel(statusKey),
      statusType: getSystemUserTagType(statusKey)
    };
  })
);

function createDefaultFormModel(): SystemUserFormModel {
  return {
    userName: '',
    password: '',
    phone: '',
    teamKey: 'envops',
    loginType: 'PASSWORD',
    status: 'ACTIVE',
    roles: ['OBSERVER']
  };
}

function resetFormModel() {
  Object.assign(formModel, createDefaultFormModel());
}

function fillFormModel(user: Api.SystemUser.SystemUserRecord) {
  Object.assign(formModel, {
    userName: user.userName,
    password: '',
    phone: user.phone,
    teamKey: user.teamKey,
    loginType: user.loginType,
    status: user.status,
    roles: [...user.roles]
  } satisfies SystemUserFormModel);
}

function buildCreatePayload(): Api.SystemUser.CreateSystemUserParams {
  return {
    userName: formModel.userName.trim(),
    password: formModel.password.trim(),
    phone: formModel.phone.trim(),
    teamKey: formModel.teamKey.trim(),
    loginType: formModel.loginType.trim(),
    status: formModel.status.trim(),
    roles: formModel.roles.map(role => role.trim())
  };
}

function buildUpdatePayload(): Api.SystemUser.UpdateSystemUserParams {
  return {
    userName: formModel.userName.trim(),
    password: formModel.password.trim() || null,
    phone: formModel.phone.trim(),
    teamKey: formModel.teamKey.trim(),
    loginType: formModel.loginType.trim(),
    status: formModel.status.trim(),
    roles: formModel.roles.map(role => role.trim())
  };
}

async function loadSystemUsers() {
  const currentRequestToken = ++requestToken.value;

  loading.value = true;

  try {
    const { data, error } = await fetchGetSystemUsers();

    if (currentRequestToken !== requestToken.value) {
      return;
    }

    if (!error) {
      systemUserList.value = getSystemUserRecords(data);
    }
  } finally {
    if (currentRequestToken === requestToken.value) {
      loading.value = false;
    }
  }
}

async function loadAssignableRoles() {
  const { data, error } = await fetchGetSystemRbacRoles();

  if (!error) {
    assignableRoles.value = data.filter(role => role.enabled);
  }
}

function getSystemUserRecords(data: Api.SystemUser.SystemUserListResponse) {
  return Array.isArray(data) ? data : [];
}

function handleOpenCreateDrawer() {
  editingUserId.value = null;
  resetFormModel();
  drawerVisible.value = true;
}

function handleOpenEditDrawer(user: Api.SystemUser.SystemUserRecord) {
  editingUserId.value = user.id;
  fillFormModel(user);
  drawerVisible.value = true;
}

function handleDrawerVisibleChange(show: boolean) {
  drawerVisible.value = show;

  if (!show) {
    editingUserId.value = null;
    resetFormModel();
  }
}

function resetRoleAssignmentState() {
  roleAssignmentToken.value += 1;
  assigningUserId.value = null;
  selectedRoleIds.value = [];
  loadingUserRoles.value = false;
  savingUserRoles.value = false;
}

async function handleOpenRoleAssignment(record: Api.SystemUser.SystemUserRecord) {
  if (!canManageUser.value) {
    return;
  }

  const userId = record.id;
  const currentRoleAssignmentToken = ++roleAssignmentToken.value;

  assigningUserId.value = userId;
  selectedRoleIds.value = [];
  roleDrawerVisible.value = true;
  loadingUserRoles.value = true;

  try {
    const response = await fetchGetSystemUserRoles(userId);

    if (
      !response.error &&
      currentRoleAssignmentToken === roleAssignmentToken.value &&
      assigningUserId.value === userId &&
      roleDrawerVisible.value
    ) {
      selectedRoleIds.value = response.data.roleIds;
    }
  } finally {
    if (
      currentRoleAssignmentToken === roleAssignmentToken.value &&
      assigningUserId.value === userId &&
      roleDrawerVisible.value
    ) {
      loadingUserRoles.value = false;
    }
  }
}

async function handleSaveUserRoles() {
  if (!canManageUser.value || assigningUserId.value === null || loadingUserRoles.value || savingUserRoles.value) {
    return;
  }

  const userId = assigningUserId.value;
  const currentRoleAssignmentToken = roleAssignmentToken.value;
  const roleIds = [...selectedRoleIds.value];

  savingUserRoles.value = true;

  try {
    const response = await fetchUpdateSystemUserRoles(userId, { roleIds });

    if (
      !response.error &&
      currentRoleAssignmentToken === roleAssignmentToken.value &&
      assigningUserId.value === userId &&
      roleDrawerVisible.value
    ) {
      window.$message?.success(t('page.envops.systemUser.roleAssignment.saveSuccess'));
      roleDrawerVisible.value = false;
      resetRoleAssignmentState();
      await loadSystemUsers();
    }
  } finally {
    if (currentRoleAssignmentToken === roleAssignmentToken.value && assigningUserId.value === userId) {
      savingUserRoles.value = false;
    }
  }
}

function validateForm() {
  if (
    !formModel.userName.trim() ||
    !formModel.phone.trim() ||
    !formModel.teamKey.trim() ||
    !formModel.loginType.trim() ||
    !formModel.status.trim() ||
    formModel.roles.length === 0 ||
    (editingUserId.value === null && !formModel.password.trim())
  ) {
    window.$message?.warning(t('page.envops.systemUser.messages.fillRequired'));
    return false;
  }

  if (!PHONE_PATTERN.test(formModel.phone.trim())) {
    window.$message?.warning(t('page.envops.systemUser.messages.phoneInvalid'));
    return false;
  }

  return true;
}

async function handleSubmit() {
  if (!validateForm()) {
    return;
  }

  submitting.value = true;

  try {
    const response =
      editingUserId.value === null
        ? await fetchCreateSystemUser(buildCreatePayload())
        : await fetchUpdateSystemUser(editingUserId.value, buildUpdatePayload());

    if (!response.error) {
      window.$message?.success(
        t(
          editingUserId.value === null
            ? 'page.envops.systemUser.messages.createSuccess'
            : 'page.envops.systemUser.messages.updateSuccess'
        )
      );
      drawerVisible.value = false;
      editingUserId.value = null;
      resetFormModel();
      await loadSystemUsers();
    }
  } finally {
    submitting.value = false;
  }
}

function normalizeLookupValue(value?: string | null) {
  return String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '_');
}

function getDisplayText(value?: string | null) {
  if (typeof value === 'string' && value.trim()) {
    return value;
  }

  return '-';
}

function getDateTimeText(value?: string | null) {
  if (!value) {
    return '-';
  }

  return value.replace('T', ' ').slice(0, 16);
}

function getTodayDatePrefix() {
  return new Date().toISOString().slice(0, 10);
}

function getTeamLabel(value?: string | null) {
  const normalizedTeam = normalizeLookupValue(value);

  if (normalizedTeam === 'platform') {
    return t('page.envops.common.team.platform');
  }

  if (normalizedTeam === 'release') {
    return t('page.envops.common.team.release');
  }

  if (normalizedTeam === 'traffic') {
    return t('page.envops.common.team.traffic');
  }

  if (normalizedTeam === 'qa') {
    return t('page.envops.common.team.qa');
  }

  if (normalizedTeam === 'envops') {
    return t('page.envops.common.team.envops');
  }

  if (normalizedTeam === 'sre') {
    return t('page.envops.common.team.sre');
  }

  if (normalizedTeam === 'fintech') {
    return t('page.envops.common.team.fintech');
  }

  return getDisplayText(value);
}

function getLoginTypeLabel(value?: string | null) {
  const normalizedLoginType = normalizeLookupValue(value);

  if (normalizedLoginType === 'password_otp') {
    return t('page.envops.common.loginType.passwordOtp');
  }

  if (normalizedLoginType === 'password') {
    return t('page.envops.common.loginType.password');
  }

  if (normalizedLoginType === 'sso') {
    return t('page.envops.common.loginType.sso');
  }

  return getDisplayText(value);
}

function getSystemUserRoleLabel(role?: string | null) {
  const normalizedRole = normalizeLookupValue(role);

  if (normalizedRole === 'super_admin') {
    return t('page.envops.common.role.superAdmin');
  }

  if (normalizedRole === 'platform_admin') {
    return t('page.envops.common.role.platformAdmin');
  }

  if (normalizedRole === 'release_manager') {
    return t('page.envops.common.role.releaseManager');
  }

  if (normalizedRole === 'traffic_owner') {
    return t('page.envops.common.role.trafficOwner');
  }

  if (normalizedRole === 'observer') {
    return t('page.envops.common.role.observer');
  }

  return getDisplayText(role);
}

function getSystemUserRoleLabels(roles: string[]) {
  const labels = roles.map(role => getSystemUserRoleLabel(role)).filter(label => label !== '-');

  return labels.length ? labels : [getDisplayText(roles[0])];
}

function getSystemUserStatusKey(value?: string | null): SystemUserStatusKey {
  const normalizedStatus = normalizeLookupValue(value);

  if (normalizedStatus.includes('review')) {
    return 'review';
  }

  if (normalizedStatus.includes('disable')) {
    return 'disabled';
  }

  return 'active';
}

function getSystemUserStatusLabel(statusKey: SystemUserStatusKey) {
  const labelMap: Record<SystemUserStatusKey, string> = {
    active: t('page.envops.common.status.active'),
    review: t('page.envops.common.status.review'),
    disabled: t('page.envops.common.status.disabled')
  };

  return labelMap[statusKey];
}

function getSystemUserTagType(statusKey: SystemUserStatusKey): SystemUserTagType {
  const typeMap: Record<SystemUserStatusKey, SystemUserTagType> = {
    active: 'success',
    review: 'warning',
    disabled: 'default'
  };

  return typeMap[statusKey];
}

watch(roleDrawerVisible, show => {
  if (!show) {
    resetRoleAssignmentState();
  }
});

onMounted(() => {
  void Promise.all([loadSystemUsers(), loadAssignableRoles()]);
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <div class="flex-col gap-12px lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h3 class="text-18px font-semibold">{{ t('page.envops.systemUser.hero.title') }}</h3>
          <p class="mt-8px text-14px text-#666">{{ t('page.envops.systemUser.hero.description') }}</p>
        </div>
        <NSpace>
          <NTag type="success">{{ t('page.envops.systemUser.tags.rbacEnabled') }}</NTag>
          <NTag type="warning">{{ t('page.envops.systemUser.tags.userUnderReview') }}</NTag>
        </NSpace>
      </div>
    </NCard>

    <NGrid cols="1 s:3" responsive="screen" :x-gap="16" :y-gap="16">
      <NGi v-for="item in metrics" :key="item.key">
        <NCard :bordered="false" class="card-wrapper" :data-summary-key="item.key">
          <NStatistic :label="item.label" :value="item.value" />
          <div class="mt-12px text-12px text-#999">{{ item.desc }}</div>
        </NCard>
      </NGi>
    </NGrid>

    <NCard :title="t('page.envops.systemUser.table.title')" :bordered="false" class="card-wrapper">
      <template #header-extra>
        <NSpace>
          <NButton type="primary" @click="handleOpenCreateDrawer">
            {{ t('page.envops.systemUser.actions.create') }}
          </NButton>
          <NButton :loading="loading" @click="loadSystemUsers">
            {{ t('page.envops.systemUser.actions.refresh') }}
          </NButton>
        </NSpace>
      </template>

      <NSpin :show="loading">
        <NTable v-if="users.length" :bordered="false" :single-line="false">
          <thead>
            <tr>
              <th>{{ t('page.envops.systemUser.table.user') }}</th>
              <th>{{ t('page.envops.systemUser.table.role') }}</th>
              <th>{{ t('page.envops.systemUser.table.team') }}</th>
              <th>{{ t('page.envops.systemUser.table.loginType') }}</th>
              <th>{{ t('page.envops.systemUser.table.lastLogin') }}</th>
              <th>{{ t('page.envops.systemUser.table.status') }}</th>
              <th>{{ t('page.envops.systemUser.table.operation') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in users" :key="item.key">
              <td>
                <div>{{ item.userName }}</div>
                <div class="mt-4px text-12px text-#999">{{ item.phone }}</div>
              </td>
              <td>{{ item.role }}</td>
              <td>{{ item.team }}</td>
              <td>{{ item.loginType }}</td>
              <td>{{ item.lastLogin }}</td>
              <td>
                <NTag :type="item.statusType" size="small">{{ item.status }}</NTag>
              </td>
              <td>
                <NSpace>
                  <NButton
                    text
                    type="primary"
                    @click="handleOpenEditDrawer(systemUserList.find(user => user.id === item.id)!)"
                  >
                    {{ t('page.envops.systemUser.actions.edit') }}
                  </NButton>
                  <NButton
                    text
                    type="primary"
                    :disabled="!canManageUser"
                    @click="handleOpenRoleAssignment(systemUserList.find(user => user.id === item.id)!)"
                  >
                    {{ t('page.envops.systemUser.roleAssignment.title') }}
                  </NButton>
                </NSpace>
              </td>
            </tr>
          </tbody>
        </NTable>
        <NEmpty v-else class="py-24px" :description="t('common.noData')" />
      </NSpin>
    </NCard>

    <NDrawer :show="drawerVisible" :width="480" placement="right" @update:show="handleDrawerVisibleChange">
      <NDrawerContent :title="drawerTitle" closable>
        <NForm label-placement="top">
          <NFormItem :label="t('page.envops.systemUser.form.userName')">
            <NInput
              v-model:value="formModel.userName"
              :placeholder="t('page.envops.systemUser.form.placeholders.userName')"
            />
          </NFormItem>
          <NFormItem :label="t('page.envops.systemUser.form.password')">
            <NInput
              v-model:value="formModel.password"
              type="password"
              show-password-on="click"
              :placeholder="
                t(
                  editingUserId === null
                    ? 'page.envops.systemUser.form.placeholders.passwordCreate'
                    : 'page.envops.systemUser.form.placeholders.passwordEdit'
                )
              "
            />
          </NFormItem>
          <NFormItem :label="t('page.envops.systemUser.form.phone')">
            <NInput
              v-model:value="formModel.phone"
              :placeholder="t('page.envops.systemUser.form.placeholders.phone')"
            />
          </NFormItem>
          <NFormItem :label="t('page.envops.systemUser.form.team')">
            <NSelect v-model:value="formModel.teamKey" :options="teamOptions" />
          </NFormItem>
          <NFormItem :label="t('page.envops.systemUser.form.loginType')">
            <NSelect v-model:value="formModel.loginType" :options="loginTypeOptions" />
          </NFormItem>
          <NFormItem :label="t('page.envops.systemUser.form.status')">
            <NSelect v-model:value="formModel.status" :options="statusOptions" />
          </NFormItem>
          <NFormItem :label="t('page.envops.systemUser.form.roles')">
            <NSelect
              v-model:value="formModel.roles"
              multiple
              :options="formRoleOptions"
              :placeholder="t('page.envops.systemUser.form.placeholders.roles')"
            />
          </NFormItem>
        </NForm>

        <template #footer>
          <NSpace justify="end">
            <NButton @click="handleDrawerVisibleChange(false)">
              {{ t('common.cancel') }}
            </NButton>
            <NButton type="primary" :loading="submitting" @click="handleSubmit">
              {{ t('page.envops.systemUser.actions.save') }}
            </NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>

    <NDrawer v-model:show="roleDrawerVisible" :width="420" placement="right">
      <NDrawerContent :title="t('page.envops.systemUser.roleAssignment.title')" closable>
        <NSpin :show="loadingUserRoles">
          <NForm label-placement="top">
            <NFormItem :label="t('page.envops.systemUser.form.roles')">
              <NSelect
                v-model:value="selectedRoleIds"
                multiple
                :options="roleOptions"
                :disabled="!canManageUser || loadingUserRoles || savingUserRoles"
                :placeholder="t('page.envops.systemUser.form.placeholders.roles')"
              />
            </NFormItem>
          </NForm>
        </NSpin>

        <template #footer>
          <NSpace justify="end">
            <NButton :disabled="savingUserRoles" @click="roleDrawerVisible = false">
              {{ t('common.cancel') }}
            </NButton>
            <NButton
              type="primary"
              :loading="savingUserRoles"
              :disabled="!canManageUser || loadingUserRoles || savingUserRoles || assigningUserId === null"
              @click="handleSaveUserRoles"
            >
              {{ t('page.envops.systemUser.actions.save') }}
            </NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>
  </NSpace>
</template>

<style scoped></style>
