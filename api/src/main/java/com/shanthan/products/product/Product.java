package com.shanthan.products.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private UUID id;

	@Column(nullable = false)
	private String name;

	protected Product() {
		// required by JPA
	}

	public Product(String name) {
		throw new UnsupportedOperationException("TODO S1.1");
	}

	public UUID getId() {
		throw new UnsupportedOperationException("TODO S1.1");
	}

	public String getName() {
		throw new UnsupportedOperationException("TODO S1.1");
	}
}
