package com.shanthan.products.service;

import com.shanthan.products.entity.Product;
import com.shanthan.products.model.CreateProductRequest;
import com.shanthan.products.model.ProductResponse;
import com.shanthan.products.repository.ProductRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	static String normalizeName(String name) {
		return name.strip().toUpperCase(Locale.ROOT);
	}

	@Transactional
	public ProductResponse create(CreateProductRequest request) {
		Product product = productRepository.save(new Product(normalizeName(request.name())));
		return new ProductResponse(product.getId(), product.getName());
	}

	@Transactional(readOnly = true)
	public List<ProductResponse> list() {
		return productRepository.findAllByOrderByIdDesc().stream()
				.map(product -> new ProductResponse(product.getId(), product.getName()))
				.toList();
	}
}
