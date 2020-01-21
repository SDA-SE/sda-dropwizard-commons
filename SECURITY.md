# Responsible disclosure policy

## Introduction

We take security very seriously at SDA SE.
We welcome any review of the latest release of all our open source code to ensure that these components can not be compromised.
In case you identified a security related issue with severity of _low_ to _medium_, please create a [GitHub issue](https://github.com/SDA-SE/sda-dropwizard-commons/issues). 


## Security related bugs with severity _high_ or _critical_

In case you identified a security related issue with severity of _high_ or _critical_, please disclose information about the issue non public via email to `opensource-security@sda.se`.

We encourage researchers to include a Proof-of-Concept, supported by screenshots or videos.
For each given security related issue with severity _high_ or _critical_ (based on SDA SE own assessment), we will respond within one week.


# Supported versions and update policy

Please be aware that only the most recent version will be subject of security patches.
The [changelog](https://github.com/SDA-SE/sda-dropwizard-commons/releases/) provides information about feature and security related fixes like patches.


# Security related configuration

The _SecurityBundle_ (`sda-commons-server-security`) hardens the configuration of Dropwizard. The [README](https://github.com/SDA-SE/sda-dropwizard-commons/tree/master/sda-commons-server-security) provides information about addressed risks and how to use the `SecurityBundle`.


# Known security gaps and future enhancements

There is no format in commits to identify security related fixes and it is not planned yet.

We identified that validation of JSON input with annotations in data classes might be too late in terms of deserialization attacks in [`jackson-databind`](https://github.com/FasterXML/jackson-databind).
Therefore, we perform daily vulnerability scans to be informed about known vulnerabilities in `jackson-databind`. We perform a vulnerability assessment as soon as possible and perform actions based on the outcome.
