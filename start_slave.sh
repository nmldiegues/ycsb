#!/bin/bash

cp="/home/ndiegues/.m2/repository/org/infinispan/infinispan-core/5.2.0-cloudtm-SNAPSHOT/infinispan-core-5.2.0-cloudtm-SNAPSHOT.jar:/home/ndiegues/.m2/repository/org/jgroups/jgroups/3.3.0-cloudtm-SNAPSHOT/jgroups-3.3.0-cloudtm-SNAPSHOT.jar:/home/ndiegues/.m2/repository/org/jboss/spec/javax/transaction/jboss-transaction-api_1.1_spec/1.0.0.Final/jboss-transaction-api_1.1_spec-1.0.0.Final.jar:/home/ndiegues/.m2/repository/org/jboss/marshalling/jboss-marshalling-river/1.3.6.GA/jboss-marshalling-river-1.3.6.GA.jar:/home/ndiegues/.m2/repository/org/jboss/marshalling/jboss-marshalling/1.3.6.GA/jboss-marshalling-1.3.6.GA.jar:/home/ndiegues/.m2/repository/org/jboss/logging/jboss-logging/3.1.0.GA/jboss-logging-3.1.0.GA.jar:/home/ndiegues/.m2/repository/org/codehaus/woodstox/woodstox-core-asl/4.1.1/woodstox-core-asl-4.1.1.jar:/home/ndiegues/.m2/repository/org/codehaus/woodstox/stax2-api/3.1.1/stax2-api-3.1.1.jar:/home/ndiegues/.m2/repository/com/clearspring/analytics/stream/2.2.0/stream-2.2.0.jar:/home/ndiegues/.m2/repository/com/github/egrim/java-bloomier-filter/1.0.Final/java-bloomier-filter-1.0.Final.jar:/home/ndiegues/.m2/repository/com/googlecode/kryo/1.04/kryo-1.04.jar:/home/ndiegues/.m2/repository/asm/asm/3.2/asm-3.2.jar:/home/ndiegues/.m2/repository/com/googlecode/reflectasm/1.01/reflectasm-1.01.jar:/home/ndiegues/.m2/repository/com/googlecode/minlog/1.2/minlog-1.2.jar:/home/ndiegues/.m2/repository/org/rhq/helpers/rhq-pluginAnnotations/3.0.4/rhq-pluginAnnotations-3.0.4.jar:/home/ndiegues/.m2/repository/org/codehaus/jackson/jackson-core-asl/1.5.2/jackson-core-asl-1.5.2.jar:/home/ndiegues/.m2/repository/org/codehaus/jackson/jackson-mapper-asl/1.5.2/jackson-mapper-asl-1.5.2.jar:/home/ndiegues/.m2/repository/log4j/log4j/1.2.16/log4j-1.2.16.jar:."


cd ~/ycsb/target/classes;
java -Xmx7168M -Xms7168M -Djava.net.preferIPv4Stack=true -Dbind.address=`hostname` -cp $cp  com.yahoo.ycsb.Client -t -db com.yahoo.ycsb.db.InfinispanClient -P workloads/workloadg -p multiplereadcount=50 -p recordcount=1000 -p operationcount=2000 -s -nodes 4 -threads 1 -p measurementtype=timeseries -p timeseries.granularity=2000 > transactions.dat


