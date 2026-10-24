package com.bankcore.account.domain;

/**
 * Account lifecycle state (SRS RF-011 to RF-013, DATABASE-DESIGN #7).
 */
public enum AccountStatus {
    ACTIVA,
    BLOQUEADA,
    CERRADA
}
