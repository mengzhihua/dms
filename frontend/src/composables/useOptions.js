import { onMounted, ref } from 'vue'

export function useOptions(api, labelKey = 'name', valueKey = 'code') {
  const options = ref([])

  async function load() {
    const rows = await api.list({ size: 200 })
    options.value = (rows || []).map((row) => ({
      label: row[labelKey],
      value: row[valueKey],
      row
    }))
  }

  onMounted(load)
  return { options, load }
}
