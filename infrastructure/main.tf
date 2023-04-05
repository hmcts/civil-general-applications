resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = var.location

  tags = var.common_tags
}

data "azurerm_key_vault" "civil" {
  name                = "${var.product}-${var.env}"
  resource_group_name = "${var.product}-service-${var.env}"
}


# TO DO: REMOVE THIS SECRET AFTER APPLICATION IS CONFGIURED TO FETCH FROM APP SPECIFIC KEY VAULT.
resource "azurerm_application_insights" "appinsights" { 
  name                = "${var.product}-${var.component}-${var.env}"
  location            = var.appinsights_location
  resource_group_name = azurerm_resource_group.rg.name
  application_type    = "web"
  tags                = var.common_tags
}

# TO DO: REMOVE THIS SECRET AFTER APPLICATION IS CONFGIURED TO FETCH FROM APP SPECIFIC KEY VAULT.
resource "azurerm_key_vault_secret" "app_insights_instrumental_key" {
  name         = "AppInsightsInstrumentationKeyGeneralApplications"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = data.azurerm_key_vault.civil.id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "app insights ${azurerm_application_insights.appinsights.name}"
  })
}

# TO DO: REMOVE THIS SECRET AFTER APPLICATION IS CONFGIURED TO FETCH FROM APP SPECIFIC KEY VAULT.
resource "azurerm_key_vault_secret" "app_insights_connection_string" {
  name         = "genapp-appinsights-connection-string"
  value        = azurerm_application_insights.appinsights.connection_string
  key_vault_id = data.azurerm_key_vault.civil.id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "appinsights ${azurerm_application_insights.appinsights.name}"
  })
}
