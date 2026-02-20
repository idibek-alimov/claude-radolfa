app_settings = frappe.get_doc("App Settings")
BACKEND_URL = app_settings.backend_url + "/api/v1/sync/categories"
SYSTEM_API_KEY = app_settings.system_api_key

# def after_insert(doc, method=None):
#

payload = {
    "categories": [{
        "name": doc.name,
        "parentName": doc.parent_item_group
    }]
}
frappe.msgprint("Payload: %s" % (payload))
try:
    frappe.msgprint("Trying request BACKEND_URL : %s" %(BACKEND_URL))
    frappe.make_post_request(
        url=BACKEND_URL,
        headers={
            "X-Api-Key": SYSTEM_API_KEY
        },
        json=payload
    )
except Exception as e:
    frappe.log_error(
        title="Single Category Sync Failed",
        message=str(e)
    )