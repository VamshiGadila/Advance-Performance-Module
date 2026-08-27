package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateCycleRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CycleResponse;
import com.practice.springbootdemo.advance_performance_module.entity.CycleStatus;
import com.practice.springbootdemo.advance_performance_module.entity.PerformanceCycle;
import com.practice.springbootdemo.advance_performance_module.exception.DuplicateResourceException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidPerformanceCycleException;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import com.practice.springbootdemo.advance_performance_module.service.hr.PerformanceCycleService;
import com.practice.springbootdemo.advance_performance_module.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceCycleServiceTest {

    @Mock
    private PerformanceCycleRepository repository;

    @InjectMocks
    private PerformanceCycleService performanceCycleService;

    private PerformanceCycle dynamicCycle;

    @BeforeEach
    void setUp() {
        dynamicCycle = TestDataFactory.createDynamicCycle(CycleStatus.DRAFT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026 Q1 Review", "2026 H2 Appraisal", "Strategic Annual Review"})
    @DisplayName("create: should dynamically create performance review cycles in DRAFT status")
    void createCycle_DynamicNames_Success(String cycleTitle) {

        CreateCycleRequest request = new CreateCycleRequest(
                cycleTitle, "Dynamic Description",
                LocalDate.now(), LocalDate.now().plusMonths(6)
        );
        when(repository.existsByNameIgnoreCase(cycleTitle)).thenReturn(false);
        when(repository.save(any(PerformanceCycle.class))).thenAnswer(i -> {
            PerformanceCycle c = i.getArgument(0);
            c.setId(TestDataFactory.nextId());
            return c;
        });


        CycleResponse response = performanceCycleService.create(request, 1L);


        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(cycleTitle);
        assertThat(response.status()).isEqualTo(CycleStatus.DRAFT);
        verify(repository, times(1)).save(any(PerformanceCycle.class));
    }

    @Test
    @DisplayName("create: should throw InvalidPerformanceCycleException when end date is before start date")
    void createCycle_EndDateBeforeStartDate_ThrowsInvalidPerformanceCycleException() {

        CreateCycleRequest request = new CreateCycleRequest(
                "Invalid Cycle", "Description",
                LocalDate.now().plusMonths(6), LocalDate.now() // Inverted dates
        );


        assertThatThrownBy(() -> performanceCycleService.create(request, 1L))
                .isInstanceOf(InvalidPerformanceCycleException.class)
                .hasMessageContaining("End date cannot be before start date");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create: should throw DuplicateResourceException when cycle name already exists")
    void createCycle_DuplicateName_ThrowsDuplicateResourceException() {

        String existingName = dynamicCycle.getName();
        CreateCycleRequest request = new CreateCycleRequest(
                existingName, "Description",
                LocalDate.now(), LocalDate.now().plusMonths(6)
        );
        when(repository.existsByNameIgnoreCase(existingName)).thenReturn(true);


        assertThatThrownBy(() -> performanceCycleService.create(request, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Performance cycle name already exists");
    }

    @Test
    @DisplayName("activate: should update cycle status to ACTIVE")
    void activateCycle_Success() {

        when(repository.findById(dynamicCycle.getId())).thenReturn(Optional.of(dynamicCycle));
        when(repository.save(any(PerformanceCycle.class))).thenReturn(dynamicCycle);


        CycleResponse response = performanceCycleService.activate(dynamicCycle.getId());


        assertThat(response).isNotNull();
        assertThat(dynamicCycle.getStatus()).isEqualTo(CycleStatus.ACTIVE);
        verify(repository, times(1)).save(dynamicCycle);
    }

    @Test
    @DisplayName("close: should update cycle status to CLOSED")
    void closeCycle_Success() {

        when(repository.findById(dynamicCycle.getId())).thenReturn(Optional.of(dynamicCycle));
        when(repository.save(any(PerformanceCycle.class))).thenReturn(dynamicCycle);


        CycleResponse response = performanceCycleService.close(dynamicCycle.getId());


        assertThat(response).isNotNull();
        assertThat(dynamicCycle.getStatus()).isEqualTo(CycleStatus.CLOSED);
        verify(repository, times(1)).save(dynamicCycle);
    }
}
