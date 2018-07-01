#!/usr/bin/env bash

#mongoimport --host luketn-shard-0/luketn-shard-00-00-qqdqm.mongodb.net:27017,luketn-shard-00-01-qqdqm.mongodb.net:27017,luketn-shard-00-02-qqdqm.mongodb.net:27017 --ssl -u lthompson --authenticationDatabase admin -d SydneyJavaMeetup -c perfstats perfstats.json
mongoimport --host localhost:27017,luketn-shard-00-02-qqdqm.mongodb.net:27017 -d SydneyJavaMeetup -c perfstats perfstats.json