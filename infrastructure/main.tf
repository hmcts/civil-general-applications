provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location

  tags = var.common_tags
}


resource "azurerm_application_insights" "appinsights" {
  name                = "${var.product}-${var.component}-${var.env}"
  location            = var.appinsights_location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"
}

data "azurerm_key_vault" "civil" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-service-${var.env}"
}


resource "azurerm_key_vault_secret" "app_insights_instrumental_key" {
  name         = "AppInsightsInstrumentationKeyGeneralApplications"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = data.azurerm_key_vault.civil.id
}
