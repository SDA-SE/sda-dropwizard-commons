package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # Jetty HTTP does not affect us, because we do not use HttpURI.
  # See https://github.com/jetty/jetty.project/security/advisories/GHSA-qh8g-58pp-2wxh
  "CVE-2024-6763",
  # Jetty CVE, also see VerifyEffectOfCve20262332IT
  "CVE-2026-2332",
  # Spring Context is just in the background because we use Spring Data MongoDB.
  "CVE-2024-38820",
  # Jetty bound to Jetty 11 on this branch, no upgrade available
  # consumers may upgrade to 11.0.31 with commercial support
  # consumers of this library are likely not affected because we propagate bearer token
  # authentication and not HTTP Digest.
  "CVE-2026-10050",
  # Jetty bound to Jetty 11 on this branch, no upgrade available
  # consumers may upgrade to 11.0.29 with commercial support
  "CVE-2026-6790",
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}