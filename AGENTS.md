# HomeSpace Discovery Server — Codex Instructions

<!-- BEGIN: HomeSpace engineering standards -->
## HomeSpace engineering standards

- Preserve business invariants, public contracts and backward compatibility. Keep authentication, authorization, input validation and error handling intact.
- Protect secrets and sensitive data in code, commits, tests, logs and reports. Treat third-party hooks as untrusted until reviewed and explicitly authorized.
- Change production configuration, deploy or run destructive data operations only when explicitly authorized for the target environment.
- Keep review tasks read-only unless the developer requests fixes.
- Run targeted verification first. Broaden coverage when the risk or affected interfaces warrant it. Report checks that ran and any unverified behavior.
<!-- END: HomeSpace engineering standards -->

## Scope

Service discovery and registration infrastructure.

Inspect actual build files and configurations before modification.

## Development

- Preserve service discovery behavior.
- Preserve registration and instance identity conventions.
- Preserve environment configuration compatibility.
- Do not modify unrelated service settings.
- Do not introduce unnecessary dependencies.

## Verification

- Prefer focused configuration validation.
- Run relevant existing tests when applicable.
- Do not start all microservices for a local change.
- Expand verification for registration, connectivity and configuration compatibility changes.

## Safety

Never modify production service discovery settings without authorization.

## Completion

Report configuration changes, verification and operational risks.
