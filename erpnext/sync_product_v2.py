# =============================================================================
# sync_product_v2.py — Rich Hierarchy Webhook for Radolfa Backend
# =============================================================================
#
# Trigger:  ERPNext Server Script → Document Event → Item → on_update
# Payload:  Template → Variant (Colour) → Item (Size) with effective pricing
# Endpoint: POST http://backend:8080/api/v1/sync/products
#
# This script replaces sync_product.py. It sends the full product hierarchy
# so the backend can maintain the 3-tier model (ProductBase → ListingVariant → Sku).
#
# IMPORTANT: This file is NOT executed by Claude. It is provided for the
# ERPNext developer to wire into the Server Script manager.
# =============================================================================

# Constants
BACKEND_URL = "http://backend:8080/api/v1/sync/products"
JWT_TOKEN = "YOUR_SYSTEM_JWT_TOKEN_HERE"


def sync_item_to_radolfa(doc, method=None):
    """
    Called on Item save. Identifies the parent template, extracts colour/size
    attributes, calculates effective price, and posts the hierarchy payload.

    Args:
        doc: The frappe document (Item) that triggered the event.
        method: The document event method (e.g., 'on_update').
    """

    # 1. Skip if this is not a variant or a template with variants
    if not doc.has_variants and not doc.variant_of:
        # Simple item with no variants — send as single-SKU hierarchy
        _sync_simple_item(doc)
        return

    if doc.has_variants:
        # This IS the template — sync all its variants
        template_doc = doc
    else:
        # This is a variant — fetch the parent template
        template_doc = frappe.get_doc("Item", doc.variant_of)

    _sync_template(template_doc)


def _sync_template(template_doc):
    """Build and send the full hierarchy for a template."""

    # Fetch all variants of this template
    variant_codes = frappe.get_all(
        "Item",
        filters={"variant_of": template_doc.name, "disabled": 0},
        pluck="name"
    )

    # Group variants by colour attribute
    colour_groups = {}
    for variant_code in variant_codes:
        variant_doc = frappe.get_doc("Item", variant_code)
        colour = _get_attribute(variant_doc, "Colour") or "default"
        size = _get_attribute(variant_doc, "Size") or ""

        if colour not in colour_groups:
            colour_groups[colour] = []

        price_info = _get_effective_price(variant_doc.name)

        colour_groups[colour].append({
            "erpItemCode": variant_doc.name,
            "sizeLabel": size,
            "stockQuantity": _get_stock(variant_doc.name),
            "price": {
                "list": price_info["list_price"],
                "effective": price_info["effective_price"],
                "saleEndsAt": price_info.get("sale_ends_at")
            }
        })

    # Build the variants list
    variants = []
    for colour_key, items in colour_groups.items():
        variants.append({
            "colorKey": colour_key,
            "items": items
        })

    # Build the full payload
    payload = {
        "templateCode": template_doc.name,
        "templateName": template_doc.item_name,
        "variants": variants
    }

    _post_to_backend(payload, template_doc.name)


def _sync_simple_item(doc):
    """Handle a simple item (no variants) as a single-SKU hierarchy."""

    price_info = _get_effective_price(doc.name)

    payload = {
        "templateCode": doc.name,
        "templateName": doc.item_name,
        "variants": [{
            "colorKey": "default",
            "items": [{
                "erpItemCode": doc.name,
                "sizeLabel": None,
                "stockQuantity": _get_stock(doc.name),
                "price": {
                    "list": price_info["list_price"],
                    "effective": price_info["effective_price"],
                    "saleEndsAt": price_info.get("sale_ends_at")
                }
            }]
        }]
    }

    _post_to_backend(payload, doc.name)


def _get_attribute(variant_doc, attribute_name):
    """Extract a specific attribute value from an Item's attributes table."""
    for attr in variant_doc.get("attributes", []):
        if attr.attribute == attribute_name:
            return attr.attribute_value
    return None


def _get_effective_price(item_code):
    """
    Get the effective price using ERPNext's pricing engine.
    Falls back to the standard selling price list if no promotions apply.
    """
    # Standard price from Item Price
    list_price_val = frappe.db.get_value(
        "Item Price",
        {"item_code": item_code, "selling": 1},
        "price_list_rate"
    )
    list_price = float(list_price_val) if list_price_val else 0.0

    # Effective price via pricing rules (promotions, discounts)
    effective_price = list_price
    sale_ends_at = None

    try:
        from erpnext.stock.get_item_details import get_item_details

        args = frappe._dict({
            "item_code": item_code,
            "price_list": frappe.db.get_single_value("Selling Settings", "selling_price_list"),
            "customer": None,
            "transaction_type": "selling",
            "doctype": "Sales Order",
            "conversion_rate": 1,
            "company": frappe.defaults.get_global_default("company"),
        })

        details = get_item_details(args)

        if details and details.get("price_list_rate"):
            effective_price = float(details.get("price_list_rate"))

        # Check for active pricing rules with valid_upto
        if details and details.get("pricing_rules"):
            rule_name = details["pricing_rules"]
            if isinstance(rule_name, str):
                valid_upto = frappe.db.get_value("Pricing Rule", rule_name, "valid_upto")
                if valid_upto:
                    sale_ends_at = str(valid_upto) + "T23:59:59Z"

    except Exception:
        # If pricing engine fails, fall back to list price
        pass

    return {
        "list_price": list_price,
        "effective_price": effective_price,
        "sale_ends_at": sale_ends_at
    }


def _get_stock(item_code):
    """Fetch total available stock across all warehouses."""
    stock_query = frappe.db.sql(
        "SELECT SUM(actual_qty) FROM `tabBin` WHERE item_code = %s",
        item_code
    )
    return int(stock_query[0][0]) if (stock_query and stock_query[0][0]) else 0


def _post_to_backend(payload, identifier):
    """POST the hierarchy payload to the Radolfa backend."""
    try:
        frappe.make_post_request(
            url=BACKEND_URL,
            headers={"Authorization": "Bearer " + JWT_TOKEN},
            json=payload
        )
    except Exception as e:
        error_msg = "Sync V2 failed for: %s. Error: %s" % (identifier, str(e))
        frappe.log_error(title="Product Sync V2 Error", message=error_msg)
