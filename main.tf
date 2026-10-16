
module "network" {
  source = "./modules/network"

  project_name           = var.project_name
  vpc_cidr               = var.vpc_cidr
  availability_zones     = var.availability_zones
  public_subnet_a_cidrs  = var.public_subnet_a_cidrs
  public_subnet_b_cidrs  = var.public_subnet_b_cidrs
  private_subnet_a_cidrs = var.private_subnet_a_cidrs
  private_subnet_b_cidrs = var.private_subnet_b_cidrs
}


module "iam" {
  source = "./modules/iam"
}


module "eks" {
  source = "./modules/eks"

  project_name    = var.project_name
  cluster_version = var.cluster_version

  private_subnet_ids = module.network.private_subnet_ids

  cluster_role_arn = module.iam.eks_cluster_role_arn
  node_role_arn    = module.iam.eks_node_role_arn

  node_instance_type = var.node_instance_type
  node_desired_size  = var.node_desired_size
  node_min_size      = var.node_min_size
  node_max_size      = var.node_max_size

  cluster_endpoint_public_access_cidrs = var.cluster_endpoint_public_access_cidrs
}


module "rds" {
  source = "./modules/rds"

  project_name = var.project_name
  vpc_id       = module.network.vpc_id

  private_subnet_ids = module.network.private_subnet_ids

  db_instance_class = var.db_instance_class
  db_name           = var.db_name
  db_username       = var.db_username
  db_password       = var.db_password

  eks_cluster_security_group_id = module.eks.cluster_security_group_id
}
