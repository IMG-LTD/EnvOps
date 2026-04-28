<script setup lang="ts">
import { useI18n } from 'vue-i18n';

defineOptions({
  name: 'SystemRbac'
});

const { t } = useI18n();
</script>

<template>
  <div>{{ t('page.envops.systemRbac.hero.title') }}</div>
</template>
