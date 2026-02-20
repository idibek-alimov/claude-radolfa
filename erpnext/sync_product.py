# NO IMPORTS, NO AS_JSON, NO FORMATTING ATTRIBUTES

# Constants
app_settings = frappe.get_doc("App Settings")
BACKEND_URL = app_settings.backend_url + "/api/v1/sync/products"
SYSTEM_API_KEY = app_settings.system_api_key

# 1. Fetch Price
# Simple get_value call.
price_val = frappe.db.get_value("Item Price", {"item_code": doc.item_code, "selling": 1}, "price_list_rate")
final_price = float(price_val) if price_val else 0.0

# 2. Fetch Stock
# Direct SQL to avoid any complex ORM overhead.
stock_query = frappe.db.sql("SELECT SUM(actual_qty) FROM `tabBin` WHERE item_code = %s", doc.item_code)
final_stock = int(stock_query[0][0]) if (stock_query and stock_query[0][0]) else 0

# 3. Build the Payload
# We keep this as a standard Python object (list of dicts).
payload = [{
    "erpId": doc.item_code,
    "name": doc.item_name,
    "price": final_price,
    "stock": final_stock
}]

try:
    # 4. The Magic Key: use 'json=' instead of 'data='
    # This tells the internal 'requests' library to:
    # a) Serialize the list to JSON automatically
    # b) Set the Content-Type to application/json automatically
    frappe.make_post_request(
        url=BACKEND_URL,
        headers={"X-Api-Key": SYSTEM_API_KEY},
        json=payload
    )

except Exception as e:
    # 5. Safe Error Logging
    # We use % formatting because .format() is blocked as 'unsafe'.
    error_msg = "Sync failed for item: %s. Error: %s" % (doc.item_code, str(e))
    frappe.log_error(title="Product Sync Error", message=error_msg)