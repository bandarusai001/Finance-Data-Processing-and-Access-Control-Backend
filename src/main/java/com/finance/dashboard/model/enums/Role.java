package com.finance.dashboard.model.enums;

/**
 * Role defines the access level of a user within the system.
 *
 * VIEWER  — read-only access to dashboard and records
 * ANALYST — read access + summary/insights endpoints
 * ADMIN   — full CRUD on records + user management
 */
public enum Role {
    VIEWER,
    ANALYST,
    ADMIN
}
