package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # H2 database only used for testing, not in production
  "CVE-2022-45868",
  # Quartz only used in test. Affected method: https://github.com/quartz-scheduler/quartz/issues/943
  "CVE-2023-39017",
  # Flapdoodle is only used in test.
  "CVE-2023-42503",
  # Netty is used by Zookeeper which is only used in test.
  "CVE-2023-4586",
  # Logback is not affected since Dropwizard does not ship logback-receiver
  # see https://github.com/dropwizard/dropwizard/issues/7969
  "CVE-2023-6378"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
