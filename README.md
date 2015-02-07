# Joplin

Joplin is a library for flexible datastore migration and seeding.

Joplin tries to solve the problems that arise when dealing with complicated systems consisting of multiple datastores. It lets you define and reason about environments (for instance dev, staging, UAT, prod).

Joplin lets you declare your `databases`, `migrators`, `seed functions` up front and combine them into different `environments`. It can be used via a [leiningen](http://leiningen.org) plugin or be called programatically.

Joplin comes with plugins for SQL/JDBC databases, Datomic, ElasticSearch, Cassandra, Hive and Zookeeper. It is built with extensibility in mind, adding more stores is done by a handful of multi-methods.

Joplin is built on top of [ragtime](https://github.com/weavejester/ragtime).

## Libraries

* joplin.core - database independent tools and functions for managing migrations and seeds
* joplin.cassandra - migrate and seed [Cassandra](http://cassandra.apache.org) clusters
* joplin.datomic - migrate and seed [Datomic](http://datomic.com) databases
* joplin.elasticsearch - migrate and seed [Elasticsearch](http://elasticsearch.org) clusters
* joplin.hive - migrate and seed [Hive](https://hive.apache.org) tables using Avro
* joplin.jdbc - migrate and seed SQL databases with jdbc
* joplin.lein - a leiningen plugin run migrations and seeds
* joplin.zookeeper - seed [Zookeeper](http://zookeeper.apache.org) clusters

## Installation

Add joplin.core as a dependency if you just want the database-independent core:

```clojure
:dependencies [[joplin.core "0.2.7"]]
```

If you want to integrate Joplin into Leiningen:

```clojure
:plugins [[joplin.lein "0.2.7"]]
```

`joplin.lein` will add dependencies to your project only for the plugins that you are using, i.e. the type of the databases you have defined.

If you are not using Leiningen, add a depedency (only) for the plugins of the databases you want to migrate;

```clojure
:dependencies [[joplin.cassandra "0.2.7"]
               [joplin.datomic "0.2.7"]
               [joplin.elasticsearch "0.2.7"]
               [joplin.hive "0.2.7"]
               [joplin.jdbc "0.2.7"]
               [joplin.zookeeper "0.2.7"]]
```

## Usage

### Using Joplin with Leiningen

To use `joplin.lein` you need to define a key called `:joplin` in your `project.clj`. See the [example project](https://github.com/juxt/joplin/blob/master/example/project.clj#L8).

The value of this key must be a map containing the keys `:databases`, `:migrators`, `:seeds` and `:environments`. The first three are pure declarations and `:environments` is where these declarations gets combined. For more details on these 4 keys see the [Concepts wiki page](https://github.com/juxt/joplin/wiki/concepts).

Please note that the folders with migrators and seed-var namespaces needs to be in the classpath in order for joplin to access them. Joplin will also look into any jar files on the classpath for resource folders with matching name.

Example of a joplin definition;
```clojure
:joplin {
         :migrators {:sql-mig "joplin/migrators/sql"}  ;; A path for a folder with migration files

         :seeds {:sql-seed "seeds.sql/run"             ;; A clojure var (function) that applies the seed
                 :es-seed "seeds.es/run"}

         :databases {:sql-dev {:type :jdbc, :url "jdbc:h2:mem:dev"}
                     :es-prod {:type :es, :host "es-prod.local", :port "9300", :cluster "dev"}
                     :sql-prod {:type :jdbc, :url "jdbc:h2:file:prod"}}

         ;; We combine the definitions above into different environments
         :environments {:dev [{:db :sql-dev, :migrator :sql-mig, :seed :sql-seed}]
                        :prod [{:db :sql-prod, :migrator :sql-mig}
                               {:db :es-prod}, :seed :es-seed]}
        }
```

Once the `:joplin` map has been defined, you can use the joplin leiningen plugin. The plugin comes with 5 commands;

All commands take the name of the environment as their first argument.

- `lein joplin migrate ENV [DB]`

Run all pending (up) migrations on either all databases in the environment or a single if the DB param is provided. This operation is idempotent.

- `lein joplin seed ENV [DB]`

Run the seed functions on either all databases in the environment or a single if the DB param is provided. This operation is NOT idempotent, running seed functions multiple time can lead to duplication of data.

Joplin will only run seed functions on databases that have been fully migrated, i.e. have no pending migrations.

- `lein joplin rollback ENV DB [N]`

Rollback N migrations (down migrate) from a single database in an environment. N is an optional argument and defaults to 1. This command requires that you specify a db.

- `lein joplin reset ENV DB`

Rollback ALL migrations, apply ALL up migrations and call the seed function for a single database in an environment. Intended to reset a database to a good state. This command require that you specify a db.

Note that reset will only work properly if all down migrations do a complete cleanup of the action taken by the up migration.

- `lein joplin create ENV DB ID`

Create a new migration scaffold file for a single database in an environment. This command requires that you specify both a database and identification string of the migrations.

### Writing migrators

Joplin migrators defaults to be 'code driven'. They are basic Clojure files and allows the migrators to be as simple or complicated as needed. If you are working with a SQL database, there is a second flavour of migrators at your disposal; ragtime-style sql migrators.

#### Joplin default migrators

A migrator consist of a single clojure source file. This file must contain (at least) 2 function definitions, one called `up` and one called `down`. The migrators will be called with one argument, which is the Ragtime Migratable record created by the joplin plugin. These record will contain the information you need to make a connections to the database and inflict a change.

Example of migrator;

```clojure
$ ls -1 migrators/cass
20140717174605_users.clj
$ cat migrators/cass/20140717174605_users.clj
(ns migrators.cass.20140717174605-users
  (:use [joplin.cassandra.database])
  (:require [clojurewerkz.cassaforte.cql    :as cql]
            [clojurewerkz.cassaforte.query  :as cq]))

(defn up [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/create-table conn "users"
                      (cq/column-definitions {:id :varchar
                                              :email :varchar
                                              :primary-key [:id]}))))

(defn down [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/drop-table conn "users")))
```

Read the `project.clj` file for the corresponding joplin plugin to see what clojure database libraries can be used in the migrators.

#### SQL migrators

When migrating SQL databases you have 2 flavours of migrators at your disposal. You may specify your migrations with two text files (one up, one down) as shown below:

```
$ ls -1 migrators/sql
20120903221323-add-test-table.down.sql
20120903221323-add-test-table.up.sql
$ cat migrators/sql/20120903221323-add-test-table.up.sql
CREATE TABLE test_table (id INT);
```

For this type of migration, use the Joplin database type `:sql`.

You may also specify your migrations as Clojure namespaces, like the example for Cassandra above, by using the Joplin database type `:jdbc`. See the example project for details.

### Writing seed functions

Seed functions are always placed in a clojure source file. The source file can contain any number of functions, since a seed definition is the name of a clojure var referencing a function.

The seed function are called with the target map (see below) as it's first parameter and any number of additional parameter that were passed into the lein plugin invocation (or to the multi-method if called programatically).

Example of a seed function;
```clojure
(ns seeds.dt
  (:require [datomic.api :as d]))

(defn run [target & args]
  (let [conn (d/connect (-> target :db :url))]
    @(d/transact conn [{:db/id #db/id [:db.part/user -100]
                        :foobar/id 42}])))
```

The target map will consist of 3 keys; `:db, :migrator, :seed`. The contents of these keys will be the (looked up) values of the keys specified in the environment map in the joplin definition.

### Calling Joplin from your code

Calling joplin from your code requires that you have loaded the `joplin.core` namespace and also the namespaces of the database plugins you intend to use. Then you can call one of 5 multi-methods to perform the same 5 actions as described above in the leiningen plugin section. The multi-methods are; `joplin.core/migrate-db`, `joplin.core/rollback-db`, `joplin.core/seed-db`, `joplin.core/reset-db`, `joplin.core/create-migration`.

All the multi-methods takes a `target` map as it's first argument and additional optional arguments.

The target map must have this shape;
```
{:db {:type DB-TYPE, DB-SETTING1: "foo", :DB-SETTING2: "bar", ...}
 :migrator "path to a folder on the classpath"
 :seed "name of a var in a namespace on the classpath"
 }
 ```

The `:migrator` and/or `:seed` keys are optional (joplin can't do much if both are missing). The `:DB-SETTING` keys vary for the different database types.

For example;
```
{:db {:type :dt, :url "datomic:mem://test"}
 :migrator "joplin/migrators/datomic"
 :seed "seeds.dt/run"
}
```

Valid database types are `:jdbc, :sql, :es, :zk, :dt, :cass`. Note that it's easy to [extend joplin](https://github.com/juxt/joplin/wiki/Adding-a-new-database-type) to handle more database types and thus introduce more valid database types.

### Hacking joplin

Joplin uses the [lein-sub](https://github.com/kumarshantanu/lein-sub) plugin, this makes it easy to install all projects locally with `lein sub install`.

## Documentation

[Wiki](https://github.com/juxt/joplin/wiki)

## License

Copyright © 2014 Martin Trojer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
