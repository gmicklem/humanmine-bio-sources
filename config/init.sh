#!/bin/bash

set -e

export PSQL_USER=test
sudo -u postgres createuser test
sudo -u postgres psql -c "alter user test with encrypted password 'test';"

# Set up properties
source config/create-ci-properties-files.sh

sudo -u postgres createdb -O test bio-test  # test owns the database: PostgreSQL 15+ no longer lets other roles create in schema public
sudo -u postgres psql -c "grant all privileges on database \""bio-test\"" to test;"


