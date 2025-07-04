package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # Jetty HTTP does not affect us, because we do not use HttpURI.
  # See https://github.com/jetty/jetty.project/security/advisories/GHSA-qh8g-58pp-2wxh
  "CVE-2024-6763",
  # Spring Context is just in the background because we use Spring Data MongoDB.
  "CVE-2024-38820"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
