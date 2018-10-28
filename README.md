# Joplin

*Joplin 0.3 contains many breaking changes over the 0.2 releases*, please [see here for details](https://github.com/juxt/joplin/wiki/Differences-between-0.2-and-0.3).

Joplin is a library for flexible datastore migration and seeding.

Joplin tries to solve the problems that arise when dealing with complicated systems consisting of multiple datastores. It lets you define and reason about environments (for instance dev, staging, UAT, prod).

Joplin lets you declare your `databases`, `migrators`, `seed functions` up front and combine them into different `environments`. It can be used from the REPL, via a [leiningen](http://leiningen.org) aliases or be called programatically.

Joplin comes with plugins for SQL/JDBC databases, Datomic, ElasticSearch, Cassandra, DynamoDB, Hive and Zookeeper. It is built with extensibility in mind, adding more stores is done by a handful of multi-methods.

Joplin is built on top of [ragtime](https://github.com/weavejester/ragtime).

## Libraries

* joplin.core - database independent tools and functions for managing migrations and seeds
* joplin.cassandra - migrate and seed [Cassandra](http://cassandra.apache.org) clusters
* joplin.dynamodb - migrate and seed [DynamoDB](http://aws.amazon.com/dynamodb) clusters
* joplin.datomic - migrate and seed [Datomic](http://datomic.com) databases
* joplin.elasticsearch - migrate and seed [Elasticsearch](http://elasticsearch.org) clusters
* joplin.hive - migrate and seed [Hive](https://hive.apache.org) tables using Avro
* joplin.jdbc - migrate and seed SQL databases with jdbc
* joplin.zookeeper - seed [Zookeeper](http://zookeeper.apache.org) clusters
* [clj-rethinkdb-migrations](https://github.com/apa512/clj-rethinkdb-migrations) - migrate and seed [RethinkDB](https://www.rethinkdb.com/) clusters
* [joplin.mongodb](https://github.com/razum2um/joplin.mongodb) - migrate and seed [MongoDB](http://mongodb.com/)

## Installation

Add joplin.core as a dependency if you just want the database-independent core:

```clojure
:dependencies [[joplin.core "0.3.11"]]
```

If you are not using Leiningen, add a dependency for the plugins of the databases you want to migrate;

```clojure
:dependencies [[joplin.cassandra "0.3.11"]
               [joplin.dynamodb "0.3.11"]
               [joplin.datomic "0.3.11"]
               [joplin.elasticsearch "0.3.11"]
               [joplin.hive "0.3.11"]
               [joplin.jdbc "0.3.11"]
               [joplin.zookeeper "0.3.11"]]
```

## Using Joplin

### Defining the configuration map

The joplin configuration map contains the keys `:databases`, `:migrators`, `:seeds` and `:environments`. The first three are pure declarations and `:environments` is where these declarations gets combined. For more details on these 4 keys see the [Concepts wiki page](https://github.com/juxt/joplin/wiki/concepts).

Typically you would define the configuration in a `.edn` file that you put somewhere on your classpath.

Please note that the folders with migrators and seed-var namespaces needs to be in the classpath in order for joplin to access them. Joplin will also look into any jar files on the classpath for resource folders with matching name.

Example of a joplin definition;
```clojure
{
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

### Hiding secrets from the configuration map

Once the config map has been written to a `.edn` file you'll need to read that file and pass the datastructure to joplin's migration and seed functions. JDBC connection strings typically include username and password that you don't want to put into source control.

The `joplin.repl` namespace provides a function called `load-config` that take the name (or URL) of a file on the classpath (`joplin.edn`) and reads the contents turning it into Clojure datastructures. This function implements 2 tagged literals to solve the problem with secrets in the configuration; `env` and `envf`
```clojure
:es-dev      {:type :es, :host #env TARGET_HOST, :port 9200}
:psql-prod   {:type :sql, :url #envf ["jdbc:postgresql://psq-prod/prod?user=%s&password=%s"
                                      PROD_USER PROD_PASSWD]}
```

* `env` takes a single name of an environment variable that is put in its place in the result of the `load-config` call.
* `envf` takes a vector of data, the first item is a string passed to Clojure's `format` function. The rest of the items in the vector are names of environment variables that will be looked up and passed along with the format string to the `format` call.

#### Overriding the migration table

For `jdbc` / `sql` migrators the migration table is called `ragtime_migrations` by default. You can change this by adding a key called `:migrations-table` to the database map.

### Running migrations

The `joplin.repl` namespace defines a function for each of Joplin's 5 basic operations. It's common to use these functions to setup a 'migration and seed REPL' from which you can control your databases.

- `migrate [conf env & args]`

Run all pending (up) migrations on either all databases in the environment or a single if the database is provided as the first argument after env. This operation is idempotent.

- `seed [conf env & args]`

Run the seed functions on either all databases in the environment or a single if the database is provided as the first argument after env. This operation is NOT idempotent, running seed functions multiple time can lead to duplication of data.

Joplin will only run seed functions on databases that have been fully migrated, i.e. have no pending migrations.

- `rollback [conf env database amount-or-id & args]`

Rollback N migrations (down migrate) from a single database in an environment. N can either be a number (the number to migration steps you want to roll back, or an id of a migration you want to roll back to). This command requires that you specify a db.

- `reset [conf env database & args]`

Rollback ALL migrations, apply ALL up migrations and call the seed function for a single database in an environment. Intended to reset a database to a good state. This command require that you specify a db.

Note that reset will only work properly if all down migrations do a complete cleanup of the action taken by the up migration.

- `pending [conf env database & args]`

Print pending migrations for a database in an environment.

- `create [conf env database & args]`

Create a new migration scaffold file for a single database in an environment. This command requires that you specify both a database and identification string of the migrations.

### Running migrations from the command line

When you want to script your migrations requiring a REPL is not convenient. In this case you can use Leiningen aliases to provide command-line commands for the 5 operation above. The example project shows to do do this in the [project.clj](https://github.com/juxt/joplin/blob/master/example/project.clj#L15) and the [code the aliases uses](https://github.com/juxt/joplin/blob/master/joplin.core/src/joplin/alias.clj).

#### Migration conflict strategies

The extra args of the `joplin.repl` functions are used to pass extra options to ragtime, this is currently used for the `migrate` operation where different strategies for dealing with migration conflicts can be specified.

`(migrate config :dev :cass-dev {:strategy ragtime.strategy/rebase})`

[Read here](https://github.com/weavejester/ragtime/blob/master/ragtime.core/src/ragtime/strategy.clj#L24) for info on available strategies.

### Writing migrators

Joplin migrators defaults to be 'code driven'. They are basic Clojure files and allows the migrators to be as simple or complicated as needed. If you are working with a SQL database, there is a second flavor of migrators at your disposal; ragtime-style sql migrators.

#### Joplin default migrators

A migrator consist of a single clojure source file. This file must contain (at least) 2 function definitions, one called `up` and one called `down`. The migrators will be called with one argument, which is the Ragtime DataStore record created by the joplin plugin. These record will contain the information you need to make a connections to the database and inflict a change.

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

When migrating SQL databases you have 2 flavors of migrators at your disposal. You may specify your migrations with two text files (one up, one down) as shown below:

```shell
$ ls -1 migrators/sql
20120903221323-add-test-table.down.sql
20120903221323-add-test-table.up.sql
$ cat migrators/sql/20120903221323-add-test-table.up.sql
CREATE TABLE test_table (id INT);
```

For this type of migration, use the Joplin database type `:sql`.

You may also specify your migrations as Clojure namespaces, like the example for Cassandra above, by using the Joplin database type `:jdbc`. See the [example project](https://github.com/juxt/joplin/tree/master/example) for details.

Please note as of Joplin 0.3 if you have multiple SQL statements in a single `.sql` file, you need to insert a marker `--;;` between them.

### Writing seed functions

Seed functions are always placed in a clojure source file. The source file can contain any number of functions, since a seed definition is the name of a clojure var referencing a function.

The seed function are called with the target map (see below) as its first parameter and any number of additional parameter that were passed into the lein plugin invocation (or to the multi-method if called programatically).

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

Calling joplin from your code requires that you have loaded the `joplin.core` namespace and also the namespaces of the database plugins you intend to use. Then you can call one of 5 multi-methods to perform the same 5 actions as described above in the leiningen plugin section. The multi-methods are; `joplin.core/migrate-db`, `joplin.core/rollback-db`, `joplin.core/seed-db`, `joplin.core/pending-migrations`, `joplin.core/create-migration`.

All the multi-methods takes a `target` map as its first argument and additional optional arguments.

The target map must have this shape;
```clojure
{:db {:type DB-TYPE, DB-SETTING1: "foo", :DB-SETTING2: "bar", ...}
 :migrator "path to a folder on the classpath"
 :seed "name of a var in a namespace on the classpath"
 }
 ```

The `:migrator` and/or `:seed` keys are optional (joplin can't do much if both are missing). The `:DB-SETTING` keys vary for the different database types.

For example;
```clojure
(ns example
  (:require [joplin.core :as joplin]
            [joplin.dt.database]))

(joplin/migrate-db
  {:db {:type :dt,
        :url "datomic:mem://test"}
   :migrator "joplin/migrators/datomic"
   :seed "seeds.dt/run"})
```

Provided database types and their respective plugins;

<table>
  <tr>
    <th>{:db {:type ? ...</th>
    <th>:require [?]</th>
  </tr>
  <tr>
    <td>:jdbc</td>
    <td>joplin.jdbc.database</td>
  </tr>
  <tr>
    <td>:sql</td>
    <td>joplin.jdbc.database</td>
  </tr>
  <tr>
    <td>:es</td>
    <td>joplin.elasticsearch.database</td>
  </tr>
  <tr>
    <td>:zk</td>
    <td>joplin.zookeeper.database</td>
  </tr>
  <tr>
    <td>:dt</td>
    <td>joplin.dt.database</td>
  </tr>
  <tr>
    <td>:cass</td>
    <td>joplin.cassandra.database</td>
  </tr>
  <tr>
    <td>:dynamo</td>
    <td>joplin.dynamodb.database</td>
  </tr>
</table>

Note that it's easy to [extend joplin](https://github.com/juxt/joplin/wiki/Adding-a-new-database-type) to handle more database types and thus introduce more valid database types.

### Hacking joplin

Joplin uses the [lein-sub](https://github.com/kumarshantanu/lein-sub) plugin, this makes it easy to install all projects locally with `lein sub install`.

## Documentation

[Wiki](https://github.com/juxt/joplin/wiki)

## License

Copyright © 2015 Martin Trojer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
