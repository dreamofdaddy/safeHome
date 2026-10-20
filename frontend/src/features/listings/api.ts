import { apiClient, unwrap } from '../../lib/api/client'
import type { Listing, ListingMetadata, ListingRegistration, RegistryDocument, RegistryProcessing, SafetyResult } from '../../types/api'

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
  update: (id: number, metadata: ListingMetadata) => unwrap<Listing>(apiClient.put(`/listings/${id}`, metadata)),
  remove: (id: number) => apiClient.delete(`/listings/${id}`),
  processing: (id: number) => unwrap<RegistryProcessing>(apiClient.get(`/listings/${id}/registry-processing`)),
  startProcessing: (id: number) => unwrap<RegistryProcessing>(apiClient.post(`/listings/${id}/registry-processing`)),
  safetyResult: (id: number) => unwrap<SafetyResult>(apiClient.get(`/listings/${id}/safety-analysis`)),
  analyze: (id: number) => unwrap<SafetyResult>(apiClient.post(`/listings/${id}/safety-analysis`)),
}
