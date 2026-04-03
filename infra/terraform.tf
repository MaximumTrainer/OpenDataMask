terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state in S3 + DynamoDB locking.
  # Initialise with backend config injected at CI time:
  #   terraform init \
  #     -backend-config="bucket=$TF_STATE_BUCKET" \
  #     -backend-config="dynamodb_table=$TF_STATE_DYNAMODB_TABLE" \
  #     -backend-config="region=$AWS_REGION"
  #
  # To bootstrap the S3 bucket and DynamoDB table once (run locally):
  #   aws s3api create-bucket --bucket <bucket> --region <region>
  #   aws s3api put-bucket-versioning --bucket <bucket> --versioning-configuration Status=Enabled
  #   aws dynamodb create-table --table-name <table> \
  #     --attribute-definitions AttributeName=LockID,AttributeType=S \
  #     --key-schema AttributeName=LockID,KeyType=HASH \
  #     --billing-mode PAY_PER_REQUEST
  backend "s3" {
    key     = "opendatamask/terraform.tfstate"
    encrypt = true
    # bucket, region, and dynamodb_table are supplied via -backend-config in CI
  }
}

provider "aws" {
  region = var.aws_region
}
