package org.example.core.service;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class AdminMutationLock {
    private final JdbcTemplate jdbc;
    // Serialize role, course ownership and group assignment edits in their transaction.
    public void acquire() { jdbc.execute("SELECT pg_advisory_xact_lock(732001)"); }
}
