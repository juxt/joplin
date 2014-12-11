## 0.2.3

### Breaking changes

joplin.elasitcsearch

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
