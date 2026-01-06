package menuorderingapp.project.testcase;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import menuorderingapp.project.model.Category;
import menuorderingapp.project.model.dto.CategoryRequest;
import menuorderingapp.project.repository.CategoryRepository;
import menuorderingapp.project.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Menu Category Test")
class CategoryTestCase {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private MenuServiceImpl menuService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Task 1: Add new category with valid data")
    void testAddCategory_Success() {
        CategoryRequest request = new CategoryRequest("Main Course", 1);
        Category category = new Category(request.getName(), request.getDisplayOrder());

        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = menuService.saveCategory(category);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Main Course");
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Task 2: Add category with blank fields")
    void testAddCategory_Fail_BlankFields() {
        CategoryRequest request = new CategoryRequest("", null);

        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .map(ConstraintViolation::getMessage)
                .anyMatch(msg -> msg.contains("Category name is required"));
    }

    @Test
    @DisplayName("Task 3: Delete existing category")
    void testDeleteCategory_Success() {
        Long categoryId = 1L;
        Category category = new Category("To Delete", 2);
        category.setId(categoryId);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        doNothing().when(categoryRepository).delete(category);

        menuService.deleteCategory(categoryId);

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Task 4: Add category with whitespace name")
    void testAddCategory_Fail_WhitespaceName() {
        CategoryRequest request = new CategoryRequest(" ", 99); // 99 as "after last existing"

        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    @DisplayName("Task 5: Add category with '-1' as name")
    void testAddCategory_Fail_NegativeOneName() {
        CategoryRequest request = new CategoryRequest("-1", 99);

        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);

        assertThat(violations)
                .withFailMessage("Expected validation error for name '-1', but got success (Bug Reproduced)")
                .isNotEmpty();
    }
}
