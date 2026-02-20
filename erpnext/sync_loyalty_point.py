app_settings = frappe.get_doc("App Settings")
BACKEND_URL = app_settings.backend_url + "/api/v1/sync/loyalty"
SYSTEM_API_KEY = app_settings.system_api_key



if not doc.customer: 
    frappe.msgprint("Loyalty point entry No customer") 
else: 
    # Fetch customer to get phone number 
    customer = frappe.db.get_value( 
        "Customer", 
        doc.customer, 
        ["mobile_no"], 
        as_dict=True 
    )
    
    if not customer or not customer.mobile_no: 
        frappe.msgprint("Loyalty points no customer or mobile_no") 
    else: 
        current_points = frappe.db.sql("""
            SELECT 
                SUM(loyalty_points) as total_points
            FROM 
                `tabLoyalty Point Entry`
            WHERE 
                customer = %s
                AND (expiry_date >= CURDATE() OR expiry_date IS NULL)
        """, doc.customer)[0][0] or 0
        payload = { "phone": customer.mobile_no, "points": current_points } 
    # Idempotency key tied to immutable entry 
    idempotency_key = "LOYALTY_ENTRY:" + doc.name 
    
    try: 
        frappe.make_post_request(
            url=BACKEND_URL,
            headers={
                "X-Api-Key": SYSTEM_API_KEY,
                "Idempotency-Key": idempotency_key
            },
            json=payload
        )
    except Exception as e: 
        frappe.log_error( 
            title="Loyalty Sync Failed", 
            message="Hello" 
        )