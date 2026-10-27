package com.shanthan.products.product;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional
	public ProductResponse create(CreateProductRequest request) {
		throw new UnsupportedOperationException("TODO S1.1");
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> list() {
		throw new UnsupportedOperationException("TODO S2.1");
	}
}
