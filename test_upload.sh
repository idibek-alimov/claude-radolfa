#!/bin/bash
# Request OTP
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone": "+992902345678"}' > /dev/null

# Verify OTP to get token
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"phone": "+992902345678", "otp": "1234"}')

TOKEN=$(echo $RESPONSE | grep -o '"accessToken":"[^"]*' | grep -o '[^"]*$')

echo "Token: $TOKEN"

# Find a valid listing slug
SLUG=$(curl -s http://localhost:8080/api/v1/listings?limit=1 | grep -o '"slug":"[^"]*' | grep -o '[^"]*$' | head -n 1)

echo "Slug: $SLUG"

# Create dummy image
echo "dummy image data" > dummy.jpg

# Try to upload image
curl -s -v -X POST http://localhost:8080/api/v1/listings/$SLUG/images \
  -H "Authorization: Bearer $TOKEN" \
  -F "image=@dummy.jpg"
