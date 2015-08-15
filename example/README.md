## Joplin example project

This is an example project showing how to use Joplin and its supported databases. This repo also contains examples of all supported migrators and seed function so it acts as a good starting point to create your own migrators and seed functions.

The `project.clj` shows how to configure Joplin, and use leiningen profiles to split up a complicated multi-datastore setup.

### Usage

Install [Docker](http://docker.io), [Docker Compose](https://docs.docker.com/compose/) and perhaps [Docker Machine](https://docs.docker.com/machine/).

`$ docker-compose up`

This will bring up all databases, please note that will require a fair amount of RAM in the docker machine. If it runs out consider editing the `fig.yml` file to only start the databases you care about.

The example contains a [migrate namespace](https://github.com/juxt/joplin/blob/master/example/src/migrate.clj) that is convenient to use from the REPL, to `$ lein run` to enter that namespace.

The `project.clj` also contains a number of [aliases](https://github.com/juxt/joplin/blob/master/example/project.clj#L14) making it possible to migrate databases from the command line.
These alias map directly to the 5 core operations Joplin provides;
* migrate
* seed
* rollback
* pending
* create

For more information check out the joplin documentation.

### IP / Hostnames

The examples comes with 2 hard-coded IPs that you probably need to change. This should the hostname (or IP) of your docker service.

The files to change are `project.clj` and `datomic-docker/Dockerfile`
