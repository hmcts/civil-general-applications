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
module "application_insights" {
  source = "git@github.com:hmcts/terraform-module-application-insights?ref=main"

  env                 = var.env
  product             = var.product
  name                = "${var.product}-${var.component}"
  location            = var.appinsights_location
  resource_group_name = azurerm_resource_group.rg.name

  common_tags = var.common_tags
}

moved {
  from = azurerm_application_insights.appinsights
  to   = module.application_insights.azurerm_application_insights.this
}

# TO DO: REMOVE THIS SECRET AFTER APPLICATION IS CONFGIURED TO FETCH FROM APP SPECIFIC KEY VAULT.
resource "azurerm_key_vault_secret" "app_insights_instrumental_key" {
  name         = "AppInsightsInstrumentationKeyGeneralApplications"
  value        = module.application_insights.instrumentation_key
  key_vault_id = data.azurerm_key_vault.civil.id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "app insights ${module.application_insights.name}"
  })
}

# TO DO: REMOVE THIS SECRET AFTER APPLICATION IS CONFGIURED TO FETCH FROM APP SPECIFIC KEY VAULT.
resource "azurerm_key_vault_secret" "app_insights_connection_string" {
  name         = "genapp-appinsights-connection-string"
  value        = module.application_insights.connection_string
  key_vault_id = data.azurerm_key_vault.civil.id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "appinsights ${module.application_insights.name}"
  })
}
