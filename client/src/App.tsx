import { useEffect, useState } from 'react'
import { errorMessage, listProducts } from './api/products'
import ProductForm from './components/ProductForm'
import ProductList from './components/ProductList'
import type { Product } from './types/product'

function App() {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  // Bumped after a create so the effect fetches the list again.
  const [reloadCount, setReloadCount] = useState(0)

  useEffect(() => {
    // Ignores the response of a fetch that a newer one has replaced (or that StrictMode cancelled).
    let ignore = false
    listProducts()
      .then((list) => {
        if (!ignore) {
          setProducts(list)
          setError(null)
        }
      })
      .catch((err: unknown) => {
        if (!ignore) {
          setError(errorMessage(err))
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false)
        }
      })
    return () => {
      ignore = true
    }
  }, [reloadCount])

  return (
    <main>
      <h1>Products</h1>
      <ProductForm onCreated={() => setReloadCount((count) => count + 1)} />
      <ProductList products={products} loading={loading} error={error} />
    </main>
  )
}

export default App
