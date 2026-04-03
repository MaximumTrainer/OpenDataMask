variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "instance_type" {
  description = "EC2 instance type. t3.small provides ~2GB RAM, sufficient for Spring Boot + PostgreSQL + Nginx."
  type        = string
  default     = "t3.small"
}

variable "app_name" {
  description = "Application name – used as a prefix for all resource names and tags"
  type        = string
  default     = "opendatamask"
}

variable "environment" {
  description = "Deployment environment (staging | production)"
  type        = string
  default     = "staging"
}

variable "public_key_material" {
  description = "SSH public key content (e.g. the contents of id_rsa.pub). Used to create the EC2 key pair."
  type        = string
  sensitive   = true
}

variable "allowed_ssh_cidr" {
  description = "CIDR block allowed to reach port 22. Restrict to your IP in production."
  type        = string
  default     = "0.0.0.0/0"
}

variable "volume_size_gb" {
  description = "Root EBS volume size in GiB"
  type        = number
  default     = 20
}
