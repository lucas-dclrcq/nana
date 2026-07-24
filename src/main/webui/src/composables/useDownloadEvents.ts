import { reactive } from 'vue'
import { useQueryClient, type QueryClient } from '@tanstack/vue-query'
import { getListDownloadsQueryKey } from '../api/generated/nana'
import type { DownloadDto, listDownloadsResponse } from '../api/generated/nana'

const downloads = reactive(new Map<string, DownloadDto>())
let started = false

export function useDownloadEvents() {
  const queryClient = useQueryClient()
  if (!started) {
    started = true
    start(queryClient)
  }
  return { downloads }
}

function start(queryClient: QueryClient) {
  const source = new EventSource('/api/downloads/events')

  source.onmessage = (event) => {
    let dl: DownloadDto
    try {
      dl = JSON.parse(event.data) as DownloadDto
    } catch {
      return
    }

    if (dl.md5) {
      downloads.set(dl.md5, dl)
    }

    if (dl.status === 'PENDING') {
      queryClient.invalidateQueries({ queryKey: getListDownloadsQueryKey() })
      return
    }

    queryClient.setQueriesData<listDownloadsResponse>({ queryKey: getListDownloadsQueryKey() }, (old) => {
      const content = old?.data.content
      if (!content) return old
      const index = content.findIndex((row) => row.id === dl.id)
      if (index === -1) return old
      const next = [...content]
      next[index] = dl
      return { ...old, data: { ...old.data, content: next } }
    })
  }

  source.onopen = () => {
    queryClient.invalidateQueries({ queryKey: getListDownloadsQueryKey() })
  }
}
