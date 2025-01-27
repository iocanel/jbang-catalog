# JBang Catalog

Collection of JBang scripts related to Quarkus and its subprojects.

## Scripts
  - [create-from-github](#create-from-github): Create a new project from a Github repository.
  - [create-from-quickstart](#create-from-quickstart): Create a new project from a Quarkus quickstart.
  - [generate-data](#generate-data): Generate random data for your JPA entities using ChatGPT.
  - [generate-entity](#generate-entity): Generate JPA entities using ChatGPT.
  - [generate-rest](#generate-rest): Generate JAX-RS endpoint for an entity using ChatGPT.

### Using from the Quarkus CLI

**Requires**: Quarkus CLI 3.0.1.Final or higher

#### Versions [3.0.1, Final, 3.0.3.Final]
The scripts can added to the Quarkus CLI in the form of plugins, by replacing the `quarkusio` catalog of jbang with this one.

```sh
jbang catalog remove quarkusio
jbang catalog add --name=quarkusio https://github.com/iocanel/jbang-catalog/blob/HEAD/jbang-catalog.json
quarkus plug list --installable
```
#### Versions (3.0.3.Final, )
The Quarkus CLI supports multiple remote jbang catalogs. So this can be added next to the default one (`quarkusio`).

```sh
jbang catalog add --name=iocanel https://github.com/iocanel/jbang-catalog/blob/HEAD/jbang-catalog.json
quarkus plug list --installable
```

This should return in something like:

```
❯ quarkus plug list --installable
    Name                   	 Type  	 Scope 	 Location                                 	 Description 	
    create-from-quickstart 	 jbang 	 user  	 quarkus-create-from-quickstart@iocanel 	             	
    create-from-github     	 jbang 	 user  	 quarkus-create-from-github@iocanel     	             	

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
 *  create-from-github 	 jbang 	 project 	 quarkus-create-from-github@iocanel
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
 *  create-from-quickstart 	 jbang 	 user  	 quarkus-create-from-quickstart@iocanel 	             	

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


## generate-data

  Generate random data for your JPA entities using ChatGPT.
  Inspired by Max Andersen's [explain](https://github.com/maxandersen/jbang-catalog/tree/master/explain). 

### Enabling the extension

```sh
❯ quarkus plug add generate-data
Added plugin:
    Name                   	 Type  	 Scope 	 Location                                 	 Description 	
 *  generate-data        	 jbang 	 user  	 quarkus-generate-data@iocanel 	             	

```

### Usage

Given an entiry like:

```java
package org.acme;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
@Table(name = "person")
public class Person extends PanaceEntity {

  public String firstName;
  @Column(name = "middle_name")
  public String middleName;
  @Column(name = "last_name")
  public String lastName;
  @Column(name = "email")
  public String email;
  @Column(name = "birth_date")
  public LocalDate birthDate;
}
```

```sh
❯ quarkus generate-data Person 

Populating data for entity src/main/java/org/acme/Person.java with model gpt-3.5-turbo and temperature 0.8. Have patience...
INSERT INTO person (first_name, middle_name, last_name, email, birth_date) VALUES
('John', 'David', 'Smith', 'john.david.smith@example.com', '1990-01-01'),
('Jane', 'Marie', 'Doe', 'jane.marie.doe@example.com', '1995-05-05'),
('Michael', null, 'Brown', 'michael.brown@example.com', '1992-12-31'),
('Emily', 'Rose', 'Wang', 'emily.rose.wang@example.com', '1988-07-23'),
('William', 'Alexander', 'Davis', 'william.alexander.davis@example.com', '1994-03-17'),

('Sophia', 'Elizabeth', 'Thompson', 'sophia.elizabeth.thompson@example.com', '1985-11-10'),
('Ethan', 'Christopher', 'Garcia', 'ethan.christopher.garcia@example.com', '1997-02-22'),
('Avery', null, 'Wilson', 'avery.wilson@example.com', '1991-06-15'),
('Oliver', 'Benjamin', 'Taylor', 'oliver.benjamin.taylor@example.com', '1989-09-07'),
('Chloe', 'Isabella', 'Anderson', 'chloe.isabella.anderson@example.com', '1996-12-24'),

('Justin', null, 'Lee', 'justin.lee@example.com', '1990-01-01'),
('Hannah', 'Grace', 'Robinson', 'hannah.grace.robinson@example.com', '1995-05-05'),
('David', 'Joseph', 'Green', 'david.joseph.green@example.com', '1992-12-31'),
('Aaliyah', 'Nicole', 'King', 'aaliyah.nicole.king@example.com', '1988-07-23'),
('Brandon', 'Daniel', 'Baker', 'brandon.daniel.baker@example.com', '1994-03-17');

File src/main/resources/import.sql has been succesfully updated.
```

## generate-entity

  Generate JPA entities using ChatGPT.
  Also inspired by Max Andersen's [explain](https://github.com/maxandersen/jbang-catalog/tree/master/explain). 

### Enabling the extension

```sh
❯ quarkus plug add generate-data
Added plugin:
    Name                   	 Type  	 Scope 	 Location                                 	 Description 	
 *  generate-entity        	 jbang 	 user  	 quarkus-generate-entity@iocanel 	             	

```

### Usage

```sh
❯ quarkus generate-entity org.acme.Address
Generating entity org.acme.Address with model gpt-3.5-turbo and temperature 0.8. Have patience...
package org.acme;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String street;
    private String city;
    private String state;
    private String zipCode;

    public Address() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

File src/main/java/org/acme/Address.java has been succesfully created.
}
```

## generate-rest

  Generate JAX-RS endpoints for existing JPA entities using ChatGPT.

### Enabling the extension

```sh
❯ quarkus plug add generate-rest
Added plugin:
    Name                   	 Type  	 Scope 	 Location                                 	 Description 	
 *  generate-rest        	 jbang 	 user  	 quarkus-generate-rest@iocanel 	             	

```

### Usage
```sh
❯ quarkus generate-rest org.acme.Address org.acme.AddressResource
```
## generate-test

  Generate Junit5 Quarkus tests for existing JAX-RS endpoints using ChatGPT.

### Enabling the extension

```sh
❯ quarkus plug add generate-test
Added plugin:
    Name                   	 Type  	 Scope 	 Location                                 	 Description 	
 *  generate-test        	 jbang 	 user  	 quarkus-generate-test@iocanel 	             	

```

### Usage
```sh
❯ quarkus generate-test org.acme.AddressResource org.acme.AddressTest
`
