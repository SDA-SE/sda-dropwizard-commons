package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
