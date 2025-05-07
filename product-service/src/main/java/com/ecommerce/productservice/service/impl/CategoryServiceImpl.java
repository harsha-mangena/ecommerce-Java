package com.ecommerce.productservice.service.impl;

import com.ecommerce.productservice.dto.CategoryDto;
import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.PagedResponse;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.exception.ResourceAlreadyExistsException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    @Transactional
    public CategoryDto createCategory(CategoryRequest categoryRequest) {
        // Check if category with same name already exists
        if (categoryRepository.findByName(categoryRequest.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Category", "name", categoryRequest.getName());
        }
        
        Category category = new Category();
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        
        // Set parent category if provided
        if (categoryRequest.getParentId() != null) {
            Category parentCategory = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryRequest.getParentId()));
            category.setParent(parentCategory);
        }
        
        Category savedCategory = categoryRepository.save(category);
        return mapCategoryToDto(savedCategory);
    }
    
    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        
        // Check if another category with the same name exists (excluding current category)
        categoryRepository.findByName(categoryRequest.getName())
                .ifPresent(existingCategory -> {
                    if (!existingCategory.getId().equals(id)) {
                        throw new ResourceAlreadyExistsException("Category", "name", categoryRequest.getName());
                    }
                });
        
        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        
        // Update parent category if provided
        if (categoryRequest.getParentId() != null) {
            // Prevent circular references
            if (categoryRequest.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            
            Category parentCategory = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryRequest.getParentId()));
            category.setParent(parentCategory);
        } else {
            category.setParent(null);
        }
        
        Category updatedCategory = categoryRepository.save(category);
        return mapCategoryToDto(updatedCategory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapCategoryToDto(category);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CategoryDto> getAllCategories(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Category> categories = categoryRepository.findAll(pageable);
        
        List<CategoryDto> categoryDtos = categories.getContent().stream()
                .map(this::mapCategoryToDto)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(
                categoryDtos,
                categories.getNumber(),
                categories.getSize(),
                categories.getTotalElements(),
                categories.getTotalPages(),
                categories.isLast()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getRootCategories() {
        List<Category> rootCategories = categoryRepository.findRootCategories();
        return rootCategories.stream()
                .map(this::mapCategoryToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getSubcategories(Long parentId) {
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", parentId));
        
        List<Category> subcategories = categoryRepository.findByParentId(parentId);
        return subcategories.stream()
                .map(this::mapCategoryToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        
        // Check if category has subcategories
        if (!category.getSubcategories().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with subcategories. Remove subcategories first.");
        }
        
        // Check if category has associated products
        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated products. Update or remove products first.");
        }
        
        categoryRepository.delete(category);
    }
    
    // Helper method to map Category entity to CategoryDto
    private CategoryDto mapCategoryToDto(Category category) {
        List<CategoryDto> subcategoryDtos = new ArrayList<>();
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            subcategoryDtos = category.getSubcategories().stream()
                    .map(subcategory -> CategoryDto.builder()
                            .id(subcategory.getId())
                            .name(subcategory.getName())
                            .description(subcategory.getDescription())
                            .parentId(category.getId())
                            .build())
                    .collect(Collectors.toList());
        }
        
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .subcategories(subcategoryDtos)
                .build();
    }
}
