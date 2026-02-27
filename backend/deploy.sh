#!/bin/bash

echo "Deploying Platform..."
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/platform/

echo "Waiting for Postgres and Kafka..."

echo "Deploying Services..."
kubectl apply -f k8s/services/

echo "Deploying Ingress..."
kubectl apply -f k8s/ingress.yaml

echo "Deployment submitted. Check pods with 'kubectl get pods'."
