#!/bin/bash

loader=node20
slave="node20 node21 node22 node23 node24 node25 node26 node27 node28 node29 node01 node02 node03 node04 node05 node06 node07 node08 node09 node10"
#slave="node20 node21 node22 node23 node24 node25 node26 node27 node28 node29"
#slave="node20 node21 node22 node23 node24"
path=/home/peluso/
max=50
count=0

for i in ${slave}
do
echo lunching slave on node $i
ssh $i ${path}start_slave.sh &
done

echo lunching loader
ssh ${loader} ${path}start_loader.sh &
while true
do
(( count += 1 ))
if [ ${count} -eq ${max} ]
then
break
fi
sleep 10

ret=`ssh ${loader} "grep END_TRANSACTIONS ${path}YCSB/transactions.dat | wc -l "`

if [ ${ret} -eq 1 ]
then
echo run ended
break
fi
echo -n .
done  


for i in ${loader} ${slave}
do
echo killing on node $i
ssh $i "killall -9 java"
done
