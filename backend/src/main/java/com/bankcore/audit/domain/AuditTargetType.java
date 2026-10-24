package com.bankcore.audit.domain;

/**
 * Polymorphic target categories used by audit events (DATABASE-DESIGN #23).
 */
public enum AuditTargetType {
    USER,
    ACCOUNT,
    TRANSFER
}
