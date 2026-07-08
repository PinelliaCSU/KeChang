const DEFAULT_BACKEND_ORIGIN = 'http://localhost:8080'

export const backendOrigin = () => {
  const envOrigin = import.meta.env.VITE_API_BASE_URL
  if (envOrigin) return envOrigin.replace(/\/$/, '')

  const { protocol, hostname, port, origin } = window.location
  if (port === '5173' || port === '3000' || port === '4173') {
    return `${protocol}//${hostname || 'localhost'}:8080`
  }
  return origin || DEFAULT_BACKEND_ORIGIN
}

export const resolveFileUrl = (url) => {
  if (!url) return ''
  const value = String(url).trim()
  if (/^(https?:|blob:|data:)/i.test(value)) return value
  const cleanPath = value.startsWith('/') ? value : `/${value}`
  return `${backendOrigin()}${encodeURI(cleanPath)}`
}

export const getFileName = (url, fallback = '文件') => {
  if (!url) return fallback
  const clean = String(url).split('?')[0].split('#')[0]
  const name = clean.split('/').pop()
  try {
    return decodeURIComponent(name || fallback)
  } catch (e) {
    return name || fallback
  }
}

export const getFileExtension = (nameOrUrl = '') => {
  const clean = String(nameOrUrl).split('?')[0].split('#')[0].toLowerCase()
  return clean.includes('.') ? clean.substring(clean.lastIndexOf('.') + 1) : ''
}
