package com.example.demo.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductImage;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository imageRepository) {

        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageRepository = imageRepository;
    }

    // Get all products or filter by category
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(String categoryName) {

        List<Product> products;

        if (categoryName != null && !categoryName.isBlank()) {

            Category category = categoryRepository
                    .findByCategoryName(categoryName)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Category not found"
                            )
                    );

            products = productRepository
                    .findByCategory_CategoryId(
                            category.getCategoryId()
                    );

        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(this::toProductResponse)
                .toList();
    }

    // Get product by ID
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer productId) {

        Product product = findProduct(productId);

        return toProductResponse(product);
    }

    // Add product
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        Category category = findCategory(request.categoryId());

        Product product = new Product();

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        saveProductImages(savedProduct, request.imageUrls());

        return toProductResponse(savedProduct);
    }

    // Update product
    @Transactional
    public ProductResponse updateProduct(
            Integer productId,
            ProductRequest request) {

        Product product = findProduct(productId);

        Category category = findCategory(request.categoryId());

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);

        imageRepository.deleteByProduct_ProductId(productId);

        saveProductImages(updatedProduct, request.imageUrls());

        return toProductResponse(updatedProduct);
    }

    // Delete product
    @Transactional
    public void deleteProduct(Integer productId) {

        Product product = findProduct(productId);

        imageRepository.deleteByProduct_ProductId(productId);

        productRepository.delete(product);
    }

    // Create category
    @Transactional
    public Category createCategory(CategoryRequest request) {

        if (categoryRepository.existsByCategoryName(
                request.categoryName())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category already exists"
            );
        }

        Category category = new Category(
                request.categoryName()
        );

        return categoryRepository.save(category);
    }

    // Get all categories
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Delete category if it has no products
    @Transactional
    public void deleteCategory(Integer categoryId) {

        Category category = findCategory(categoryId);

        if (productRepository.existsByCategory_CategoryId(categoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a category containing products"
            );
        }

        categoryRepository.delete(category);
    }

    // Find product
    private Product findProduct(Integer productId) {

        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }

    // Find category
    private Category findCategory(Integer categoryId) {

        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Category not found"
                        )
                );
    }

    // Save product image URLs
    private void saveProductImages(
            Product product,
            List<String> imageUrls) {

        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<ProductImage> images = imageUrls.stream()
                .map(url -> new ProductImage(product, url))
                .toList();

        imageRepository.saveAll(images);
    }

    // Convert Product entity to response DTO
    private ProductResponse toProductResponse(Product product) {

        List<String> images = imageRepository
                .findByProduct_ProductId(product.getProductId())
                .stream()
                .map(ProductImage::getImageUrl)
                .toList();

        Category category = product.getCategory();

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                category != null ? category.getCategoryId() : null,
                category != null ? category.getCategoryName() : null,
                images,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}