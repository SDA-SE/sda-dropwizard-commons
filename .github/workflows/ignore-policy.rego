package trivy

import data.lib.trivy

default ignore = false

# add CVE as String and a comment why it s ignored
ignore_cves := {
  # evaluation issue, 5.4.3 is defined as fixed in https://github.com/advisories/GHSA-hf6x-8p5f-cgmf
  # we are already beyond 5.4.3
  "CVE-2026-54399"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
