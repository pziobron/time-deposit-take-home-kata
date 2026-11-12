# Dependency security review

Reviewed on 2026-11-13 (Europe/Warsaw). This is a dependency-advisory review,
not a penetration test or a claim that the application has no security issues.

## Compatible upgrades

The Spring Boot parent remains 3.5.16, the newest available 3.5 patch checked in
Maven Central. Its dependency versions are overridden narrowly in `pom.xml`:

| Component | Before | After |
|---|---|---|
| Embedded Tomcat components | 10.1.55 | 10.1.61 |
| Jackson BOM | 2.21.4 | 2.21.8 |
| PostgreSQL JDBC | 42.7.11 | 42.7.14 |
| Log4j API/bridge, aligned by BOM | 2.24.3 | 2.26.1 |
| Commons Compress (Testcontainers transitive dependency) | 1.24.0 | 1.28.0 |
| Commons Lang (introduced transitively during the upgrade) | 3.17.0 | 3.18.0 |
| Logback classic/core | 1.5.34 | 1.6.6 |
| Commons Codec (test) | 1.18.0 | 1.23.0 |
| JSON Smart / Accessors Smart (test) | 2.5.2 | 2.6.1 |
| ASM (test) | 9.7.1 | 9.11.1 |
| JSONassert's org.json implementation (test) | Unmaintained Android JSON | `org.json:json:20260814` |

Commons Compress is managed by version rather than added as a production
dependency. It and Commons Lang resolve in test scope. Revisit these overrides
when changing the Boot parent so it can manage compatible patched versions again.

The follow-up IntelliJ/Mend findings prompted these additional updates. Android
JSON has no patched release, so it is excluded from the test starter and replaced
with maintained `org.json`, preserving the existing JSONassert comparisons. This
is a parser replacement, not removal of JSON test coverage. The runtime classpath
does not include these test parser/bytecode dependencies. Logback's two modules
remain aligned, and their base class files target Java 11, below our Java 17
baseline.

Primary fix information: [Logback releases](https://logback.qos.ch/news.html),
[ASM releases](https://asm.ow2.io/versions.html),
[Commons Codec advisory](https://www.cve.org/CVERecord?id=CVE-2026-119078),
[Android JSON advisory](https://www.cve.org/CVERecord?id=CVE-2026-110974), and
[JSON Smart upstream issue](https://github.com/netplex/json-smart-v2/issues/302).
For Codec, the fix corrects Blake3 documentation rather than changing hashing
behavior; this project has no Blake3 key-derivation code.

## Results and remaining findings

The baseline OSV scan matched 24 advisory records across seven packages among 90
resolved package/version pairs. After upgrades, a new resolved-tree scan checked
94 pairs, including runtime, provided, and test dependencies, and matched only two
advisories, both against `org.springframework:spring-webmvc:6.2.19`:

- [CVE-2026-47884 / GHSA-pc63-qcmh-9cmg](https://spring.io/security/cve-2026-47884/):
  XSLT view rendering with an implicit view name and a wildcard mapping.
  This application has explicit REST mappings returning JSON and no XSLT views.
- [CVE-2026-47890 / GHSA-j9f9-w8pj-32f8](https://spring.io/security/cve-2026-47890/):
  SSE stream corruption while rendering view fragments. This application does not
  use SSE or render view fragments.

These are the OSV package/version matches, not an exhaustive finding count across
all vulnerability databases. The additional Mend advisories were checked against
CVE Program records and Spring's own advisories rather than dismissed because
OSV did not report them.

### IntelliJ/Mend follow-up

After the follow-up upgrades, a fresh IntelliJ inspection of `pom.xml` reports
only Spring Framework 6.2.19 findings: 13 distinct CVEs, with `spring-beans`
repeated under two dependency paths. The supplied non-Spring findings no longer
appear. No claim is made that every possible Mend check or vulnerability is cleared.

| CVE (vendor advisory) | Required feature / current applicability assessment |
|---|---|
| [2026-59282](https://spring.io/security/cve-2026-59282/) | Untrusted property-path binding into a self-populating list. No controller binds request models or accepts property paths. |
| [2026-59283](https://spring.io/security/cve-2026-59283/) | Compiled SpEL with `SimpleEvaluationContext`. No application expression parser/compiler is configured. |
| [2026-47886](https://spring.io/security/cve-2026-47886/) | Evaluation of untrusted SpEL with large numeric powers. No user expressions are evaluated. |
| [2026-47891](https://spring.io/security/cve-2026-47891/) | WebFlux/Aalto XML decoding. This is a servlet MVC application without that decoder. |
| [2026-47889](https://spring.io/security/cve-2026-47889/) | Jetty reactive cookie response handling. This application uses servlet Tomcat. |
| [2026-47885](https://spring.io/security/cve-2026-47885/) | WebFlux multipart part-event streaming. No reactive multipart endpoints exist. |
| [2026-47883](https://spring.io/security/cve-2026-47883/) | Broadly configured `UrlHandlerFilter` redirects. No such filter is configured. |
| [2026-59281](https://spring.io/security/cve-2026-59281/) | Rendering escaped data-binding field errors as HTML. Endpoints return JSON and do not render binding errors. |
| [2026-59314](https://spring.io/security/cve-2026-59314/) | Untrusted filenames used in Content-Disposition response/outbound multipart headers. The application builds neither. |
| [2026-47884](https://spring.io/security/cve-2026-47884/) | XSLT view rendering. No XSLT views. |
| [2026-47890](https://spring.io/security/cve-2026-47890/) | SSE view fragments. No SSE/view fragments. |
| [2026-59313](https://spring.io/security/cve-2026-59313/) | Functional MVC SSE responses. No functional MVC/SSE endpoints. |
| [2026-47887](https://spring.io/security/cve-2026-47887/) | `UrlFileNameViewController` redirects. No such controller is configured. |

These remain affected-version findings with no documented vulnerable path
identified in current application code/configuration. They are not fixed merely
because their prerequisites are absent, and adding those features requires
reassessment. No warning/advisory suppressions were introduced. Spring lists the 6.2.20 fixes as enterprise-only and 7.0.9 as an OSS
fix. A compatible Spring Boot 4 migration is a separate change. Do not override
only `spring-webmvc` to 7.x inside Boot 3.5.

`mvn clean verify` passed all 55 tests after the final upgrades, including the
PostgreSQL HTTP, transaction rollback, and concurrency tests. IntelliJ's build
also passed. No business rules, API contracts, or shared model signatures changed.

## Scan method and limits

Maven Dependency Plugin 3.8.1 exported the resolved dependency tree. Each distinct
Maven package name and version was queried through OSV's public
[`querybatch` API](https://google.github.io/osv.dev/post-v1-querybatch/). The response
contained a result for every query and no unprocessed pagination tokens. The raw
post-upgrade result is generated at `target/security/osv-after-upgrades.json`.
Generated reports are ignored and are removed by `mvn clean`.

To regenerate the resolved inventory from `java/`:

```sh
mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:tree \
  -DoutputType=json -DoutputFile=target/security/dependency-tree.json
```

Known-advisory matching is not automatic proof of exploitability. The scan does
not cover container image OS packages, JDK issues, build-plugin dependencies,
secrets, or unknown/unreported vulnerabilities.

The earlier OWASP Dependency-Check attempt failed to initialize NVD data because
no API key or usable cache was available. It produced no usable assessment and
was not treated as a clean scan. To retry with `NVD_API_KEY` set in your environment:

```sh
mvn org.owasp:dependency-check-maven:13.0.0:check \
  -DnvdApiKeyEnvironmentVariable=NVD_API_KEY \
  -Dformats=HTML,JSON -DskipTestScope=false -DfailBuildOnCVSS=7
```

Do not commit API keys or suppress findings solely to make a report green.
