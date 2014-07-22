#!/bin/bash
PROJECTS=("joplin.core" "joplin.elasticsearch" "joplin.zookeeper" "joplin.cassandra" "joplin.jdbc" "joplin.datomic" "joplin.lein" ".")

function install() {
    declare -a projects=("${!1}")
    for project in "${projects[@]}"; do
        echo "Installing $project"
	pushd $project	
	lein install
	popd
    done
}

install PROJECTS[@]
