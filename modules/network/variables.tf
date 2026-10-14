
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
