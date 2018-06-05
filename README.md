# datanucleus-api-jpa

Support for DataNucleus persistence using the JPA API (JSR0220, JSR0317, JSR0338).  
JPA "persistence provider" is [org.datanucleus.api.jpa.PersistenceProviderImpl](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/PersistenceProviderImpl.java).  
JPA EntityManagerFactory is implemented by [org.datanucleus.api.jpa.JPAEntityManagerFactory](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAEntityManagerFactory.java).  
JPA EntityManager is implemented by [org.datanucleus.api.jpa.JPAEntityManager](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAEntityManager.java).  
JPA Query is implemented by [org.datanucleus.api.jpa.JPAQuery](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAQuery.java).  
JPA EntityTransaction is implemented by [org.datanucleus.api.jpa.JPAEntityTransaction](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAEntityTransaction.java).  

This is built using Maven, by executing `mvn clean install` which installs the built jar in your local Maven repository.


## KeyFacts

__License__ : Apache 2 licensed  
__Issue Tracker__ : http://github.com/datanucleus/datanucleus-api-jpa/issues  
__Javadocs__ : [5.1](http://www.datanucleus.org/javadocs/api.jpa/5.1/), [5.0](http://www.datanucleus.org/javadocs/api.jpa/5.0/), [4.1](http://www.datanucleus.org/javadocs/api.jpa/4.1/), [4.0](http://www.datanucleus.org/javadocs/api.jpa/4.0/)  
__Download(Releases)__ : [Maven Central](http://central.maven.org/maven2/org/datanucleus/datanucleus-api-jpa)  
__Download(Nightly)__ : [Nightly Builds](http://www.datanucleus.org/downloads/maven2-nightly/org/datanucleus/datanucleus-api-jpa)  
__Dependencies__ : See file [pom.xml](pom.xml)  
__Support__ : [DataNucleus Support Page](http://www.datanucleus.org/support.html)  



## JPA Next Status

The JPA "expert group" is seemingly dead, only providing very minimal things in v2.2 (with Oracle dictating to people this is all you will get), with no plan beyond that.
They do, however, provide an issue tracker for people to raise issues of what they would like to see in the "next" release of JPA (if/when it ever happens). 
DataNucleus, not being content to wait for hell to freeze over, has gone ahead and worked on the following issues. 
All of these are embodied in our "JPA 2.2" offering (DN 5.1+), so why wait until Oracle can be bothered ?

[jpa-spec-25](https://github.com/javaee/jpa-spec/issues/25) : access the JPQL string query and native query from a JPA Query object. Done  
[jpa-spec-30](https://github.com/javaee/jpa-spec/issues/30) : case sensitive JPQL queries. _Not implemented but would simply need JDOQL semantics copying_  
[jpa-spec-35](https://github.com/javaee/jpa-spec/issues/35) : support for more types. DataNucleus has provided way more types since JPA1 days!  
[jpa-spec-41](https://github.com/javaee/jpa-spec/issues/41) : targetClass attribute for @Embedded. Provided in javax.persistence-2.2.jar  
[jpa-spec-42](https://github.com/javaee/jpa-spec/issues/42) : allow null embedded objects. Provided by extension  
[jpa-spec-48](https://github.com/javaee/jpa-spec/issues/48) : @CurrentUser on a field. Implemented using DN extension annotations  
[jpa-spec-49](https://github.com/javaee/jpa-spec/issues/49) : @CreateTimestamp, @UpdateTimestamp. Implemented using DN extension annotations  
[jpa-spec-52](https://github.com/javaee/jpa-spec/issues/52) : EM.createNativeQuery(String,Class). DN already works like that  
[jpa-spec-74](https://github.com/javaee/jpa-spec/issues/74) : Method of obtaining @Version value. Available via NucleusJPAHelper  
[jpa-spec-76](https://github.com/javaee/jpa-spec/issues/76) : Allow specification of null handling in ORDER BY. Provided in javax.persistence-2.2.jar for Criteria and also in JPQL string-based  
[jpa-spec-77](https://github.com/javaee/jpa-spec/issues/77) : EMF should implement AutoCloseable. Done  
[jpa-spec-85](https://github.com/javaee/jpa-spec/issues/85) : Metamodel methods to get entity by name. Provided in javax.persistence-2.2.jar  
[jpa-spec-86](https://github.com/javaee/jpa-spec/issues/86) : Allow multiple level inheritance strategy. Not explicitly done but we do this for JDO so likely mostly there  
[jpa-spec-100](https://github.com/javaee/jpa-spec/issues/100) : Allow an empty collection_valued_input_parameter in an "IN" expression. Already works like this  
[jpa-spec-102](https://github.com/javaee/jpa-spec/issues/102) : JPQL : DATE/TIME functions. Done, with Criteria changes provided in javax.persistence-2.2.jar  
[jpa-spec-103](https://github.com/javaee/jpa-spec/issues/103) : JPQL : Allow use of parameter in more than just WHERE/HAVING. Done  
[jpa-spec-105](https://github.com/javaee/jpa-spec/issues/105) : Allow AttributeConverter to multiple columns. Done but using vendor extension  
[jpa-spec-107](https://github.com/javaee/jpa-spec/issues/107) : JPQL : support subqueries in SELECT. Done   
[jpa-spec-108](https://github.com/javaee/jpa-spec/issues/108) : Path.get(PluralAttribute<X, C, E>) lower bound missing on X. Provided in javax.persistence-2.2.1.jar  
[jpa-spec-112](https://github.com/javaee/jpa-spec/issues/112) : EntityGraph generic type incorrect. Provided in javax.persistence-2.2.jar  
[jpa-spec-113](https://github.com/javaee/jpa-spec/issues/113) : Allow @GeneratedValue on non-PK fields. Provided since JPA 1  
[jpa-spec-128](https://github.com/javaee/jpa-spec/issues/128) : JPQL : Support JOIN on 2 root candidates. Done  
[jpa-spec-133](https://github.com/javaee/jpa-spec/issues/133) : Make GeneratedValue strategy=TABLE value available in PrePersist. Done  
[jpa-spec-137](https://github.com/javaee/jpa-spec/issues/137) : Add methods taking List to Criteria. Provided in javax.persistence-2.2.jar  
[jpa-spec-150](https://github.com/javaee/jpa-spec/issues/150) : Define requirement of using TransactionSynchronizationRegistry in EE environment. Present since DN v3.x  
[jpa-spec-151](https://github.com/javaee/jpa-spec/issues/151) : GenerationType.UUID. Provided in javax.persistence-2.2.jar. Supported since DN 2.x  
[jpa-spec-152](https://github.com/javaee/jpa-spec/issues/152) : Native support for UUID class. Supported since DN 2.x  
[jpa-spec-163](https://github.com/javaee/jpa-spec/issues/163) : Support for java.time.Instant. Supported since DN 4.x  

