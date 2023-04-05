resource "azurerm_key_vault_secret" "appinsights-instrumentation-key" {
  name         = "appinsights-instrumentation-key"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "app insights ${azurerm_application_insights.appinsights.name}"
  })
}

resource "azurerm_key_vault_secret" "appinsights-connection-string" {
  name         = "appinsights-connection-string"
  value        = azurerm_application_insights.appinsights.connection_string
  key_vault_id = module.key-vault.key_vault_id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "appinsights ${azurerm_application_insights.appinsights.name}"
  })
}