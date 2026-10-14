# FullCycle DevOps

Infrastructure-as-Code project for building AWS cloud infrastructure with Terraform and running workloads on Amazon EKS.

> **Project status:** In progress. The initial infrastructure deployment is being validated, including EKS worker-node readiness and cluster networking.

## Architecture

The planned infrastructure includes:

- **Amazon VPC** with public and private subnets across two Availability Zones
- **Internet Gateway** for public subnet connectivity
- **NAT Gateway** for outbound connectivity from private subnets
- **Amazon EKS** with a managed node group
- **Amazon RDS for PostgreSQL** deployed in private subnets
- **AWS IAM** roles for the EKS cluster, worker nodes, and related components
- **Terraform modules** to organize infrastructure into reusable components

## Technology stack

- AWS
- Terraform
- Amazon EKS
- Kubernetes
- Amazon RDS for PostgreSQL
- AWS IAM and VPC networking

## Repository structure

The Terraform configuration is organized into modules for the network, IAM, EKS, and RDS components.

```text
.
├── main.tf
├── variables.tf
├── outputs.tf
├── terraform.tfvars.example
└── modules/
    ├── network/
    ├── iam/
    ├── eks/
    └── rds/
```

Adjust this tree to match the actual files committed to the repository.

## Prerequisites

- AWS CLI configured with an IAM identity that has the required permissions
- Terraform installed
- kubectl installed
- Access to an AWS account

## Deployment workflow

1. Review the Terraform configuration and AWS permissions.
2. Set required input variables, including the database password, through a secure mechanism.
3. Run `terraform init`.
4. Run `terraform validate` and `terraform plan`.
5. Review the plan carefully before running `terraform apply`.
6. Configure kubectl using `aws eks update-kubeconfig` and verify cluster and node status.
7. Validate application connectivity to the private PostgreSQL database before deploying workloads.

Example commands:

```bash
terraform init
terraform fmt -recursive
terraform validate
terraform plan
aws eks update-kubeconfig --region us-east-1 --name fullcycle-devops
kubectl get nodes
kubectl get pods -A
```

Do not apply infrastructure changes without reviewing the plan and understanding the associated AWS costs.

## Security notes

- Never commit AWS access keys, database passwords, Terraform state files, or saved plan files.
- Keep the RDS instance private and restrict database access to the required application or node security group.
- Restrict public access to the EKS API endpoint to trusted IP addresses.
- Use least-privilege IAM permissions.

## Cleanup

When testing is complete, review the resources managed by Terraform and plan their removal to avoid ongoing AWS charges. Take care with database data before destroying RDS resources.

## Current focus

The current phase focuses on validating the EKS managed node group, worker-node readiness, and cluster networking before proceeding to workload deployment.

## Author

**Abdallah Ahmed**  
GitHub: [@n00shy](https://github.com/n00shy)  
LinkedIn: [n00shy](https://www.linkedin.com/in/n00shy/)

---

This project is for learning and portfolio demonstration. Review all infrastructure settings, permissions, and costs before using them in production.
