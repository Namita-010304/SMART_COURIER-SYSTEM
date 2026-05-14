# Testing Deliveries Dashboard

## Overview
A new test data feature has been added to help debug and test the delivery dashboard functionality.

## How It Works

### Quick Start
1. **Login** to the application: `http://localhost:4200/login`
2. **Navigate to Dashboard**: Click "Dashboard" or go to `http://localhost:4200/customer`
3. **Click "Test Data" button** (lightning icon) in the header
4. **Wait 1-2 seconds** for data to load
5. **Dashboard updates** with statistics and delivery list

### What Gets Created
The test data generator creates **6 sample deliveries** with various statuses:

| Status | Count | Uses |
|--------|-------|------|
| DRAFT | 1 | Represents incomplete/draft deliveries |
| BOOKED | 1 | Represents confirmed bookings |
| PICKED_UP | 1 | Active delivery in progress |
| IN_TRANSIT | 1 | Package is moving |
| DELIVERED | 1 | Successfully completed |
| FAILED | 1 | Delivery that failed |

### Dashboard Statistics Updated

After clicking "Test Data", the dashboard will show:
- **Total Deliveries**: 6
- **Active Parcels**: 3 (BOOKED, PICKED_UP, IN_TRANSIT)
- **Delivered**: 1
- **Failed**: 1

## API Endpoints

### Create Test Deliveries
```
POST /gateway/deliveries/test/create-samples
Headers:
  Authorization: Bearer {token}
  X-User-Username: {username} (added automatically by gateway)

Response:
{
  "message": "Sample deliveries created for user: {username}"
}
```

### Get My Deliveries (Used automatically)
```
GET /gateway/deliveries/my
Headers:
  Authorization: Bearer {token}
  X-User-Username: {username} (added by gateway)

Response: Array of DeliveryResponseDTO objects
```

## Browser Console Debugging

Open DevTools (F12) and check the **Console** tab to see:

### Successful Load
```
Fetching deliveries from: http://localhost:9090/gateway/deliveries/my
Deliveries received from API: Array(6) [ {...}, {...}, ... ]
Data type: object Is Array: true
Stats calculated: Array(4) [ {...stats...} ]
```

### Creating Test Data
```
Creating sample deliveries from: http://localhost:9090/gateway/deliveries/test/create-samples
Test deliveries created: {"message": "Sample deliveries created for user: ..."}
```

## Troubleshooting

### Dashboard Shows "0 Deliveries"
1. Check **Network tab** in DevTools
2. Verify `/gateway/deliveries/my` returns a 200 status
3. Click "Test Data" button to generate sample data
4. Check console for error messages

### API Returns 401 Unauthorized
- ❌ Token expired or invalid
- ✅ **Solution**: Logout and login again

### API Returns 404 Not Found
- ❌ Endpoint doesn't exist
- ✅ **Solution**: Verify API gateway is running on port 9090

### Database Connection Issues
- ❌ MySQL container not running
- ✅ **Solution**: Run `docker-compose up -d` from the project root

## Test Data Properties

Each sample delivery includes:

```java
- trackingNumber: Auto-generated (SC + timestamp + random)
- username: Current logged-in user
- status: One of {DRAFT, BOOKED, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED}
- senderAddress: Generated with random city
- receiverAddress: Generated with different city
- packageDetails: 
    - weight: 2.5 + index kg
    - dimensions: 10x10x10 cm
    - serviceType: DOMESTIC
    - declaredValue: 100 + (index * 50)
    - fragile: Alternates (every other one)
- charge: $250.00 (fixed)
- paid: Alternates (every other one)
- createdAt: Current timestamp
```

## Resetting Data

### Option 1: Delete via Database
Connect to MySQL and run:
```sql
DELETE FROM delivery_db.deliveries WHERE username = 'your_username';
```

### Option 2: Use New User Account
- Create a new account via signup
- Login with new credentials
- Click "Test Data" to generate fresh test deliveries

## Performance Notes

- Creating 6 test deliveries typically takes **0.5-1 second**
- Dashboard refresh after creation: **< 100ms**
- Each API call includes full delivery details and nested address/package data

## File Modifications

### Backend
- `DeliveryController.java`: Added POST `/test/create-samples` endpoint
- `DeliveryService.java`: Added `createSampleDeliveries()` method

### Frontend
- `delivery.ts`: Added `createSampleDeliveries()` service method
- `customer.ts`: Added `createTestDeliveries()` component method + enhanced logging
- `customer.html`: Added "Test Data" button with loading state

## Next Steps

After verifying the dashboard works with test data:
1. ✅ Test creating real deliveries via the wizard
2. ✅ Test delivery status transitions
3. ✅ Test tracking functionality
4. ✅ Test delivery filtering and sorting

