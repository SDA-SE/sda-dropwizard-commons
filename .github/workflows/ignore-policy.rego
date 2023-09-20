package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # H2 database only used for testing, not in production
  "CVE-2022-45868",
  # Quartz only used in test. Affected method: https://github.com/quartz-scheduler/quartz/issues/943
  "CVE-2023-39017",
  # Flapdoodle is only used in test.
  "CVE-2023-42503"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
