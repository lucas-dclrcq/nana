export const http = async <T>(url: string, options: RequestInit = {}): Promise<T> => {
  const response = await fetch(url, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...options.headers,
    },
  })
  if (!response.ok) {
    let message = `Request failed (HTTP ${response.status})`
    try {
      const body = await response.json()
      if (body && typeof body.message === 'string') {
        message = body.message
      }
    } catch {
      // no JSON body; keep the generic message
    }
    throw new Error(message)
  }
  let data: unknown
  if (response.status !== 204) {
    try {
      data = await response.json()
    } catch {
      data = undefined
    }
  }
  return { data, status: response.status, headers: response.headers } as T
}

export default http
