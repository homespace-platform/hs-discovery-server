---
name: discovery-config-review
description: Review changes to HomeSpace Eureka setup, client registration compatibility, health probes and deployment wiring. Use when a discovery configuration or startup review is requested.
---

# Discovery Configuration Review

Use the requested diff as the review scope. Inspect `src/main/resources/application.properties`, `compose.production.yml` and `deploy/dokploy.md` where relevant.

1. Check that the standalone Eureka server keeps self-registration and registry fetching disabled. Review registry paths, DNS, ports and environment overrides for compatibility with existing clients.
2. Trace port, readiness and liveness expectations through configuration and service health checks. Preserve health status mapping and the narrow Actuator endpoint exposure.
3. Identify configuration changes that need coordinated client verification. Review checked-in configuration first. Compare environment variable names without reading or disclosing secret values.
4. Use the Maven wrapper for a focused compile or existing test when it gives evidence. If startup verification is needed, scope it to an authorized local environment. Keep the review read-only.

Report actionable findings with file locations, a concrete failure scenario and any unverified runtime assumptions.
