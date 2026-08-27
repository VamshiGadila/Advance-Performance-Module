package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateDepartmentRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.DepartmentResponse;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.DuplicateResourceException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.service.hr.DepartmentService;
import com.practice.springbootdemo.advance_performance_module.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private User dynamicManager;
    private Department dynamicDepartment;

    @BeforeEach
    void setUp() {
        dynamicManager = TestDataFactory.createDynamicUser(Role.MANAGER);
        dynamicDepartment = Department.builder()
                .id(TestDataFactory.nextId())
                .name("Engineering Core")
                .description("Core Backend and Systems")
                .defaultManagerId(dynamicManager.getId())
                .build();
        dynamicManager.setDepartmentId(dynamicDepartment.getId());
    }

    @Test
    @DisplayName("create: should create and return new department successfully")
    void createDepartment_Success() {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "Cloud Platform", "Infrastructure and DevOps", dynamicManager.getId()
        );

        when(departmentRepository.existsByNameIgnoreCase("Cloud Platform")).thenReturn(false);
        when(userRepository.findById(dynamicManager.getId())).thenReturn(Optional.of(dynamicManager));
        when(departmentRepository.save(any(Department.class))).thenAnswer(i -> {
            Department d = i.getArgument(0);
            d.setId(TestDataFactory.nextId());
            return d;
        });
        when(userRepository.findByDepartmentIdAndRoleAndActiveTrue(anyLong(), eq(Role.EMPLOYEE)))
                .thenReturn(Collections.emptyList());

        DepartmentResponse response = departmentService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Cloud Platform");
        assertThat(response.defaultManagerId()).isEqualTo(dynamicManager.getId());
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    @DisplayName("create: should throw DuplicateResourceException when department name already exists")
    void createDepartment_DuplicateName_ThrowsDuplicateResourceException() {
        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "Engineering Core", "Duplicate dept", null
        );
        when(departmentRepository.existsByNameIgnoreCase("Engineering Core")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Department already exists: Engineering Core");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("setDefaultManager: should successfully update default manager for department")
    void setDefaultManager_Success() {
        when(departmentRepository.findById(dynamicDepartment.getId())).thenReturn(Optional.of(dynamicDepartment));
        when(userRepository.findById(dynamicManager.getId())).thenReturn(Optional.of(dynamicManager));
        when(departmentRepository.save(any(Department.class))).thenReturn(dynamicDepartment);
        when(userRepository.findByDepartmentIdAndRoleAndActiveTrue(dynamicDepartment.getId(), Role.EMPLOYEE))
                .thenReturn(Collections.emptyList());

        DepartmentResponse response = departmentService.setDefaultManager(dynamicDepartment.getId(), dynamicManager.getId());

        assertThat(response).isNotNull();
        assertThat(response.defaultManagerId()).isEqualTo(dynamicManager.getId());
        verify(departmentRepository, times(1)).save(dynamicDepartment);
    }

    @Test
    @DisplayName("setDefaultManager: should throw BadRequestException when user is not a manager")
    void setDefaultManager_NonManagerUser_ThrowsBadRequestException() {
        User employee = TestDataFactory.createDynamicUser(Role.EMPLOYEE);
        when(departmentRepository.findById(dynamicDepartment.getId())).thenReturn(Optional.of(dynamicDepartment));
        when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> departmentService.setDefaultManager(dynamicDepartment.getId(), employee.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Selected user is not a manager");
    }

    @Test
    @DisplayName("setDefaultManager: should throw BadRequestException when manager belongs to different department")
    void setDefaultManager_MismatchedDepartment_ThrowsBadRequestException() {
        User outsiderManager = TestDataFactory.createDynamicUser(Role.MANAGER);
        outsiderManager.setDepartmentId(TestDataFactory.nextId() + 999); // Different department

        when(departmentRepository.findById(dynamicDepartment.getId())).thenReturn(Optional.of(dynamicDepartment));
        when(userRepository.findById(outsiderManager.getId())).thenReturn(Optional.of(outsiderManager));

        assertThatThrownBy(() -> departmentService.setDefaultManager(dynamicDepartment.getId(), outsiderManager.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to this department");
    }

    @Test
    @DisplayName("getOrThrow: should throw ResourceNotFoundException when department is not found")
    void getOrThrow_NotFound_ThrowsResourceNotFoundException() {
        long invalidDeptId = TestDataFactory.nextId() + 5000;
        when(departmentRepository.findById(invalidDeptId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getOrThrow(invalidDeptId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found with ID: " + invalidDeptId);
    }
}
