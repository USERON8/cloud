# User schema migration runbook

The user-service owns the Flyway history for `user_db`. The existing `db/init/user-service/init.sql` remains the local cold-start provisioner; after those base tables exist, Flyway upgrades them from baseline version `0`.

Before an existing environment is upgraded:

1. Stop user-service writers and take a consistent `user_db` backup or storage snapshot.
2. Run `audit-user-identity.sql` and `audit-user-domain.sql`. Both must return zero rows.
3. Start exactly one updated user-service instance and wait for Flyway to reach the expected version before rolling out the remaining instances.
4. Verify `flyway_schema_history`, service health, principal links and the new generated unique columns.

These migrations contain MySQL DDL and are not transactionally reversible. Recovery means stopping writers, restoring the pre-migration snapshot, and redeploying the previous application version. Do not use `flyway clean`; it is disabled in application configuration.

For a new local environment, provision the base tables with the repository init scripts and then start user-service. An empty database without the base schema is intentionally not treated as a production provisioning path in this compatibility phase.
