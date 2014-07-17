# Joplin

Joplin is a library for flexible datastore migration and seeding.

Joplin tries to solve the problems that arise when dealing with complicated systems consisting of multiple datastores. It lets you define and reason about environments (for instance dev, staging, UAT, prod).

Joplin lets you declare your `databases`, `migrators`, `seed functions` up front and combine them in different `environments`. It can be used via a [leiningen](http://leiningen.org) plugin or be called programatically.

Joplin comes with plugins for SQL/JDBC databases, Datomic, ElasticSearch, Cassandra and Zookeeper. It is built with extensibility in mind, adding more stores is done by a handful of multi-methods.

Joplin is built on top of [ragtime](https://github.com/weavejester/ragtime).

## Libraries

* joplin.core - database independent tools and functions for managing migrations and seeds
* joplin.cassandra - migrate and seed [Cassandra](http://cassandra.apache.org) clusters
* joplin.datomic - migrate and seed [Datomic](http://datomic.com) databases
* joplin.elasticsearch - migrate and seed [Elasticsearch](http://elasticsearch.org) clusters
* joplin.jdbc - migrate and seed SQL databses with jdbc
* joplin.lein - a leiningen plugin run migrations and seeds
* joplin.zookeeper - seed [Zookeeper](http://zookeeper.apache.org) clusters

## Installation

Add joplin.core as a dependency if you just want the database-independent core:

```clojure
:dependencies [[joplin.core "0.1.5-SNAPSHOT"]]
```

Or add the full library if you want support for ES/SQL/DT/CASS/ZK databases:

```clojure
:dependencies [[joplin "0.1.5-SNAPSHOT"]]
```

You can also cherry-pick the plugins you need to minimize the dependencies in your classpath. Just include `joplin.core` and the plugins you are interested in.

If you want to integrate Joplin into Leiningen:

```clojure
:plugins [[joplin.lein "0.1.5-SNAPSHOT"]]
```

`joplin.lein` will only add dependencies for the plugins that you are using, i.e. the type of the databases you have defined.

## Usage

### Using Joplin with Leiningen

To use `joplin.lein` you need to define a key called `:joplin` in your `project.clj`. See the [example project](https://github.com/juxt/joplin/blob/master/example/project.clj#L8).

The value of this key must be a map containing the keys `:databases`, `:migrators`, `:seeds` and `:environments`. The first three are pure declarations and `:environments` is where these declarations gets combined. For more details on these 4 keys see the [Concepts](https://github.com/martintrojer/joplin/wiki/concepts).

Once the `:joplin` map has been defined, you can use the joplin leiningen plugin. The plugin comes with 5 commands;

- `lein joplin migrate ENV [DB]`

This command runs all pending (up) migrations on either all databases in the environment of a signle if the DB param us provided. This operation is idempotent.

- `lein joplin seed ENV [DB]`

This command run the seed functions on either all databases in the environemnt or a single if the DB param is provided. This operation is NOT idempotent, running seed functions multiple time can lead to duplication of data.

Joplin will only run seed functions on databases that have been fully migrated, i.e. have no pending migrations.


### Calling Joplin from your code

## Documentation

[Wiki](https://github.com/martintrojer/joplin/wiki)

## License

Copyright © 2014 Martin Trojer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
