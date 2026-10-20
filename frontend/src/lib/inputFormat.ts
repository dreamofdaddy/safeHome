export const digitsOnly = (value: string, maxLength?: number) => {
  const digits = value.replace(/\D/g, '')
  return maxLength ? digits.slice(0, maxLength) : digits
}

export const formatPhone = (value: string) => {
  const digits = digitsOnly(value, 11)
  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

export const isStrongPassword = (value: string) => value.length >= 12
  && value.length <= 72
  && !/\s/.test(value)
  && /[A-Za-z]/.test(value)
  && /\d/.test(value)
  && /[^A-Za-z\d\s]/.test(value)

const EMAIL_PATTERN = /^(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,63}$/

export const isValidEmail = (value: string) => {
  const email = value.trim()
  return email.length <= 254 && EMAIL_PATTERN.test(email)
}

export const formatMoney = (value: string) => {
  const digits = digitsOnly(value, 15)
  return digits ? new Intl.NumberFormat('ko-KR').format(Number(digits)) : ''
}

export const parseMoney = (value: string) => {
  const digits = digitsOnly(value, 15)
  return digits ? Number(digits) : 0
}
