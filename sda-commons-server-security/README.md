# SDA Commons Server Security

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-server-security/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-server-security)

The module `sda-commons-server-security` helps to configure a secure Dropwizard application according to the 
recommendations "Härtungsmaßnahmen Dropwizard" available at the internal wiki entry
by Timo Pagel.

Currently the [`SecurityBundle`](./src/main/java/org/sdase/commons/server/security/SecurityBundle.java) addresses

- Risk: Utilization of HTTP Methods
 -> HTTP offers several methods to perform actions on a web server. Some of the methods are designed to help developers deploy and test HTTP applications. If a Web server is incorrectly configured, attackers can take advantage of this. For example, cross-site tracing by attackers can be used to determine user data. Cross-site tracing involves cross-site scripting to capture advanced information via the HTTP TRACE method. For example, a cookie protected by HTTP-Only.
- Risk: Root Start
 -> If Dropwizard is started with extended privileges as root user, an attacker can  attack more easily the operating system after taking over the container.
- Risk: Loss of source IP address
 -> After detecting an attack, the source IP address is often blocked first. To do this, the source IP address must be known. If a proxy (e.g. a load balancer) is used before the application, the IP address of the proxy is often specified as the source IP.
- Risk: Detection of confidential components
 -> Attackers attempt to determine the components of an application that are in use and then determine the versions that belong to them. Then vulnerabilities in the detected components of the found versions are exploited. The version number is often sent in error pages or via HTTP headers from servers and applications.
- Risk: Buffer Overflow
 -> Attackers use buffer overflow to corrupt the execution stack. Attackers send prepared packets to a web application to execute malicious code.
- Risk: Clickjacking
 -> Clickjacking integrates an existing website, such as www.sda-se.de, into another malicious website (e.g. www.sda-se.de.xx). The malicious website inserts a transparent layer (technical IFrame) over the integration of the page www.sda-se.de. In addition, the malicious website installed a key logger that records all mouse movements and keystrokes of a victim and sends them to the attacker. URLs such as www.sda-se.de.xx are often distributed via phishing to obtain user access data.
- Risk: Interpretation of content by the browser
 -> Attackers can attempt to upload files to a server, which are then delivered to other users. If the browser of the user/victim interprets the uploaded file, the content type can be misinterpreted. For example, an attacker can hide and upload Javascript code in images, which is then executed in the victim's browser.
- Risk: Cross Site Scripting (XSS)
 -> Attackers attempt to manipulate external resources, such as the content on content delivery services, to execute malicious code on users' browsers. In addition, attackers try to execute malicious JavaScript code via cross-site scripting (XSS) on users' browsers.
- Risk: Passing on visited URLs to third parties
 -> In the World Wide Web, referrer refers to the website via which the user accessed the current website or file. In the case of an HTTP request (e.g. a website or an image), the web browser sends the URL of the original website to the web server. The referrer is an optional part of the HTTP request sent to the web server. Although optional, the transmission is preset for all common browsers. Only if the current page is to be retrieved via HTTPS and the page to be retrieved is to be transmitted via HTTP should the referrer not be transmitted. If, on the other hand, the page to be retrieved is also transmitted via HTTPS, the referrer is transmitted independently of the host.
- Risk: Reloading content into Flash and PDFs
 -> Flash and PDFs can reload content from other domains. Attackers can use this to reload malicious code.

## Usage

Just add the [`SecurityBundle`](./src/main/java/org/sdase/commons/server/security/SecurityBundle.java) to the 
application to avoid known risks:

```java
public class MyApp extends Application<MyConfiguration> {
   @Override
   public void initialize(Bootstrap<MyConfiguration> bootstrap) {
      // ...
      bootstrap.addBundle(SecurityBundle.builder().build());
      bootstrap.addBundle(JacksonConfigurationBundle.builder().build()); // enables required custom error handlers
      // ...
   }

   @Override
   public void run(MyConfiguration configuration, Environment environment) {
      // ...
   }
}
``` 