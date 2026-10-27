const PDF_ONLY = '등기사항증명서 파일은 PDF 형식만 등록할 수 있습니다. PDF 파일을 선택해 주세요.'

export async function registryPdfError(file: File): Promise<string | undefined> {
  if (!/\.pdf$/i.test(file.name)
    || (file.type && !['application/pdf', 'application/octet-stream'].includes(file.type.toLowerCase()))) return PDF_ONLY
  if (!file.size) return '내용이 비어 있는 파일입니다. 다른 PDF 파일을 선택해 주세요.'
  if (file.size > 10 * 1024 * 1024) return 'PDF는 10MB 이하로 선택해 주세요.'
  try {
    const bytes = await new Promise<ArrayBuffer>((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result as ArrayBuffer)
      reader.onerror = () => reject(new Error('FILE_READ_FAILED'))
      reader.onabort = () => reject(new Error('FILE_READ_ABORTED'))
      reader.readAsArrayBuffer(file.slice(0, 5))
    })
    if (String.fromCharCode(...new Uint8Array(bytes)) !== '%PDF-') return PDF_ONLY
  } catch { return '파일을 읽을 수 없습니다. PDF 파일을 다시 선택해 주세요.' }
}
