#!/bin/bash
echo "*** Creating test database"
gosu postgres postgres --single <<- EOSQL
   CREATE DATABASE test;
EOSQL
