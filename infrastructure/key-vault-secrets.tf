resource "azurerm_key_vault_secret" "appinsights-instrumentation-key" {
  name         = "appinsights-instrumentation-key"
  value        = module.application_insights.instrumentation_key
  key_vault_id = module.key-vault.key_vault_id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "app insights ${module.application_insights.name}"
  })
}

resource "azurerm_key_vault_secret" "appinsights-connection-string" {
  name         = "appinsights-connection-string"
  value        = module.application_insights.connection_string
  key_vault_id = module.key-vault.key_vault_id
  content_type = "secret"
  tags = merge(var.common_tags, {
    "source" : "appinsights ${module.application_insights.name}"
  })
}
