// Data shapes shared by the API module and the components. They follow the API contract in docs/PLAN.md.

export type Product = {
  id: string
  name: string
}

export type FieldError = {
  field: string
  message: string
}

// RFC 9457 problem details. Spring omits `type` when it's the default "about:blank".
export type ProblemDetail = {
  type?: string
  title: string
  status: number
  detail?: string
  instance: string
  errors?: FieldError[]
}
