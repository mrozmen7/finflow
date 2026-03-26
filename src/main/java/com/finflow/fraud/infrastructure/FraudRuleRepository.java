package com.finflow.fraud.infrastructure;

import com.finflow.fraud.domain.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link FraudRule} entities.
 */
public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {

    /**
     * Returns all currently active fraud rules.
     *
     * @return list of enabled rules, may be empty
     */
    List<FraudRule> findByEnabledTrue();

    /**
     * Looks up a rule by its unique name.
     *
     * @param name the rule name
     * @return the matching rule, or empty if not found
     */
    Optional<FraudRule> findByName(String name);
}
