package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # H2 database only used for testing, not in production
  "CVE-2022-45868",
  # from org.yaml:snakeyaml:1.33 to :2.0 would be a breaking update in release 2.x.x
  "CVE-2022-1471"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
