import { useState, type JSX, type SubmitEvent } from 'react'
import { createProduct, errorMessage } from '../api/products'
import { validateProductName } from '../validation/productName'

type ProductFormProps = {
  onCreated: () => void
}

type Status = { kind: 'success' | 'error'; text: string }

function failure(description: string): Status {
  return { kind: 'error', text: `Product creation failed: ${description}` }
}

export default function ProductForm({ onCreated }: ProductFormProps): JSX.Element {
  const [name, setName] = useState('')
  const [status, setStatus] = useState<Status | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    // The name is sent as typed; the API trims and uppercases it.
    const invalid = validateProductName(name)
    if (invalid !== null) {
      setStatus(failure(invalid))
      return
    }
    setStatus(null)
    setSubmitting(true)
    try {
      const product = await createProduct(name)
      setName('')
      setStatus({ kind: 'success', text: `Product "${product.name}" created.` })
      onCreated()
    } catch (err) {
      setStatus(failure(errorMessage(err)))
    } finally {
      setSubmitting(false)
    }
  }

  const failed = status?.kind === 'error'

  return (
    <form onSubmit={handleSubmit} noValidate>
      <label htmlFor="product-name">Name</label>
      <input
        id="product-name"
        value={name}
        onChange={(event) => setName(event.target.value)}
        aria-invalid={failed}
        aria-describedby={failed ? 'product-form-status' : undefined}
      />
      <button type="submit" disabled={submitting}>
        {submitting ? 'Creating…' : 'Create'}
      </button>
      {status !== null && (
        <p
          id="product-form-status"
          className={failed ? 'message-error' : 'message-success'}
          role={failed ? 'alert' : 'status'}
        >
          {status.text}
        </p>
      )}
    </form>
  )
}
