# JBang Catalog

Collection of JBang scripts related to Quarkus and its subprojects.

### Using from the Quarkus CLI

**Requires**: Quarkus CLI 3.0.1.Final or higher

The scripts can added to the Quarkus CLI in the form of plugins, by replacing the `quarkusio` catalog of jbang with this one.

```sh
jbang catalog remove quarkusio
jbang catalog add --name=quarkusio https://github.com/iocanel/jbang-catalog/blob/HEAD/jbang-catalog.json
quarkus plug list --installable
```

This should return in something like:

```
❯ quarkus plug list --installable
    Name                   	 Type  	 Scope 	 Location                                 	 Description 	
 *  explain                	 jbang 	 user  	 quarkus-explain                          	             	
    create-from-quickstart 	 jbang 	 user  	 quarkus-create-from-quickstart@quarkusio 	             	
    create-from-github     	 jbang 	 user  	 quarkus-create-from-github@quarkusio     	             	

Use the 'plugin add' sub command and pass the location of any plugin listed above, or any remote location in the form of URL / GACTV pointing to a remote plugin.
```

**Note**: A simpler way to [enable 3rd party jbang catalogs](https://github.com/quarkusio/quarkus/issues/33205) is currently in the works.

## create-from-github

Allows creating a project from an existing github repository, optionally specifying the maven coordinates of the new project.

### Enabling the extension

```sh
❯ quarkus plug add create-from-github
Added plugin:
    Name               	 Type  	 Scope   	 Location                             	 Description 	
 *  create-from-github 	 jbang 	 project 	 quarkus-create-from-github@quarkusio
```

### Usage

```sh
❯ quarkus create-from-github iocanel/quarkus-helloworld com.iocanel:my-helloworld:1.0-SNAPSHOT
Created project: my-helloworld from repository:quarkus-helloworld

❯ tree my-helloworld
my-helloworld
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── docker
    │   │   ├── Dockerfile.jvm
    │   │   ├── Dockerfile.legacy-jar
    │   │   ├── Dockerfile.native
    │   │   └── Dockerfile.native-micro
    │   ├── java
    │   │   └── com
    │   │       └── iocanel
    │   │           └── GreetingResource.java
    │   └── resources
    │       ├── application.properties
    │       └── META-INF
    │           └── resources
    │               └── index.html
    └── test
        └── java
            └── com
                └── iocanel
                    ├── GreetingResourceIT.java
                    └── GreetingResourceTest.java

14 directories, 13 files
```

## create-from-quickstart

Similar to `create-from-github` but uses a fixed repository `quarkusio/quarkus-quickstarts` and a configurable quickstart name which is checked out using sparse checkout (to limit the amount of data transferred).

### Enabling the extension

```sh
❯ quarkus plug add create-from-quickstart
Added plugin:
    Name                   	 Type  	 Scope 	 Location                                 	 Description 	
 *  create-from-quickstart 	 jbang 	 user  	 quarkus-create-from-quickstart@quarkusio 	             	

```

### Usage

```sh
❯ quarkus create-from-quickstart hibernate-orm-panache-quickstart com.iocanel:hibernate-exmaple:1.0-SNAPSHOT
Created project: hibernate-exmaple from repository:quarkus-quickstarts

❯ tree hibernate-exmaple
hibernate-exmaple
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── docker
    │   │   ├── Dockerfile.jvm
    │   │   ├── Dockerfile.legacy-jar
    │   │   ├── Dockerfile.native
    │   │   └── Dockerfile.native-micro
    │   ├── java
    │   │   └── org
    │   │       └── acme
    │   │           └── hibernate
    │   │               └── orm
    │   │                   └── panache
    │   │                       ├── entity
    │   │                       │   ├── FruitEntity.java
    │   │                       │   └── FruitEntityResource.java
    │   │                       └── repository
    │   │                           ├── Fruit.java
    │   │                           ├── FruitRepository.java
    │   │                           └── FruitRepositoryResource.java
    │   └── resources
    │       ├── application.properties
    │       ├── import.sql
    │       └── META-INF
    │           └── resources
    │               └── index.html
    └── test
        └── java
            └── org
                └── acme
                    └── hibernate
                        └── orm
                            └── panache
                                ├── FruitsEndpointIT.java
                                └── FruitsEndpointTest.java

22 directories, 18 files
```
