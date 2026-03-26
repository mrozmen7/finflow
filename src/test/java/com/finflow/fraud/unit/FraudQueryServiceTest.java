package com.finflow.fraud.unit;

import com.finflow.fraud.application.FraudQueryService;
import com.finflow.fraud.domain.FraudCase;
import com.finflow.fraud.domain.FraudCaseStatus;
import com.finflow.fraud.infrastructure.FraudCaseRepository;
import com.finflow.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudQueryService")
class FraudQueryServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @InjectMocks
    private FraudQueryService fraudQueryService;

    private FraudCase buildOpenCase() {
        return new FraudCase(UUID.randomUUID(), UUID.randomUUID(),
            "HIGH_AMOUNT", 80, "Amount exceeds threshold");
    }

    @Nested
    @DisplayName("Query Cases")
    class QueryCases {

        @Test
        @DisplayName("should return all cases as a page")
        void should_ReturnAllCases() {
            FraudCase fraudCase = buildOpenCase();
            Pageable pageable = PageRequest.of(0, 20);
            when(fraudCaseRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(fraudCase)));

            Page<FraudCase> result = fraudQueryService.getAllCases(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getRuleViolated()).isEqualTo("HIGH_AMOUNT");
        }

        @Test
        @DisplayName("should return case by ID when it exists")
        void should_ReturnCaseById() {
            UUID id = UUID.randomUUID();
            FraudCase fraudCase = buildOpenCase();
            when(fraudCaseRepository.findById(id)).thenReturn(Optional.of(fraudCase));

            FraudCase result = fraudQueryService.getCaseById(id);

            assertThat(result).isSameAs(fraudCase);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when case does not exist")
        void should_ThrowException_When_CaseNotFound() {
            UUID id = UUID.randomUUID();
            when(fraudCaseRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fraudQueryService.getCaseById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
        }
    }

    @Nested
    @DisplayName("Resolve and Dismiss")
    class ResolveAndDismiss {

        @Test
        @DisplayName("should resolve open fraud case and persist updated status")
        void should_ResolveCase() {
            UUID id = UUID.randomUUID();
            FraudCase fraudCase = buildOpenCase();
            when(fraudCaseRepository.findById(id)).thenReturn(Optional.of(fraudCase));
            when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(i -> i.getArgument(0));

            FraudCase result = fraudQueryService.resolveCase(id, "admin");

            assertThat(result.getStatus()).isEqualTo(FraudCaseStatus.RESOLVED);
            assertThat(result.getResolvedBy()).isEqualTo("admin");
            assertThat(result.getResolvedAt()).isNotNull();
            verify(fraudCaseRepository).save(fraudCase);
        }

        @Test
        @DisplayName("should dismiss open fraud case and persist updated status")
        void should_DismissCase() {
            UUID id = UUID.randomUUID();
            FraudCase fraudCase = buildOpenCase();
            when(fraudCaseRepository.findById(id)).thenReturn(Optional.of(fraudCase));
            when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(i -> i.getArgument(0));

            FraudCase result = fraudQueryService.dismissCase(id, "reviewer");

            assertThat(result.getStatus()).isEqualTo(FraudCaseStatus.DISMISSED);
            assertThat(result.getResolvedBy()).isEqualTo("reviewer");
            assertThat(result.getResolvedAt()).isNotNull();
            verify(fraudCaseRepository).save(fraudCase);
        }
    }
}
