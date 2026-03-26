package com.finflow.fraud.domain;

/** Lifecycle status of a detected fraud case. */
public enum FraudCaseStatus {
    /** Newly detected, awaiting review. */
    OPEN,
    /** Under active investigation. */
    INVESTIGATING,
    /** Confirmed as fraud and resolved. */
    RESOLVED,
    /** Determined to be a false positive and dismissed. */
    DISMISSED
}
