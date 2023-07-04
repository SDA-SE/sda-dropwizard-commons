package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # H2 database only used for testing, not in production
  "CVE-2022-45868",
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
