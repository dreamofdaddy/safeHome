import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppLayout } from '../components/AppLayout'
import { AuthGuard } from '../features/auth/AuthGuard'

export const router = createBrowserRouter([
  { element: <AppLayout />, children: [
    { path: '/login', lazy: async () => ({ Component: (await import('../features/auth/AuthPage')).AuthPage }) },
    { path: '/signup', lazy: async () => { const { AuthPage } = await import('../features/auth/AuthPage'); return { Component: () => <AuthPage mode="signup" /> } } },
    { path: '/change-password', lazy: async () => ({ Component: (await import('../features/auth/ChangePasswordPage')).ChangePasswordPage }) },
    { element: <AuthGuard />, children: [
      { path: '/listings', lazy: async () => ({ Component: (await import('../features/listings/ListingsPage')).ListingsPage }) },
      { path: '/listings/new', lazy: async () => ({ Component: (await import('../features/listings/RegisterListingPage')).RegisterListingPage }) },
      { path: '/listings/:id/edit', lazy: async () => ({ Component: (await import('../features/listings/RegisterListingPage')).RegisterListingPage }) },
      { path: '/listings/:id', lazy: async () => ({ Component: (await import('../features/listings/ListingDetailPage')).ListingDetailPage }) },
    ] },
    { path: '*', element: <Navigate to="/listings" replace /> },
  ] },
])
