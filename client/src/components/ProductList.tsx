import type { JSX } from 'react'
import type { Product } from '../api/products'

type ProductListProps = {
  products: Product[]
  loading: boolean
  error: string | null
}

export default function ProductList(_props: ProductListProps): JSX.Element {
  throw new Error('TODO S2.2')
}
