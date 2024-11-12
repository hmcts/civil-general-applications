resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location

  tags = var.common_tags
}

data "azurerm_key_vault" "civil" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-service-${var.env}"
}
