
data "aws_iam_role" "eks_cluster" {
  name = "CloudOpsEKSClusterRole"
}

data "aws_iam_role" "eks_node" {
  name = "CloudOpsEKSNodeRole"
}

data "aws_iam_role" "eks_cni" {
  name = "CloudOpsEKSCNIRole"
}

data "aws_iam_role" "load_balancer_controller" {
  name = "CloudOpsEKSLoadBalancerControllerRole"
}
