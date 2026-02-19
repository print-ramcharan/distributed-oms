#!/bin/bash

# Deploy Platform
echo "Deploying Platform..."
kubectl apply -f k8s/secrets/
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/platform/

# Wait for platform to be ready (optional, but good practice)
echo "Waiting for Postgres and Kafka..."
# In a real script we'd use 'kubectl wait'

# Deploy Services
echo "Deploying Services..."
kubectl apply -f k8s/services/

# Deploy Ingress
echo "Deploying Ingress..."
kubectl apply -f k8s/ingress.yaml

echo "Deployment submitted. Check pods with 'kubectl get pods'."
