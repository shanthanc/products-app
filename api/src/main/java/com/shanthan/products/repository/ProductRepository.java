package com.shanthan.products.repository;

import com.shanthan.products.entity.Product;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

	// Ids are UUID v7, which start with their creation time, so the highest id is the newest product.
	List<Product> findAllByOrderByIdDesc();
}
