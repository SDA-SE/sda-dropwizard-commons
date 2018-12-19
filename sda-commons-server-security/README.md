# SDA Commons Server Security

The module `sda-commons-server-security` helps to configure a secure Dropwizard application according to the 
recommendations 
"[Härtungsmaßnahmen Dropwizard](https://sda-se.atlassian.net/wiki/spaces/platform/pages/686718998/H+rtungsma+nahmen+Dropwizard)"
by Timo Pagel.

Currently the [`SecurityBundle`](./src/main/java/org/sdase/commons/server/security/SecurityBundle.java) addresses

- [Risiko: Ausnutzung von HTTP-Methoden](https://sda-se.atlassian.net/wiki/spaces/platform/pages/686718998/H+rtungsma+nahmen+Dropwizard#H%C3%A4rtungsma%C3%9FnahmenDropwizard-Risiko:AusnutzungvonHTTP-Methoden)
- [Risiko: Root-Start](https://sda-se.atlassian.net/wiki/spaces/platform/pages/686718998/H+rtungsma+nahmen+Dropwizard#H%C3%A4rtungsma%C3%9FnahmenDropwizard-Risiko:Root-Start)
- [Risiko: Verlust der Quell-IP-Adresse](https://sda-se.atlassian.net/wiki/spaces/platform/pages/686718998/H+rtungsma+nahmen+Dropwizard#H%C3%A4rtungsma%C3%9FnahmenDropwizard-Risiko:VerlustderQuell-IP-Adresse)
- [Risiko: Erkennung von vertraulichen Komponenten](https://sda-se.atlassian.net/wiki/spaces/platform/pages/686718998/H+rtungsma+nahmen+Dropwizard#H%C3%A4rtungsma%C3%9FnahmenDropwizard-Risiko:ErkennungvonvertraulichenKomponenten)
- [Risiko: Buffer Overflow](https://sda-se.atlassian.net/wiki/spaces/platform/pages/686718998/H+rtungsma+nahmen+Dropwizard#H%C3%A4rtungsma%C3%9FnahmenDropwizard-Risiko:BufferOverflow)

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