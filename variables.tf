
variable "aws_region" {
  type = string
}

variable "project_name" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "availability_zones" {
  type = list(string)
}

variable "public_subnet_a_cidrs" {
  type = list(string)
}

variable "public_subnet_b_cidrs" {
  type = list(string)
}

variable "private_subnet_a_cidrs" {
  type = list(string)
}

variable "private_subnet_b_cidrs" {
  type = list(string)
}


variable "cluster_version" {}

variable "node_instance_type" {}

variable "node_desired_size" {
  type = number
}

variable "node_min_size" {
  type = number
}

variable "node_max_size" {
  type = number
}

variable "cluster_endpoint_public_access_cidrs" {
  type = list(string)
}


variable "db_instance_class" {
  type = string
}

variable "db_name" {
  type = string
}

variable "db_username" {
  type = string
}

variable "db_password" {
  type      = string
  sensitive = true
}

