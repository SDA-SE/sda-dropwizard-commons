package trivy

import data.lib.trivy

default ignore = false

ignore_cves := {
  # Jetty HTTP does not affect us, because we do not use HttpURI.
  # See https://github.com/jetty/jetty.project/security/advisories/GHSA-qh8g-58pp-2wxh
  "CVE-2024-6763",
  # Spring Context is just in the background because we use Spring Data MongoDB.
  "CVE-2024-38820",
  # lz4-java is only used by Kafka. And they staid there code is not affected under Linux, besides some corner cases.
  # https://issues.apache.org/jira/browse/KAFKA-19951?focusedCommentId=18042357&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-18042357
  "CVE-2025-12183",
  "CVE-2025-66566",
  # Vulnability only occurs with AssertJ and specific XML related functions, which are not used.
  "CVE-2026-24400"
}

ignore {
  input.VulnerabilityID == ignore_cves[_]
}
