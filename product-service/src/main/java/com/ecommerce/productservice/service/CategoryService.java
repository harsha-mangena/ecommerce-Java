package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryDto;
import com.ecommerce.productservice.dto.CategoryRequest;
import com.ecommerce.productservice.dto.PagedResponse;

import java.util.List;

public interface CategoryService {
    
    CategoryDto createCategory(CategoryRequest categoryRequest);
    
    CategoryDto updateCategory(Long id, CategoryRequest categoryRequest);
    
    CategoryDto getCategoryById(Long id);
    
    PagedResponse<CategoryDto> getAllCategories(int page, int size);
    
    List<CategoryDto> getRootCategories();
    
    List<CategoryDto> getSubcategories(Long parentId);
    
    void deleteCategory(Long id);
}
