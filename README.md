# datanucleus-api-jpa

Support for DataNucleus persistence using the JPA API (JSR0220, JSR0317, JSR0338).  
JPA "persistence provider" is [org.datanucleus.api.jpa.PersistenceProviderImpl](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/PersistenceProviderImpl.java).  
JPA EntityManagerFactory is implemented by [org.datanucleus.api.jpa.JPAEntityManagerFactory](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAEntityManagerFactory.java).  
JPA EntityManager is implemented by [org.datanucleus.api.jpa.JPAEntityManager](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAEntityManager.java).  
JPA Query is implemented by [org.datanucleus.api.jpa.JPAQuery](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAQuery.java).  
JPA EntityTransaction is implemented by [org.datanucleus.api.jpa.JPAEntityTransaction](https://github.com/datanucleus/datanucleus-api-jpa/blob/master/src/main/java/org/datanucleus/api/jpa/JPAEntityTransaction.java).  

This is built using Maven, by executing `mvn clean install` which installs the built jar in your local Maven repository.


Please note that JPA effectively ends at JPA 2.2, and is continued by Jakarta Persistence v3.0. 
We support that through associated plugins for the [jakarta.persistence API](https://github.com/datanucleus/jakarta.persistence), [DataNucleus support for the API](https://github.com/datanucleus/datanucleus-api-jakarta), as well as associated samples.


## KeyFacts

__License__ : Apache 2 licensed  
__Issue Tracker__ : http://github.com/datanucleus/datanucleus-api-jpa/issues  
__Javadocs__ : [6.0](http://www.datanucleus.org/javadocs/api.jpa/6.0/), [5.2](http://www.datanucleus.org/javadocs/api.jpa/5.2/), [5.1](http://www.datanucleus.org/javadocs/api.jpa/5.1/), [5.0](http://www.datanucleus.org/javadocs/api.jpa/5.0/), [4.1](http://www.datanucleus.org/javadocs/api.jpa/4.1/), [4.0](http://www.datanucleus.org/javadocs/api.jpa/4.0/)  
__Download__ : [Maven Central](https://repo1.maven.org/maven2/org/datanucleus/datanucleus-api-jpa)  
__Dependencies__ : See file [pom.xml](pom.xml)  
__Support__ : [DataNucleus Support Page](http://www.datanucleus.org/support.html)  



## JPA Next Status

The JPA "expert group" is seemingly dead, only providing very minimal things in v2.2 (with Oracle dictating to people this is all you will get), with no plan beyond that.
They do, however, provide an issue tracker for people to raise issues of what they would like to see in the "next" release of JPA (if/when it ever happens). 
DataNucleus, not being content to wait for hell to freeze over, has gone ahead and worked on the following issues. 
All of these are embodied in our "JPA 2.2" offering (DN 5.1+).

[jpa-spec-25](https://github.com/eclipse-ee4j/jpa-api/issues/25) : access the JPQL string query and native query from a JPA Query object. Done  
[jpa-spec-30](https://github.com/eclipse-ee4j/jpa-api/issues/30) : case sensitive JPQL queries. _Not implemented but would simply need JDOQL semantics copying_  
[jpa-spec-32](https://github.com/eclipse-ee4j/jpa-api/issues/32) : Support for javax.cache. Provided since very early javax.cache releases!  
[jpa-spec-35](https://github.com/eclipse-ee4j/jpa-api/issues/35) : support for more types. DataNucleus has provided way more types since JPA1 days!  
[jpa-spec-41](https://github.com/eclipse-ee4j/jpa-api/issues/41) : targetClass attribute for @Embedded. Provided in javax.persistence-2.2.jar  
[jpa-spec-42](https://github.com/eclipse-ee4j/jpa-api/issues/42) : allow null embedded objects. Provided by extension  
[jpa-spec-48](https://github.com/eclipse-ee4j/jpa-api/issues/48) : @CurrentUser on a field. Implemented using DN extension annotations  
[jpa-spec-49](https://github.com/eclipse-ee4j/jpa-api/issues/49) : @CreateTimestamp, @UpdateTimestamp. Implemented using DN extension annotations  
[jpa-spec-52](https://github.com/eclipse-ee4j/jpa-api/issues/52) : EM.createNativeQuery(String,Class). DN already works like that  
[jpa-spec-74](https://github.com/eclipse-ee4j/jpa-api/issues/74) : Method of obtaining @Version value. Available via NucleusJPAHelper  
[jpa-spec-76](https://github.com/eclipse-ee4j/jpa-api/issues/76) : Allow specification of null handling in ORDER BY. Provided in javax.persistence-2.2.jar for Criteria and also in JPQL string-based  
[jpa-spec-77](https://github.com/eclipse-ee4j/jpa-api/issues/77) : EMF should implement AutoCloseable. Done  
[jpa-spec-81](https://github.com/eclipse-ee4j/jpa-api/issues/81) : @Version Support for Temporal Types. Done  
[jpa-spec-85](https://github.com/eclipse-ee4j/jpa-api/issues/85) : Metamodel methods to get entity by name. Provided in javax.persistence-2.2.jar  
[jpa-spec-86](https://github.com/eclipse-ee4j/jpa-api/issues/86) : Allow multiple level inheritance strategy. Not explicitly done but we do this for JDO so likely mostly there  
[jpa-spec-100](https://github.com/eclipse-ee4j/jpa-api/issues/100) : Allow an empty collection_valued_input_parameter in an "IN" expression. Already works like this  
[jpa-spec-102](https://github.com/eclipse-ee4j/jpa-api/issues/102) : JPQL : DATE/TIME functions. Done, with Criteria changes provided in javax.persistence-2.2.jar  
[jpa-spec-103](https://github.com/eclipse-ee4j/jpa-api/issues/103) : JPQL : Allow use of parameter in more than just WHERE/HAVING. Done  
[jpa-spec-105](https://github.com/eclipse-ee4j/jpa-api/issues/105) : Allow AttributeConverter to multiple columns. Done but using vendor extension  
[jpa-spec-107](https://github.com/eclipse-ee4j/jpa-api/issues/107) : JPQL : support subqueries in SELECT. Done  
[jpa-spec-108](https://github.com/eclipse-ee4j/jpa-api/issues/108) : Path.get(PluralAttribute<X, C, E>) lower bound missing on X. Provided in javax.persistence-2.2.1.jar  
[jpa-spec-111](https://github.com/eclipse-ee4j/jpa-api/issues/111) : Allow side-effect free check whether a named query is available. DN throws IllegalArgumentException and doesn't kill the current transaction.  
[jpa-spec-112](https://github.com/eclipse-ee4j/jpa-api/issues/112) : EntityGraph generic type incorrect. Provided in javax.persistence-2.2.jar  
[jpa-spec-113](https://github.com/eclipse-ee4j/jpa-api/issues/113) : Allow @GeneratedValue on non-PK fields. Provided since JPA 1  
[jpa-spec-128](https://github.com/eclipse-ee4j/jpa-api/issues/128) : JPQL : Support JOIN on 2 root candidates. Done  
[jpa-spec-133](https://github.com/eclipse-ee4j/jpa-api/issues/133) : Make GeneratedValue strategy=TABLE value available in PrePersist. Done  
[jpa-spec-137](https://github.com/eclipse-ee4j/jpa-api/issues/137) : Add methods taking List to Criteria. Provided in javax.persistence-2.2.jar  
[jpa-spec-150](https://github.com/eclipse-ee4j/jpa-api/issues/150) : Define requirement of using TransactionSynchronizationRegistry in EE environment. Present since DN v3.x  
[jpa-spec-151](https://github.com/eclipse-ee4j/jpa-api/issues/151) : GenerationType.UUID. Provided in javax.persistence-2.2.jar. Supported since DN 2.x  
[jpa-spec-152](https://github.com/eclipse-ee4j/jpa-api/issues/152) : Native support for UUID class. Supported since DN 2.x  
[jpa-spec-163](https://github.com/eclipse-ee4j/jpa-api/issues/163) : Support for java.time.Instant. Supported since DN 4.x  
[jpa-spec-167](https://github.com/eclipse-ee4j/jpa-api/issues/167) : preUpdate is fired and always has been.  
[jpa-spec-169](https://github.com/eclipse-ee4j/jpa-api/issues/169) : root.get() does not add a join, and neither should it.  
[jpa-spec-171](https://github.com/eclipse-ee4j/jpa-api/issues/171) : round() is included in javax.persistence-2.2.jar. Supported from DN 5.2.x.  
[jpa-spec-207](https://github.com/eclipse-ee4j/jpa-api/issues/207) : Allow @Convert usage on id attributes. Supported since DN 4.x.  
[jpa-spec-237](https://github.com/eclipse-ee4j/jpa-api/issues/237) : Auto-Quote database identifiers to avoid use of SQL keywords. Supported since DN 1.x.  
[jpa-spec-297](https://github.com/eclipse-ee4j/jpa-api/issues/297) : Support for "SELECT ... AS identifier1, ... AS identifier2" in JPQL constructor expressions. Supported from DN 6.x  
[jpa-spec-316](https://github.com/eclipse-ee4j/jpa-api/issues/316) : math functions in queries. Supported since a long time ago.  
[jpa-spec-326](https://github.com/eclipse-ee4j/jpa-api/issues/326) : CriteriaBuilder avgDistinct,sumDistinct,minDistinct,maxDistinct. Supported from DN 6.x.  

