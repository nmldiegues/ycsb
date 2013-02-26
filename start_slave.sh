#!/bin/bash

cd YCSB
java -Xmx7168M -Xms7168M -Djava.net.preferIPv4Stack=true -Dbind.address=`hostname` -cp build/ycsb.jar::db/infinispan-5.0/lib/*:db/infinispan-5.0/conf/:lib/*:.  com.yahoo.ycsb.Client -t -db com.yahoo.ycsb.db.InfinispanClient -P workloads/workloadb -p recordcount=10000 -p operationcount=500000 -s -threads 1 -p measurementtype=timeseries -p timeseries.granularity=2000 > transactions.dat


