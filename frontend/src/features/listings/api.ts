import { apiClient, unwrap } from '../../lib/api/client'
import type { BuildingRegisterEnvelope, BuildingRegisterReview, BuildingIdentification, Listing, ListingMetadata, ListingRegistration, RegistryDocument, RegistryProcessing, RegistryFacts, SafetyResult } from '../../types/api'

export const listingsApi = {
  mine: () => unwrap<Listing[]>(apiClient.get('/listings/mine')),
  one: (id: number) => unwrap<Listing>(apiClient.get(`/listings/${id}`)),
  documents: () => unwrap<RegistryDocument[]>(apiClient.get('/registry-documents/mine')),
  register: (metadata: ListingMetadata, file: File) => {
    const form = new FormData()
    form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
    form.append('registryPdf', file)
    return unwrap<ListingRegistration>(apiClient.post('/registry-documents/listings', form))
  },
  create: (metadata: ListingMetadata) => unwrap<Listing>(apiClient.post('/listings', metadata)),
  uploadAndAnalyzeRegistry: (id: number, file: File, requestId: string) => {
    const form = new FormData(); form.append('registryPdf', file); form.append('consent', 'true'); form.append('requestId', requestId)
    return unwrap<RegistryProcessing>(apiClient.post(`/listings/${id}/registry-analysis`, form))
  },
  update: (id: number, metadata: ListingMetadata) => unwrap<Listing>(apiClient.put(`/listings/${id}`, metadata)),
  remove: (id: number) => apiClient.delete(`/listings/${id}`),
  processing: (id: number) => unwrap<RegistryProcessing>(apiClient.get(`/listings/${id}/registry-processing`)),
  startProcessing: (id: number, reparseConsent = false) => unwrap<RegistryProcessing>(apiClient.post(`/listings/${id}/registry-processing`, reparseConsent ? { reparseConsent: true } : undefined)),
  registryFacts: (id: number, documentId?: number) => unwrap<RegistryFacts>(apiClient.get(`/listings/${id}/registry-facts`, { params: documentId ? { documentId } : undefined })),
  registryHistory: (id: number) => unwrap<RegistryDocument[]>(apiClient.get(`/listings/${id}/registry-documents`)),
  registryDocumentFile: async (documentId: number) => (await apiClient.get<Blob>(`/registry-documents/${documentId}/file`, { responseType: 'blob' })).data,
  safetyResult: (id: number) => unwrap<SafetyResult>(apiClient.get(`/listings/${id}/safety-analysis`)),
  buildingIdentification: (id: number) => unwrap<BuildingIdentification>(apiClient.get(`/listings/${id}/building-identification`)),
  retryBuildingIdentification: (id: number, input?: import('../../types/api').BuildingLookupInput) => unwrap<BuildingIdentification>(apiClient.post(`/listings/${id}/building-identification/lookup`, input || {})),
  buildingRegister: (id: number) => unwrap<BuildingRegisterEnvelope>(apiClient.get(`/listings/${id}/building-register`)),
  buildingRegisterReview: (id: number, reviewId: number) => unwrap<BuildingRegisterReview>(apiClient.get(`/listings/${id}/building-register/${reviewId}`)),
  uploadAndAnalyzeBuildingRegister: (id: number, files: File[], requestId: string) => {
    const form = new FormData(); files.forEach(file => form.append('files', file)); form.append('consent', 'true'); form.append('requestId', requestId)
    return unwrap<BuildingRegisterReview>(apiClient.post(`/listings/${id}/building-register/analysis`, form))
  },
  uploadBuildingRegister: (id: number, files: File[]) => {
    const form = new FormData(); files.forEach(file => form.append('files', file))
    return unwrap<BuildingRegisterReview>(apiClient.post(`/listings/${id}/building-register`, form))
  },
  startBuildingRegister: (id: number, reviewId: number) => unwrap<BuildingRegisterReview>(apiClient.post(`/listings/${id}/building-register/${reviewId}/processing`, { consent: true })),
  buildingRegisterFile: async (id: number, reviewId: number, index: number) => (await apiClient.get<Blob>(`/listings/${id}/building-register/${reviewId}/files/${index}`, { responseType: 'blob' })).data,
  analyze: (id: number) => unwrap<SafetyResult>(apiClient.post(`/listings/${id}/safety-analysis`)),
}
