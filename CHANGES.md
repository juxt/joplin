## 0.2.3

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
