# Joplin

Joplin is a library for flexible datastore migration and seeding.

Joplin tries to solve the problems that arise when dealing with complicated systems consisting of multiple datastores. It lets you define and reason about environments (for instance dev, staging, UAT, prod).

Joplin lets you declare your `databases`, `migrators`, `seed functions` up front and them combined them in different `environments`. It can be used via a [leiningen](http://leiningen.org) plugin or be called programatically.

Joplin comes with plugins for SQL/ databases, ElasticSearch and Zookeeper. It is built with extensibility in mind, adding more stores is done by a handful of multi-methods.

Joplin is built on top of [ragtime](https://github.com/weavejester/ragtime).

## Libraries

* joplin.core - database independent tools and functions for managing migrations and seeds
* joplin.elasticsearch - migrate and seed [Elasticsearch](http://elasticsearch.org) clusters
* joplin.jdbc - migrate and seed SQL databses with jdbc
* joplin.lein - a leiningen plugin run migrations and seeds
* joplin.zookeeper - seed [Zookeeper](http://zookeeper.apache.org) clusters

## Installation

Add joplin.core as a dependency if you just want the database-independent core:

```clojure
:dependencies [[joplin.core "0.1.4"]]
```

Or add the full library if you want support for ES/SQL/ZK databases:

```clojure
:dependencies [[joplin "0.1.4"]]
```

If you want to integrate Joplin into Leiningen:

```clojure
:plugins [[joplin.lein "0.1.4"]]
```

## Documentation

[Wiki](https://github.com/martintrojer/joplin/wiki)

## License

Copyright © 2014 Martin Trojer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
