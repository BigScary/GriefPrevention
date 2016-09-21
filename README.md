### Adding GriefPrevention as a gradle/maven/sbt/leiningen dependency

Apparently there's this neat thing called JitPack that makes a public maven repo for public Github repos on the fly.
According to it, this is all you need to do to add this to your maven project:

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

```xml
	<dependency>
	    <groupId>com.github.TechFortress</groupId>
	    <artifactId>GriefPrevention</artifactId>
	    <version>master-SNAPSHOT</version>
	</dependency>
```

You can also add it to gradle/sbt/leiningen projects: https://jitpack.io/#TechFortress/GriefPrevention/master-SNAPSHOT
