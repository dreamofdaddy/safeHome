import { describe, expect, it } from 'vitest'
import { canAnalyze, ownsListing } from './format'
import type { Listing, SafetyResult, User } from '../types/api'

const user = { id: 7 } as User
const listing = { registeredByUserId: 7, status: 'REGISTRY_READY' } as Listing

describe('listing ownership actions', () => {
  it('allows an owner to analyze a ready listing', () => expect(canAnalyze(listing, undefined, user)).toBe(true))
  it('allows an explicit manual analysis without a reanalysis-needed banner', () => {
    expect(canAnalyze(listing, { reanalysisAvailable: false } as SafetyResult, user)).toBe(true)
    expect(canAnalyze(listing, { reanalysisAvailable: true } as SafetyResult, user)).toBe(true)
  })
  it('never exposes analysis to another user', () => expect(canAnalyze({ ...listing, registeredByUserId: 8 }, { reanalysisAvailable: true } as never, user)).toBe(false))
  it('keeps a completed listing available for manual analysis', () => {
    expect(canAnalyze({ ...listing, status: 'SAFETY_ANALYZED' }, { reanalysisAvailable: true } as never, user)).toBe(true)
    expect(canAnalyze({ ...listing, status: 'SAFETY_ANALYZED' }, { reanalysisAvailable: false } as never, user)).toBe(true)
    expect(ownsListing(listing, user)).toBe(true)
  })
})
