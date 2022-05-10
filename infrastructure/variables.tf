variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {}

variable "common_tags" {
  type = "map"
}

variable "appinsights_location" {
  type        = string
  default     = "UK South"
  description = "Location for Application Insights"
}
