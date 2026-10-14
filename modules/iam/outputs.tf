
output "eks_cluster_role_arn" {
  value = data.aws_iam_role.eks_cluster.arn
}

output "eks_node_role_arn" {
  value = data.aws_iam_role.eks_node.arn
}

output "eks_cni_role_arn" {
  value = data.aws_iam_role.eks_cni.arn
}

output "load_balancer_controller_role_arn" {
  value = data.aws_iam_role.load_balancer_controller.arn
}
