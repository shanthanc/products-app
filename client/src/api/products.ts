// The only module that talks to the API. Response and error types follow the API contract in docs/PLAN.md.

export type Product = {
  id: string
  name: string
}

export type FieldError = {
  field: string
  message: string
}

export type ProblemDetail = {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  errors?: FieldError[]
}

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(_problem: ProblemDetail) {
    super()
    throw new Error('TODO S1.4')
  }
}

export async function listProducts(): Promise<Product[]> {
  throw new Error('TODO S2.2')
}

export async function createProduct(_name: string): Promise<Product> {
  throw new Error('TODO S1.4')
}
