package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # Snakeyaml 1.33
  # no update possible because of incompatibilities with current Jackson version
  # unlikely to exploit because yaml should only be used for configuration
  "CVE-2022-1471",
  # H2 database only used for testing, not in production
  "CVE-2022-45868",
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
