#!/usr/bin/env bash

#!/usr/bin/env bash

# delete pods
docker exec kubectl-support kubectl delete --all pods --namespace=jenkins
# remove namespace
RESPONSE=`echo `docker exec kubectl-support kubectl delete namespaces jenkins``

if [ "${RESPONSE}" != "Error response from daemon: No such container: kubectl-support" ]; then
# check status of the pods in every 30 seconds
PODS_STATUS=echo `docker exec kubectl-support kubectl get pods --namespace=jenkins`
    while [ "${PODS_STATUS}" != "No resources found." ]
    do
        sleep 30
        PODS_STATUS=echo `kubectl get pods --namespace=jenkins`
    done
fi

# stop containers
CID=`docker container ls -aq -f "name=kubectl-support"`
if [ ! -z "${CID}" ]; then
    docker container stop ${CID}
fi

#remove containers
CID=`docker container ls -aq -f "name=kubectl-support"`
if [ ! -z "${CID}" ]; then
    docker container rm ${CID}
fi

# remove all gatling solr dockers
IMG_ID=`docker images -a | grep "kubectl-support" | awk '{print $3}'`
if [ ! -z "${IMG_ID}" ]; then
    docker rmi ${IMG_ID}
fi