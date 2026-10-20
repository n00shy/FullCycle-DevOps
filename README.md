# FullCycle DevOps

A hands-on DevOps portfolio project that combines a Terraform-managed AWS foundation with a containerized Notes application. The project is being developed incrementally to practice infrastructure provisioning, Docker, Kubernetes/EKS, CI/CD, security, and observability.

> **Current status:** The React frontend, Spring Boot backend, and PostgreSQL database are implemented for local development with Docker Compose. The AWS VPC, EKS cluster, managed node group, IAM integrations, and private RDS foundation are also part of the project. Deploying the application workloads to EKS, then adding CI/CD and monitoring, remains planned work.

## Application

The current local application is a Notes web app with a frontend, API backend, and relational database.

- **Frontend:** React 18, TypeScript, Vite, and Lucide React. The production image serves the built frontend through Nginx.
- **Backend:** Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Bean Validation, and Flyway database migrations.
- **Database:** PostgreSQL 16 for the local Docker Compose stack.
- **Application capabilities:** User authentication, user-owned notes, note creation/read/update/delete operations, search and category filtering, and audit events for note changes.
- **Health and metrics:** Spring Boot Actuator health endpoints and Prometheus metrics registry.
- **Containers:** Multi-stage Docker builds for the frontend and backend.

### Local application architecture

```text
Browser
  |
  | http://localhost:80
  v
Frontend container (Nginx + React static build)
  |  /api/* and /actuator/*
  v
Backend container (Spring Boot, port 8082)
  |
  | JDBC
  v
PostgreSQL container (port 5432 inside Compose network)
  |
  v
Named Docker volume: postgres_data
```

Nginx proxies API and Actuator requests to the backend, so the browser can use the frontend origin instead of requiring a separate API host.

## AWS infrastructure

The Terraform configuration provisions the cloud foundation used for the broader DevOps practice project.

- **Amazon VPC:** CIDR `10.0.0.0/16`, with public and private subnets across two Availability Zones.
- **Public networking:** One Internet Gateway and a shared public route table for both public subnets.
- **Private networking:** One NAT Gateway in public subnet A; private subnet route tables send outbound internet traffic through it. This reduces cost compared with one NAT Gateway per Availability Zone, but is not a highly available NAT design.
- **Amazon EKS:** EKS cluster with a managed node group in private subnets. The public Kubernetes API endpoint is restricted to configured client CIDRs; private endpoint access is enabled.
- **EKS Pod Identity:** The `eks-pod-identity-agent` add-on and a Pod Identity association for `kube-system/aws-node`, using the `CloudOpsEKSCNIRole` IAM role for the VPC CNI.
- **Amazon RDS for PostgreSQL:** Private, encrypted, Single-AZ database configured with automated backup retention set to zero for this disposable learning environment.
- **IAM:** The Terraform configuration looks up existing project IAM roles for the EKS cluster, node group, CNI, and AWS Load Balancer Controller. The current IAM module does not create those roles.

### Current EKS configuration

- Region: `us-east-1`
- Cluster: `fullcycle-devops`
- Managed node group: `fullcycle-devops-nodes`
- Node instance type: `t3.medium`
- Desired / minimum / maximum nodes: `2 / 2 / 2`
- Node placement: private subnets in two Availability Zones
- Authentication mode: `API_AND_CONFIG_MAP`
- CNI permissions: `CloudOpsEKSCNIRole` associated with `kube-system/aws-node` through EKS Pod Identity

The EKS version is controlled by the Terraform input variable `cluster_version`; check the active configuration for its current value.

## Repository structure

```text
.
├── main.tf
├── providers.tf
├── versions.tf
├── variables.tf
├── outputs.tf
├── modules/
│   ├── network/
│   ├── iam/
│   ├── eks/
│   └── rds/
├── docker-compose.yml
├── frontend/
│   ├── Dockerfile
│   ├── nginx.local.conf
│   └── src/
└── backend/
    ├── Dockerfile
    ├── pom.xml
    └── src/
```

Terraform state, saved plans, and local variable files should remain out of Git. Keep secrets and generated state out of the repository.

## Run the application locally

### Prerequisites

- Docker Engine
- Docker Compose v2 (the `docker compose` command)
- Ports `80` and `8082` available on the host

### Start the stack

From the repository root:

```bash
docker compose up -d --build
docker compose ps
```

Open the frontend:

- Web app: [http://localhost](http://localhost)
- Backend port (direct access, when needed): `http://localhost:8082`

The frontend Nginx configuration proxies `/api/` and `/actuator/` to the backend container. PostgreSQL is intended to be accessed by the backend over the internal Compose network.

### View logs and stop the stack

```bash
# Follow all service logs
docker compose logs -f

# Follow only backend logs
docker compose logs -f backend

# Stop containers and preserve database data in the named volume
docker compose down

# Stop containers and delete the local database volume (destructive)
docker compose down -v
```

The Compose configuration currently uses local-development database credentials and disables the secure-cookie flag for localhost. These settings are for local practice only, not for a public or production deployment. Do not expose this Compose stack to the internet with the development credentials unchanged.

## Configure and apply Terraform

### Prerequisites

- An AWS account and AWS CLI configured with an identity that has the required permissions
- Terraform `>= 1.5.0` and the AWS provider `~> 6.0`
- `kubectl`
- The required project IAM roles already present in the AWS account
- Local Terraform inputs, including your public IPv4 address and a database password

Create a local `terraform.tfvars` file in the repository root. Do not commit it: it contains environment-specific values and a database password.

The EKS public API endpoint is restricted by `cluster_endpoint_public_access_cidrs`. Add the **public IPv4 address of the machine running kubectl** as a single-host CIDR ending in `/32`; do not use your private LAN address.

Get the current public IPv4 address:

```bash
curl -4 https://checkip.amazonaws.com
```

Example Terraform inputs (replace the example values with your own):

```hcl
cluster_endpoint_public_access_cidrs = ["YOUR_PUBLIC_IPV4/32"]
db_password                           = "REPLACE_WITH_A_STRONG_PASSWORD"
```

Keep any other required Terraform variables in the same local file. Never publish your actual password or `terraform.tfvars`.

Initialize and validate the configuration, then review the plan before applying:

```bash
terraform init
terraform fmt -recursive
terraform validate
terraform plan
# Apply only after reviewing the plan:
terraform apply
```

Configure kubectl and check cluster health:

```bash
aws eks update-kubeconfig --region us-east-1 --name fullcycle-devops
kubectl get nodes -o wide
kubectl get pods -A
kubectl get daemonset aws-node -n kube-system
kubectl get pods -n kube-system -o wide
```

The expected baseline is for managed nodes to be `Ready`, `aws-node` pods to be running on the nodes, and CoreDNS to become `Running`.

## Troubleshooting notes

### VPC CNI and node readiness

During initial cluster setup, the VPC CNI could not call EC2 APIs because it did not have the required IAM permissions through Pod Identity. The issue was resolved by:

1. Installing the `eks-pod-identity-agent` EKS add-on.
2. Associating `CloudOpsEKSCNIRole` with the `aws-node` service account in namespace `kube-system`.
3. Ensuring the managed node group depends on the Pod Identity association in Terraform.
4. Restarting the `aws-node` DaemonSet and verifying node and system-pod readiness.

### EKS API access after a public IP change

The public API endpoint is restricted by `cluster_endpoint_public_access_cidrs`. If the client public IP changes, update the Terraform allowlist to the new IP in `/32` format and apply the change. Until then, the client may not be able to reach the endpoint.

Network allowlisting is separate from Kubernetes authentication and authorization. The allowlist determines which client IPs can reach the endpoint; EKS access configuration and IAM identity mappings determine which identities can authenticate and what they can do.

Useful checks:

```bash
aws eks describe-cluster --region us-east-1 --name fullcycle-devops \
  --query 'cluster.resourcesVpcConfig.{Public:endpointPublicAccess,Private:endpointPrivateAccess,AllowedCIDRs:publicAccessCidrs}'

kubectl get nodes
kubectl get configmap aws-auth -n kube-system -o yaml
```

Do not open the Kubernetes API to `0.0.0.0/0` as a workaround.

## Cost and security considerations

This is a learning environment, not a production-ready reference architecture.

- EKS, EC2 worker nodes, NAT Gateway, public IPv4 addresses, and RDS can incur charges while running.
- One NAT Gateway reduces cost but creates a single point of failure and incurs hourly and data-processing charges.
- RDS is private, encrypted, Single-AZ, and configured without automated backup retention for disposable practice data. Do not use these data-protection settings for important or production data.
- The RDS security group should allow PostgreSQL (`5432/tcp`) only from the intended EKS workload/node security group.
- Keep the EKS API allowlist narrow.
- Never commit AWS credentials, database passwords, `terraform.tfstate`, secret-bearing `.tfvars` files, or saved Terraform plans.

## Cleanup

When finished with an AWS practice session, review which resources and data must be retained, then run:

```bash
terraform plan -destroy
terraform destroy
```

Verify that resources have been deleted. NAT Gateway and RDS are particularly important to remove when no longer needed to avoid ongoing charges.

For the local application, use `docker compose down`. Use `docker compose down -v` only when you intentionally want to delete the local PostgreSQL volume and its data.

## Roadmap

- [x] Build a local Notes frontend, backend API, and PostgreSQL stack with Docker Compose
- [x] Build the AWS VPC, EKS managed-node-group, IAM integration, and private RDS foundation
- [ ] Deploy the application workloads to EKS
- [ ] Connect the EKS-hosted backend to private RDS
- [ ] Add continuous integration (CI)
- [ ] Add continuous delivery/deployment (CD)
- [ ] Add monitoring and observability

## Author

**Abdallah Ahmed**  
GitHub: [@n00shy](https://github.com/n00shy)  
LinkedIn: [n00shy](https://www.linkedin.com/in/n00shy/)

---

For learning and portfolio demonstration only. Review infrastructure settings, IAM permissions, application security, and AWS costs before adapting this design for production.
