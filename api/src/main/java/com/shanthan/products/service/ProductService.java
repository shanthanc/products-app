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

	// "P1", "p1" and " P1 " are all stored as "P1": trimmed and uppercased; whitespace between words is kept.
	// strip() removes only whitespace the request's blank check also rejects, so a valid name never
	// ends up empty. Locale.ROOT keeps the result independent of the server's locale (in Turkish,
	// "pi" would otherwise become "Pİ").
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
