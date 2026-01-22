package com.shopping.onlineshop.repository;

import com.shopping.onlineshop.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyRepo extends JpaRepository<Product,Long> {
}
