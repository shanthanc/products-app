package com.shanthan.products.product;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@PostMapping
	public ProductResponse create(@RequestBody CreateProductRequest request) {
		throw new UnsupportedOperationException("TODO S1.1");
	}

	@GetMapping
	public List<ProductResponse> list() {
		throw new UnsupportedOperationException("TODO S2.1");
	}
}
