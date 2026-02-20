app_settings = frappe.get_doc("App Settings")
BACKEND_URL = app_settings.backend_url + "/api/v1/sync/users"
SYSTEM_API_KEY = app_settings.system_api_key

payload = {
        "phone": doc.mobile_no,
        "name": doc.customer_name,
        "email": doc.email_id,
        "role": "USER",
        "enabled": 1,
        "loyaltyPoints": 0
    }

try:
    frappe.make_post_request(
        url=BACKEND_URL,
        headers={
            "X-Api-Key": SYSTEM_API_KEY
        },
        json=payload
    )
except Exception as e:
    frappe.log_error(
        title="User Sync Failed",
        message=str(e)
    )