[![](https://jitpack.io/v/ecsoya/bear.svg)](https://jitpack.io/#ecsoya/bear)

# how to use

Step 1. Add the JitPack repository to your build file

```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

Step 2. Add the dependency

```
	<dependency>
	    <groupId>com.github.ecsoya.bear</groupId>
	    <artifactId>bear</artifactId>
	    <version>${release version}</version>
	</dependency>
```

# bear
Springboot developing framework from [RuoYi](https://ruoyi.vip/)

## New Features:
1. Move admin resources and controllers to bear-framework.
2. Refactor index page to admin manager and generator (include demos).
3. Add ControllerAdapter for custom index and login page.
4. Add redis support for cache and session, the default is ehcache.