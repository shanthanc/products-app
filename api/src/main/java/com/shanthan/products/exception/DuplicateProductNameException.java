package com.shanthan.products.exception;

public class DuplicateProductNameException extends RuntimeException {

	public DuplicateProductNameException(String name, Throwable cause) {
		super("A product named " + name + " already exists.", cause);
	}
}
