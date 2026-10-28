// The only module that talks to the API.

import type { ProblemDetail, Product } from '../types/product'

const UNEXPECTED_ERROR = 'Something went wrong. Please try again.'

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? UNEXPECTED_ERROR)
    this.name = 'ApiError'
    this.problem = problem
  }
}

// A human-readable reason for any error a call here can throw: the first field error, else the API's
// detail, else a generic message (also used for network failures).
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.problem.errors?.[0]?.message ?? error.problem.detail ?? UNEXPECTED_ERROR
  }
  return UNEXPECTED_ERROR
}

// Anything that isn't a problem+json body (e.g. the dev proxy failing to reach the API) gets a generic message.
async function toApiError(response: Response): Promise<ApiError> {
  if (response.headers.get('Content-Type')?.includes('application/problem+json')) {
    const problem: unknown = await response.json()
    return new ApiError(problem as ProblemDetail)
  }
  return new ApiError({
    title: response.statusText,
    status: response.status,
    detail: UNEXPECTED_ERROR,
    instance: new URL(response.url).pathname,
  })
}

// Newest first; the API decides the order.
export async function listProducts(): Promise<Product[]> {
  const response = await fetch('/api/v1/products')
  if (!response.ok) {
    throw await toApiError(response)
  }
  const products: unknown = await response.json()
  return products as Product[]
}

export async function createProduct(name: string): Promise<Product> {
  const response = await fetch('/api/v1/products', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  if (!response.ok) {
    throw await toApiError(response)
  }
  const product: unknown = await response.json()
  return product as Product
}
