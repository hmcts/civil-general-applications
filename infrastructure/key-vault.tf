data "azuread_group" "dts_civil" {
  display_name     = "DTS Civil"
  security_enabled = true
}

data "azurerm_user_assigned_identity" "civil-mi" {
  name                = "${var.product}-${var.env}-mi"
  resource_group_name = "managed-identities-${var.env}-rg"
}

module "key-vault" {
  source                      = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  name                        = "${var.product}-gen-apps-${var.env}" # NAME HAS BEEN ABBREVIATED DUE TO 24 LIMIT MAX ON KV NAMES
  product                     = var.product
  env                         = var.env
  tenant_id                   = var.tenant_id
  object_id                   = var.jenkins_AAD_objectId
  resource_group_name         = azurerm_resource_group.rg.name
  product_group_object_id     = data.azuread_group.dts_civil.object_id
  managed_identity_object_ids = [data.azurerm_user_assigned_identity.civil-mi.principal_id]
  common_tags                 = var.common_tags
  create_managed_identity     = false
}