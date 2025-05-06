kubectl create configmap kafka-client --from-file kafka/sasl_client.properties -n kafka
kubectl create configmap kafka-admin --from-file kafka/sasl_admin.properties -n kafka