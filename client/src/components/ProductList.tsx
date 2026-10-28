import type { JSX } from 'react'
import type { Product } from '../types/product'

type ProductListProps = {
  products: Product[]
  loading: boolean
  error: string | null
}

export default function ProductList(_props: ProductListProps): JSX.Element {
  throw new Error('TODO S2.2')
}
