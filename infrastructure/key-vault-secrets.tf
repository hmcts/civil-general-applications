resource "azurerm_key_vault_secret" "app-insights-instrumentation-key" {
  name         = "app-insights-instrumentation-key"
  value        = azurerm_application_insights.appinsights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "app insights ${azurerm_application_insights.appinsights.name}"
  })
}