## 0.3.5

## New features

DynamoDB support

## 0.3.4

### Fixes

Fix issue with duplicate .sql migrators

## 0.3.3

### Minor changes

Fix issue #74 (minor differences between how joplin / ragtime scans the classpath)

## 0.3.2

### Minor changes

Better error reporting when loading vars

## 0.3.1

### Minor changes

Improved backwards compatability for :jdbc migrators

## 0.3.0

Major rewrite with several breaking changes. [See here for details](https://github.com/juxt/joplin/wiki/Differences-between-0.2-and-0.3)

## 0.2.17

### Fixes
* joplin.lein with leiningen 2.5.2

## 0.2.11

### Minor changes
* pending migrations enforces the ordering invariant, and can be used to check for inconsistencies without running the migrations

## 0.2.10

### Minor changes
* pending migrations now returns info about missing (but applied) migrators

### Fixes
* pending migrations not working for :type :sql

## 0.2.9

### Fixes
* Bug in elasticsearch native-client code

## 0.2.8

### New features
* New lein command 'pending' to list pending migrations

### Minor changes
* JDBC; support for multiple migration tables

## 0.2.7

### New features
* Vastly improved examples

### Minor changes
* Elasticsearch; native client support for migrating data

## 0.2.6

### Minor changes
* Elasticsearch; support for multiple migration indicies

## 0.2.5

### Minor changes
* Elasticsearch; expose index from config
* joplin.cassandra/cassaforte 2.0.0

## 0.2.3

### Breaking changes

joplin.elasticsearch

update-index now updates the index mappings and settings in place. There are many caveats to what you can change in place in ES. For things that you can't change, clone-index has been changed to behave like the old update-index did - that is, it creates a new index, copies over all settings and mappings (with optional deltas that you can supply) and points the alias at the newly created index.

Main joplin project no longer depends on all the plugins (too many classpath conflicts)

### New features

* New plugin joplin.hive using Avro

### Minor changes

* joplin.elasticsearch/elastisch 2.1.0-rc1
* joplin.cassandra/cassaforte 2.0.0-rc4
* joplin.datomic/datmoic 0.9.5078
* back on ragtime/ragtime 0.3.8
* joplin.elasticsearch/migrate-data now uses bulk index

## 0.2.2

### Fixes

* Fixed bug in joplin.cassandra for down migrations

### Minor changes

* joplin.core/do-rollback accepts the 'N' parameter in both integer (when called from code) and string (when called fron lein)

## 0.2.1

### Fixes

* reset did not rollback before migrating

## 0.2.0

### Breaking changes

* :jdbc migrators are now 'code driven' (just like all non-sql migrators). A new type called :sql has been added for 'ragtime style' sql-file migrators.
* upgrade to cassaforte 2.0.0-beta8 (which includes breaking changes https://github.com/clojurewerkz/cassaforte/blob/master/ChangeLog.md)

### Minor changes

* changed to clojurewerkz/ragtime (to solve https://github.com/weavejester/ragtime/issues/34)
