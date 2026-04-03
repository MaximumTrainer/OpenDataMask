output "server_public_ip" {
  description = "Public IP address of the application server"
  value       = aws_eip.app.public_ip
}

output "instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "ssh_connection_string" {
  description = "SSH command to connect to the server"
  value       = "ssh -i <your-private-key.pem> ec2-user@${aws_eip.app.public_ip}"
}

output "frontend_url" {
  description = "URL for the frontend application"
  value       = "http://${aws_eip.app.public_ip}"
}

output "backend_api_url" {
  description = "URL for the backend REST API"
  value       = "http://${aws_eip.app.public_ip}:8080"
}

output "health_check_url" {
  description = "Spring Boot Actuator health endpoint"
  value       = "http://${aws_eip.app.public_ip}:8080/actuator/health"
}
