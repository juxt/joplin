#!/bin/bash

sed "s/alt-host=127.0.0.1/alt-host=$TARGET_HOST/" -i ~/datomic/free-transactor.properties

/root/datomic/bin/transactor /root/datomic/free-transactor.properties
