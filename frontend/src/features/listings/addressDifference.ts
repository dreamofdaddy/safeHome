export function addressDifference(left: string, right: string) {
  const a = Array.from(left), b = Array.from(right), width = b.length + 1
  const matrix = new Uint16Array((a.length + 1) * width)
  for (let i = a.length - 1; i >= 0; i--) for (let j = b.length - 1; j >= 0; j--) {
    matrix[i * width + j] = a[i] === b[j] ? matrix[(i + 1) * width + j + 1] + 1 : Math.max(matrix[(i + 1) * width + j], matrix[i * width + j + 1])
  }
  const first: { text: string; different: boolean }[] = [], second: typeof first = []
  const push = (parts: typeof first, text: string, different: boolean) => {
    const previous = parts[parts.length - 1]
    if (previous?.different === different) previous.text += text
    else parts.push({ text, different })
  }
  let i = 0, j = 0
  while (i < a.length || j < b.length) {
    if (i < a.length && j < b.length && a[i] === b[j]) { push(first, a[i++], false); push(second, b[j++], false) }
    else if (i < a.length && (j === b.length || matrix[(i + 1) * width + j] >= matrix[i * width + j + 1])) push(first, a[i++], true)
    else push(second, b[j++], true)
  }
  return { first, second }
}
