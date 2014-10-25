## 0.2.0

### Breaking changes

* :jdbc migrators are now 'code driven' (just like all non-sql migrators). A new type called :sql has been added for 'ragtime style' sql-file migrators.
* upgrade to cassaforte 2.0.0-beta8 (which includes breaking changes https://github.com/clojurewerkz/cassaforte/blob/master/ChangeLog.md)

### Minor changes

* changed to clojurewerkz/ragtime (to solve https://github.com/weavejester/ragtime/issues/34)
