import type { JSX } from 'react'
import type { Product } from '../types/product'

type ProductListProps = {
  products: Product[]
  loading: boolean
  error: string | null
}

export default function ProductList({ products, loading, error }: ProductListProps): JSX.Element {
  let content: JSX.Element
  if (loading) {
    content = <p role="status">Loading products…</p>
  } else {
    // A failed reload shows the error above the list that is already loaded.
    content = (
      <>
        {error !== null && (
          <p className="message-error" role="alert">
            Could not load products: {error}
          </p>
        )}
        {products.length > 0 ? (
          <table className="product-table">
            <thead>
              <tr>
                <th scope="col">ID</th>
                <th scope="col">Name</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td>{product.id}</td>
                  <td>{product.name}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          error === null && <p>No products yet.</p>
        )}
      </>
    )
  }

  return (
    <section aria-labelledby="product-list-heading">
      <h2 id="product-list-heading">Saved products</h2>
      {content}
    </section>
  )
}
