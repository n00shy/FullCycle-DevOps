# FullCycle DevOps

A hands-on AWS infrastructure project built with Terraform and Amazon EKS. The goal is to practice the infrastructure and operational workflow behind a three-tier application, then add containerization, CI/CD, and observability incrementally.

> **Current status:** The AWS infrastructure has been applied successfully. The EKS managed worker nodes reached `Ready`, and the VPC CNI issue was resolved by configuring EKS Pod Identity. Kubernetes API access was also restored after correcting the access configuration for the current client identity/IP. Application workloads, CI/CD, and monitoring are the next phases.

## Architecture

- **Amazon VPC** using CIDR `10.0.0.0/16`, with public and private subnets across two Availability Zones.
- **Public networking:** one Internet Gateway and a shared public route table for both public subnets.
- **Private networking:** one NAT Gateway in public subnet A, with both private subnet route tables routing outbound internet traffic through it. This is a cost-saving choice, not a highly available NAT design.
- **Amazon EKS:** EKS cluster with a managed node group in private subnets. The public Kubernetes API endpoint is restricted to configured client CIDRs, while private endpoint access is enabled.
- **EKS Pod Identity:** the `eks-pod-identity-agent` add-on and a Pod Identity association for the `kube-system/aws-node` service account, using the `CloudOpsEKSCNIRole` IAM role for the VPC CNI.
- **Amazon RDS for PostgreSQL:** private, encrypted storage, Single-AZ, with automated backup retention set to zero for this disposable learning environment.
- **IAM:** Terraform looks up existing project IAM roles for the EKS cluster, node group, CNI, and AWS Load Balancer Controller. The roles themselves are not created by the current IAM module.

## Current EKS configuration

- Region: `us-east-1`
- Cluster: `fullcycle-devops`
- Managed node group: `fullcycle-devops-nodes`
- Node instance type: `t3.medium`
- Desired/minimum/maximum nodes: `2 / 2 / 2`
- Node placement: private subnets in two Availability Zones
- Authentication mode: `API_AND_CONFIG_MAP`
- CNI permissions: `CloudOpsEKSCNIRole` associated to `kube-system/aws-node` using EKS Pod Identity

The cluster version is controlled by the Terraform input variable `cluster_version`; use the value in the active configuration rather than assuming a fixed version from this document.

## Repository structure

```text
.
├── main.tf
├── providers.tf
├── versions.tf
├── variables.tf
├── outputs.tf
└── modules/
    ├── network/
    ├── iam/
    ├── eks/
    └── rds/
```

Terraform state, variable files, and saved plans are excluded by `.gitignore`. Keep secret values and generated state out of Git.

## Prerequisites

- An AWS account and AWS CLI configured with an identity that has the required permissions
- Terraform `>= 1.5.0` and the AWS provider `~> 6.0`
- `kubectl`
- The required project IAM roles already present in the AWS account
- Local Terraform inputs, including your own public IPv4 address and a database password

## Configure local Terraform inputs

Before running Terraform, create a local `terraform.tfvars` file in the repository root. Do not commit this file: it contains environment-specific values and a database password.

The EKS public API endpoint is restricted by `cluster_endpoint_public_access_cidrs`. You must enter the **public IPv4 address of the machine from which you will run `kubectl`**, as a single-host CIDR ending in `/32`. Do not use your computer's private LAN address.

Get your current public IPv4 address with:

```bash
curl -4 https://checkip.amazonaws.com
```

If the command returns `203.0.113.25`, for example, the CIDR format would be `203.0.113.25/32`. This is only an example address; replace it with your own actual public IPv4 address.

Set the relevant values in `terraform.tfvars`:

```hcl
cluster_endpoint_public_access_cidrs = ["YOUR_PUBLIC_IPV4/32"]
db_password                           = "REPLACE_WITH_A_STRONG_PASSWORD"
```

Keep the other required Terraform variables in the same file as appropriate for your environment. Never publish your real password or your local `terraform.tfvars` file.

If your public IP changes later, update `cluster_endpoint_public_access_cidrs` to your new public IPv4 address in `/32` format and apply the Terraform change. Until the allowlist is updated, your client may be unable to reach the public EKS API endpoint. Do not use `0.0.0.0/0` as a workaround.

## Deployment and verification

Review the Terraform configuration and costs before creating resources.

```bash
terraform init
terraform fmt -recursive
terraform validate
terraform plan
# Apply only after reviewing the plan:
terraform apply
```

Configure `kubectl` and verify the cluster:

```bash
aws eks update-kubeconfig --region us-east-1 --name fullcycle-devops
kubectl get nodes -o wide
kubectl get pods -A
kubectl get daemonset aws-node -n kube-system
kubectl get pods -n kube-system -o wide
```

The expected baseline is for the managed nodes to be `Ready`, the `aws-node` pods to be running on the nodes, and CoreDNS to become `Running`.

## Troubleshooting notes

### VPC CNI / node readiness

During initial cluster setup, the VPC CNI could not call EC2 APIs because the CNI workload did not have the required IAM permissions through Pod Identity. The issue was resolved by:

1. Installing the `eks-pod-identity-agent` EKS add-on.
2. Associating the `CloudOpsEKSCNIRole` role with service account `aws-node` in namespace `kube-system`.
3. Ensuring the managed node group depends on the Pod Identity association in Terraform.
4. Restarting the `aws-node` DaemonSet after the association was created, then verifying node and system-pod readiness.

### Kubernetes API access when the client IP changes

The EKS public API endpoint is restricted by `cluster_endpoint_public_access_cidrs`. If the public IP of the machine running `kubectl` changes, the new public IP (as a single-host CIDR, typically `x.x.x.x/32`) must be added to the Terraform variable and applied before public endpoint access will work again.

This is separate from Kubernetes authentication. The EKS authentication mode is `API_AND_CONFIG_MAP`; IAM identity mapping through the EKS access configuration / `aws-auth` ConfigMap controls who is authenticated/authorized, while the public access CIDR controls which client IPs can reach the endpoint. Changing one does not replace the other.

Useful checks:

```bash
aws eks describe-cluster --region us-east-1 --name fullcycle-devops \
  --query 'cluster.resourcesVpcConfig.{Public:endpointPublicAccess,Private:endpointPrivateAccess,AllowedCIDRs:publicAccessCidrs}'

kubectl get nodes
kubectl get configmap aws-auth -n kube-system -o yaml
```

Do not open the Kubernetes API to `0.0.0.0/0` as a workaround. Prefer a current trusted client CIDR and keep IAM/Kubernetes access appropriately restricted.

## Cost and security decisions

This is a learning environment, not a production-ready reference architecture.

- One NAT Gateway reduces cost but creates a single point of failure and can incur hourly and data-processing charges.
- EKS, EC2 worker nodes, NAT Gateway, public IPv4 addresses, and RDS can incur charges while running.
- RDS is Single-AZ, private, encrypted, and configured without automated backup retention to limit cost for disposable test data. Do not use these data-protection settings for important or production data.
- The RDS security group should permit PostgreSQL (`5432/tcp`) only from the intended EKS workload/node security group.
- Keep the EKS public API allowlist narrow.
- Never commit AWS credentials, database passwords, `terraform.tfstate`, `.tfvars` files containing secrets, or saved Terraform plans.

## Cleanup

When finished with a practice session, inspect the resources and data you need to retain, then run:

```bash
terraform plan -destroy
terraform destroy
```

Confirm in the AWS console/CLI that the resources are deleted. NAT Gateway and RDS are particularly important to remove when no longer needed to avoid ongoing charges.

## Roadmap

1. Build and test the three-tier application locally.
2. Containerize the frontend and backend and run the stack with Docker Compose.
3. Deploy workloads to EKS and connect the backend to private RDS.
4. Add CI.
5. Add CD.
6. Add monitoring and observability.

## Author

**Abdallah Ahmed**  
GitHub: [@n00shy](https://github.com/n00shy)  
LinkedIn: [n00shy](https://www.linkedin.com/in/n00shy/)

---

For learning and portfolio demonstration only. Review all infrastructure settings, IAM permissions, and AWS costs before using any part of this design in production.
