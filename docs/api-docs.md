# API DOCS - Ứng dụng chia sẻ lộ trình

> **Base URL:** `http://localhost:8080/api/v1`
> **Version:** 1.0.0
> **Last Updated:** 2026-07-28

---

## 1. Quy ước chung

### 1.1. HTTP Methods

| Method   | Mục đích                      |
| -------- | ----------------------------- |
| `GET`    | Lấy dữ liệu                  |
| `POST`   | Tạo mới tài nguyên            |
| `PUT`    | Cập nhật toàn bộ tài nguyên   |
| `PATCH`  | Cập nhật một phần tài nguyên  |
| `DELETE` | Xóa tài nguyên                |

### 1.2. HTTP Status Codes

| Code  | Ý nghĩa                                          |
| ----- | ------------------------------------------------- |
| `200` | Thành công                                        |
| `201` | Tạo mới thành công                                |
| `204` | Thành công, không có nội dung trả về              |
| `400` | Yêu cầu không hợp lệ (Bad Request)               |
| `401` | Chưa xác thực (Unauthorized)                      |
| `403` | Không có quyền truy cập (Forbidden)               |
| `404` | Không tìm thấy tài nguyên (Not Found)             |
| `409` | Xung đột dữ liệu (Conflict)                      |
| `422` | Dữ liệu không thể xử lý (Unprocessable Entity)   |
| `500` | Lỗi máy chủ (Internal Server Error)               |

### 1.3. Response Format chuẩn

**Thành công (Success):**

```json
{
  "status": 200,
  "message": "Mô tả kết quả",
  "data": { },
  "meta": { } // Chứa những thông tin bổ sung liên quan đến data
}
```

**Thành công với phân trang (Paginated):**

```json
{
  "status": 200,
  "message": "Mô tả kết quả",
  "data": {},
  "meta": {
    "page": 2,          // Trang hiện tại
    "limit": 10,        // Số lượng item/trang
    "totalItems": 505, // Tổng số bản ghi trong DB (để tính ra số trang cuối)
    "totalPages": 51   // Tổng số trang (Backend tính sẵn hộ Frontend luôn)
  }
}
```

**Lỗi (Error):**

```json
{
  "status": 400,
  "message": "Mô tả lỗi chung",
  "errors": {
    "fieldName": "Mô tả lỗi cụ thể cho field"
  },
  "meta": null
}
```

### 1.4. Quy ước đặt tên

- **URL:** dùng `kebab-case` và danh từ số nhiều → `/api/v1/dish-categories`
- **Request/Response body:** dùng `camelCase` → `firstName`, `tableNumber`
- **Query params:** dùng `camelCase` → `?pageSize=10&sortBy=createdAt`


---

## UC04: Khởi tạo hành trình

### 4.1. Bắt đầu hành trình (Start Trip)

**`POST /api/v1/trips`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Dùng để tài xế xác nhận xuất phát. Hệ thống sẽ chuyển trạng thái lộ trình, khởi tạo vòng đời chuyến đi, sinh danh sách điểm dừng (đánh số thứ tự nghiêm ngặt) và trả về đường vẽ Polyline từ Map Service. |
| **Auth** | Có (Bearer Token) |
| **Role** | Driver (Yêu cầu Permission: `ACTION_START_TRIP`) |

**Request Body (JSON):**

| Param | Type | Required | Default | Mô tả |
| --- | --- | --- | --- | --- |
| `sharedRouteId` | `integer` | Yes | - | ID của Lộ trình chia sẻ đã được ghép cặp |
| `currentLocation` | `object` | Yes | - | Tọa độ GPS hiện tại của tài xế |
| `currentLocation.latitude` | `double` | Yes | - | Vĩ độ hiện tại |
| `currentLocation.longitude` | `double` | Yes | - | Kinh độ hiện tại |

**Example Request:**

```json
{
  "sharedRouteId": 1024,
  "currentLocation": {
    "latitude": 10.878123,
    "longitude": 106.801234
  }
}

```

**Response `201 Created`:**

```json
{
  "status": 201,
  "message": "Bắt đầu hành trình thành công.",
  "data": { 
    "tripId": 5001,
    "tripStatus": "ON_THE_WAY",
    "sharedRouteId": 1024,
    "status": "ON_THE_WAY",
    "routingDetails": {
      "totalDistanceMeters": 2828, 
      "totalDurationSeconds": 79799,
      "overviewPolyline": "mtm_C{cudSG@eAfDc@xAq@",
      "mapBounds": { 
        "northeast": { "lat": 21.1023, "lng": 105.9123 },
        "southwest": { "lat": 20.9412, "lng": 105.7834 }
      }
    },
    "waypoints": [
      {
        "waypointId": 8001,
        "sequence": 0,
        "type": "START_DRIVER",
        "status": "PENDING",
        "address": "Ký túc xá Khu A, ĐHQG",
        "location": { "latitude": 10.8781, "longitude": 106.8012 },
        "passengerInfo": null,
        "authCacheState": null,
        "receipt": null
      },
      {
        "waypointId": 8002,
        "sequence": 1,
        "type": "PICKUP",
        "status": "PENDING",
        "address": "Trạm xe buýt Suối Tiên",
        "location": { "latitude": 10.8652, "longitude": 106.8011 },
        "passengerInfo": {
          "requestId": 3001,
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        },
        "authCacheState": null,
        "receipt": null
      }
      // ... Các trạm DROPOFF và END_DRIVER tương tự
    ]
  },      
  "meta": null
}
```

**Response `4xx`:**

```json
{
  "status": 400,
  "message": "Không thể bắt đầu hành trình",
  "errors": { 
      "sharedRouteId": "Lộ trình chia sẻ này chưa có khách hàng ghép cặp hoặc đã bị hủy."
  }
}

```

---

### 4.2. Lấy thông tin chuyến đi hiện tại (Resume Trip)

**`GET /api/v1/trips/current`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Dùng để khôi phục (resume) giao diện điều hướng khi tài xế vô tình tắt App (Kill App), mất mạng rồi mở lại. Backend tự xác định chuyến đi dựa vào JWT. |
| **Auth** | Có (Bearer Token) |
| **Role** | Driver (Yêu cầu Permission: `ACTION_VIEW_TRIP_DETAILS`) |

**Query Parameters:**
*Không yêu cầu. Backend tự động trích xuất `userId` từ JWT Token để tìm `ChuyenDi` đang ở trạng thái `ON_THE_WAY` hoặc `SUSPICIOUS_LOCKED`.*

**Response `200 OK`:**

```json
{
  "status": 201,
  "message": "Bắt đầu hành trình thành công.",
  "data": { 
    "tripId": 5001,
    "status": "ON_THE_WAY",
    "sharedRouteId": 1024,
    "shareRouteStatus": "ON_THE_WAY",
    "routingDetails": {
      "totalDistanceMeters": 2828, 
      "totalDurationSeconds": 79799,
      "overviewPolyline": "mtm_C{cudSG@eAfDc@xAq@",
      "mapBounds": { 
        "northeast": { "lat": 21.1023, "lng": 105.9123 },
        "southwest": { "lat": 20.9412, "lng": 105.7834 }
      }
    },
    "waypoints": [
      {
        "waypointId": 8001,
        "sequence": 0,
        "type": "START_DRIVER",
        "status": "PENDING",
        "address": "Ký túc xá Khu A, ĐHQG",
        "location": { "latitude": 10.8781, "longitude": 106.8012 },
        "passengerInfo": null,
        "authCacheState": null,
        "receipt": null
      },
      {
        "waypointId": 8002,
        "sequence": 1,
        "type": "PICKUP",
        "status": "PENDING",
        "address": "Trạm xe buýt Suối Tiên",
        "location": { "latitude": 10.8652, "longitude": 106.8011 },
        "passengerInfo": {
          "requestId": 3001,
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        },
        "authCacheState": null,
        "receipt": null
      }
      // ... Các trạm DROPOFF và END_DRIVER tương tự
    ]
  },      
  "meta": null
}
```

**Response `4xx`:**

```json
{
  "status": 404,
  "message": "Không tìm thấy chuyến đi nào đang hoạt động",
  "errors": null
}

```

### 4.3. Lấy thông tin yêu cầu chuyến đi hiện tại (Dành cho Hành Khách)

**`GET /api/v1/ride-requests/current`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Dùng để khôi phục màn hình theo dõi chuyến đi của hành khách khi mở lại App. Backend sẽ dựa vào JWT để tìm Yêu cầu ghép cặp đang ở trạng thái `ACCEPTED` (đang đợi xe) hoặc `IN_TRANSIT` (đang trên xe) của hành khách này. |
| **Auth** | Có (Bearer Token) |
| **Role** | Passenger (Yêu cầu Permission: `ACTION_VIEW_MY_TRIP`) |

**Query Parameters:**
*Không yêu cầu. Backend tự động trích xuất `userId` từ JWT Token.*

**Response `200 OK`:**

```json
{
  "status": 200,
  "message": "Tải dữ liệu chuyến đi hiện tại thành công",
  "data": {
    "requestId": 3001,
    "tripId": 5001,
    "tripStatus": "ON_THE_WAY",
    "requestStatus": "IN_TRANSIT",
    "supportAmount": 25000,
    "driverInfo": {
      "driverId": 881,
      "fullName": "Trần Văn Tài",
      "avatarUrl": "https://s3.example.com/avatar_881.jpg",
      "phoneNumber": "0987654321",
      "vehicleInfo": {
        "plateNumber": "59E1-123.45",
        "model": "Honda AirBlade 150",
        "color": "Đen"
      }
    },
    "routingDetails": {
      "overviewPolyline": "mtm_C{cudSG@eAfDc@xAq@",
      "mapBounds": {
        "northeast": { "lat": 21.1023, "lng": 105.9123 },
        "southwest": { "lat": 20.9412, "lng": 105.7834 }
      }
    },
    "waypoints": [  // CHỈ trả về đúng điểm Đón và Thả của user này (Bảo mật quyền riêng tư)
      {
        "waypointId": 8002,
        "type": "PICKUP",
        "sequence": 1,
        "status": "COMPLETED", // Ví dụ khách đã lên xe, trạng thái là COMPLETED
        "address": "Trạm xe buýt Suối Tiên",
        "location": {
          "latitude": 10.8652,
          "longitude": 106.8011
        },
        "passengerInfo": {
          "requestId": 3001,
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        },
        "authCacheState": { // Đã được cấp dữ liệu
          "isDriverVerified": true, // Tài xế đã xác nhận
          "isPassengerVerified": true, // Hành khách chưa xác nhận
          "driverFailCount": 0,
          "passengerFailCount": 0,
          "allVerified": true
        },
        "receipt": null
      },
      {
        "waypointId": 8003,
        "type": "DROPOFF",
        "sequence": 2,
        "status": "PENDING",   // Trạm thả chưa đến nên PENDING
        "address": "Trường Đại học Bách Khoa CS1",
        "location": {
          "latitude": 10.7732,
          "longitude": 106.6593
        },
        "passengerInfo": {
          "requestId": 3001,
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        },
        "authCacheState": null,
        "receipt": null
      }
    ]
  },
  "meta": {
    "driverLastLocation": {  
      "latitude": 10.8710,
      "longitude": 106.8000
    },
    "realtimeTracking": {
      "provider": "WEBSOCKET",      
      "topicUrl": "/topic/trips/5001/location"
    }
  }
}

```

**Response `4xx`:**

```json
{
  "status": 404,
  "message": "Bạn không có chuyến đi nào đang diễn ra.",
  "errors": null
}

```

---

## UC05: Xác thực điểm dừng

### 5.1. Khách hàng xem OTP Chuyến đi (Dự phòng FaceID)

**`POST /api/v1/ride-requests/{requestId}/otp`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Khách hàng nhập Master PIN cá nhân để lấy OTP 4 số. Mã này dùng để đọc cho tài xế nhập trong trường hợp hệ thống AI nhận diện khuôn mặt tại điểm dừng bị lỗi. |
| **Auth** | Có (Bearer Token) |
| **Role** | Passenger (Yêu cầu Permission: `ACTION_VIEW_TRIP_OTP`) |

**Request Body (JSON):**

| Param | Type | Required | Default | Mô tả |
| --- | --- | --- | --- | --- |
| `masterPin` | `string` | Yes | - | Mã PIN cá nhân của hành khách (đã băm SHA-256 từ FE hoặc gửi plaintext tùy chuẩn bảo mật). |

**Response `200 OK` (Thành công):**

```json
{
  "status": 200,
  "message": "Lấy mã OTP chuyến đi thành công.",
  "data": {
    "requestId": 3001,
    "otpCode": "8241",
    "expiresAt": "2026-07-27T13:45:00Z",
    "tripContext": { 
      "driverName": "Trần Văn Tài",
      "vehiclePlate": "59E1-123.45"
    }
  },
  "meta": {}
}

```

**Response `400 Bad Request` (Sai mã PIN):**

```json
{
  "status": 400,
  "message": "Mã PIN không chính xác.",
  "errors": {
    "masterPin": "Mã PIN sai. Vui lòng thử lại."
  },
  "meta": {
    "failCount": 3,
    "remainingAttempts": 2 
  }
}

```

---

### 5.2. Tài xế Xác nhận Đến nơi (Check-in & Khởi tạo Cache)

**`POST /api/v1/waypoints/{waypointId}/arrive`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Tài xế xác nhận đã đến đúng vị trí GPS. API kiểm tra khoảng cách (<= 50m), thứ tự trạm (chống nhảy cóc) và mở khóa màn hình "Xác thực danh tính" trên App tài xế. |
| **Auth** | Có (Bearer Token) |
| **Role** | Driver (Yêu cầu Permission: `ACTION_ARRIVE_WAYPOINT`) |

**Request Body (JSON):**

| Param | Type | Required | Mô tả |
| --- | --- | --- | --- |
| `currentLocation.latitude` | `double` | Yes | Vĩ độ hiện tại để Backend validate khoảng cách. |
| `currentLocation.longitude` | `double` | Yes | Kinh độ hiện tại. |

**Response `200 OK` (Kịch bản tại trạm PICKUP / DROPOFF - Có khách):**

```json
{
  "status": 200,
  "message": "Xác nhận đến nơi thành công. Hệ thống đã mở khóa xác thực.",
  "data": {
    "waypointId": 8002,
    "type": "PICKUP",
    "status": "ARRIVED",
    "sequence": 1,
    "address": "Trạm xe buýt Suối Tiên",
    "location": {
      "latitude": 10.8652,
      "longitude": 106.8011
    },
    "authCacheState": {
      "isDriverVerified": false,
      "isPassengerVerified": false,
      "driverFailCount": 0,
      "passengerFailCount": 0,
      "allVerified": false
    },
    "passengerInfo": {
      "requestId": 3001,
      "passengerId": 992,
      "fullName": "Nguyễn Văn Khách",
      "avatarUrl": "https://s3.example.com/avatar_992.jpg",
      "phoneNumber": "0901234567",
      "supportAmount": 25000,
      "paymentMethod": "CASH"
    }
  },
  "meta": null
}

```

**Response `200 OK` (Kịch bản tại trạm START / END - Không có khách):**

```json
{
  "status": 200,
  "message": "Đến điểm xuất phát. Vui lòng xác thực tài xế.",
  "data": {
    "waypointId": 8001,
    "type": "START_DRIVER",
    "sequence": 0,
    "address": "Trạm xe buýt Suối Tiên",
    "location": {
      "latitude": 10.8652,
      "longitude": 106.8011
    },
    "status": "ARRIVED",
    "authCacheState": {
      "isDriverVerified": false,
      "driverFailCount": 0,
      "allVerified": false
      //Không có thuộc tính của passenger
    },
    "passengerInfo": null 
  },
  "meta": null
}

```

**Response `400 Bad Request` (Bị chặn bởi rào cản Không gian / Thứ tự):**

```json
{
  "status": 400,
  "message": "Không thể xác nhận đến nơi.",
  "errors": {
    "sequence": "Lỗi nhảy cóc. Bạn chưa hoàn thành điểm dừng trước đó.",
    //"currentLocation": "Bạn đang cách điểm dừng 450m. Phải dưới 50m mới được check-in."
  }
}

```

---

### 5.3. Xác thực Danh tính (FaceID / OTP)

**`POST /api/v1/waypoints/{waypointId}/verify`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | API lõi xử lý đối chiếu danh tính. Chấp nhận File ảnh (FaceID) hoặc Text (OTP). Thực thi thuật toán 3-Strikes (Bạo động nếu sai quá 3 lần). |
| **Content-Type** | `multipart/form-data` |
| **Auth** | Có (Bearer Token của Tài xế đang cầm máy) |

**Request Body (FormData):**

| Param | Type | Required | Mô tả |
| --- | --- | --- | --- |
| `targetType` | `string` | Yes | Enum: `CHUYEN_DI_TAI_XE` hoặc `YEU_CAU_HANH_KHACH` |
| `authMethod` | `string` | Yes | Enum: `FACE_ID` hoặc `OTP` |
| `duLieuFile` | `file` | No | Bắt buộc có nếu `authMethod` = `FACE_ID`. |
| `duLieuString` | `string` | No | Bắt buộc có nếu `authMethod` = `OTP`. |

**Response `200 OK` (Thành công - Bật Checkmark xanh trên UI):**

```json
{
  "status": 200,
  "message": "Xác thực hành khách thành công.",
  "data": {
    "waypointId": 8002,
    "status": "COMPLETED",
    "type": "PICKUP",
    "sequence": 1,
    "address": "Trạm xe buýt Suối Tiên",
    "location": {
      "latitude": 10.8652,
      "longitude": 106.8011
    },
    "authCacheState": {
      "isDriverVerified": true,     
      "isPassengerVerified": true,  
      "driverFailCount": 0,
      "passengerFailCount": 0,
      "allVerified": true 
    }
  },
  "meta": null
}

```

**Response `400 Bad Request` (Sai dưới 3 lần):**

```json
{
  "status": 400,
  "message": "Xác thực thất bại.",
  "errors": { "auth": "Khuôn mặt hoặc mã OTP không chính xác." },
  "meta": {
    "failedRole": "PASSENGER", 
    "failCount": 2,
    "remainingAttempts": 1
  }
}

```

**Response `403 Forbidden` (Bạo động Cục bộ - Hủy ghép cặp chuyến):**
```json
{
  "status": 403,
  "message": "Hành khách xác thực thất bại 3 lần. Đã hủy ghép cặp chuyến này.",
  "errors": {
    "auth": "Vượt quá số lần cho phép. Yêu cầu của khách hàng bị hủy."
  },
  "meta": {
    "failedRole": "PASSENGER",
    "freedSeats": 1, 
    "affectedWaypoints": [
      {
        "waypointId": 8002,
        "location": {
          "latitude": 10.8652,
          "longitude": 106.8011
        },
        "address": "Trạm xe buýt Suối Tiên",
        "sequence": 1,
        "type": "PICKUP",
        "status": "CANCELLED",
        "passengerInfo": {
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        }
      },
      {
        "waypointId": 8003,
        "location": {
          "latitude": 10.9000,
          "longitude": 106.7000
        },
        "address": "Điểm trả khách xa lộ Hà Nội",
        "sequence": 2,
        "type": "DROPOFF",
        "status": "CANCELLED",
        "passengerInfo": {
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        }
      }
    ]
  }
}
```


**Response `423 Locked` (Bạo động Toàn cục - Khóa xe do sai 3 lần nguy hiểm):**

```json
{
  "status": 423,
  "message": "BẠO ĐỘNG AN NINH: Khóa chuyến đi do Tài xế giả mạo.",
  "errors": { 
    "security": "Chuyến đi bị khóa. Ứng dụng bị đình chỉ để An ninh xác minh." 
  },
  "meta": {
    "tripId": 5001,
    "status": "SUSPICIOUS_LOCKED",
    "sharedRouteId": 1024,
    "sharedRouteStatus": "ABORTED_MIDWAY",
    "routingDetails": {
      "totalDistanceMeters": 2828,
      "totalDurationSeconds": 79799,
      "overviewPolyline": "mtm_C{cudSG@eAfDc@xAq@",
      "mapBounds": { 
        "northeast": { "lat": 21.1023, "lng": 105.9123 },
        "southwest": { "lat": 20.9412, "lng": 105.7834 }
      }
    },
    "failedRole": "DRIVER",
    "securityReportLevel": "CRITICAL",
    "frozenWaypoints": [
      {
        "waypointId": 8001,
        "status": "COMPLETED",
        "location": {
          "latitude": 10.8652,
          "longitude": 106.8011
        },
        "address": "Trạm xe buýt Suối Tiên",
        "sequence": 0,
        "type": "START_DRIVER",
        "passengerInfo": null,
        "receipt": null
      },
      {
        "waypointId": 8002,
        "status": "COMPLETED",
        "location": {
          "latitude": 10.8652,
          "longitude": 106.8011
        },
        "address": "Trạm xe buýt Suối Tiên",
        "sequence": 1,
        "type": "PICKUP",
        "passengerInfo": {
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        },
        "receipt": null
      },
      {
        "waypointId": 8003,
        "status": "FROZEN",
        "location": {
          "latitude": 10.9000,
          "longitude": 106.7000
        },
        "address": "Điểm trả khách xa lộ Hà Nội",
        "sequence": 2,
        "type": "DROPOFF",
        "passengerInfo": {
          "passengerId": 992,
          "fullName": "Nguyễn Văn Khách",
          "avatarUrl": "https://s3.example.com/avatar_992.jpg",
          "phoneNumber": "0901234567",
          "supportAmount": 25000,
          "paymentMethod": "CASH"
        },
        "receipt": null
      },
      {
        "waypointId": 8004,
        "status": "FROZEN",
        "location": {
          "latitude": 10.9300,
          "longitude": 106.8500
        },
        "address": "Điểm trả khách xa lộ Hà Nội",
        "sequence": 3,
        "type": "END_DRIVER",
        "passengerInfo": null,
        "receipt": null
      }
    ]
  }
}

```

---

### 5.4. Chốt Hoàn thành Trạm & Sinh Biên lai

**`POST /api/v1/waypoints/{waypointId}/complete`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Cập nhật trạng thái trạm thành COMPLETED. Tùy thuộc vào loại trạm (PICKUP, DROPOFF, END) sẽ trả về dữ liệu tương ứng (Biên lai, Tổng kết). |
| **Auth** | Có (Bearer Token) |
| **Role** | Driver |

**Request Body:** *Không yêu cầu (Chỉ truyền ID qua URL)*

**Response `200 OK` (Kịch bản tại trạm DROPOFF - Có Biên lai):**

```json
{
  "status": 200,
  "message": "Hoàn thành điểm thả khách.",
  "data": {
    "tripStatus": "ON_THE_WAY",
    "waypoint": {
      "waypointId": 8003,
      "sequence": 2,
      "type": "DROPOFF",
      "status": "COMPLETED", 
      "authCacheState": null,
      "receipt": { // Đã sinh biên lai
        "supportAmount": 25000,
      }
    }
  },
  "meta": null
}

```

**Response `200 OK` (Kịch bản tại trạm cuổi cùng END_DRIVER - Tổng kết Chuyến):**

```json
{
  "status": 200,
  "message": "Chuyến đi đã kết thúc thành công.",
  "data": {
    "waypointId": 8004,
    "location": {
      "latitude": 10.9300,
      "longitude": 106.8500
    },
    "address": "Điểm trả khách xa lộ Hà Nội",
    "sequence": 3,
    "type": "END_DRIVER",
    "status": "COMPLETED",
    "tripStatus": "COMPLETED",
    "tripSummary": { // Render màn hình chúc mừng
      "totalEarned": 75000,     
      "totalDistanceKm": 12.5,
      "completedRequests": 3    
    },
  },
  "meta": null
}

```

---

### 5.5. Tài xế Hủy khách (Chỉ được gọi tại trạm PICKUP)

**`POST /api/v1/waypoints/{waypointId}/cancel-passenger`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Tài xế từ chối khách tại điểm PICKUP do khách không ra (No show) hoặc vi phạm chính sách. Hoàn trả ghế trống cho lộ trình. |
| **Auth** | Có (Bearer Token) |

**Request Body (JSON):**

| Param | Type | Required | Mô tả |
| --- | --- | --- | --- |
| `reasonCode` | `string` | Yes | `NO_SHOW` (Khách không ra), `VIOLATION` (Sai quy định) |
| `note` | `string` | No | Ghi chú thêm của tài xế |
| `evidenceImages` | `array` | No | Mảng URL ảnh bằng chứng (nếu có) |

**Response `200 OK` (Thành công):**

```json
{
  "status": 200,
  "message": "Đã hủy hành khách thành công.",
  "data": {
    "requestId": 3001,
    "affectedWaypoints": [
      {
        "waypointId": 8002,
        "sequence": 1,
        "type": "PICKUP",
        "status": "CANCELLED", // Đã update
        // ... (address, location, passengerInfo giữ nguyên)
        "authCacheState": null,
        "receipt": null
      },
      {
        "waypointId": 8003,
        "sequence": 2,
        "type": "DROPOFF",
        "status": "CANCELLED", // Đã update
        // ... (address, location, passengerInfo giữ nguyên)
        "authCacheState": null,
        "receipt": null
      }
    ]
  },
  "meta": null
}

```

---

### 5.6. Hành khách Bấm SOS (Bạo động Toàn cục)

**`POST /api/v1/trips/{tripId}/sos`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Báo động khẩn cấp từ hành khách. Hủy toàn bộ chuyến đi, phong tỏa hệ thống, đánh dấu SOS và yêu cầu hỗ trợ. |
| **Auth** | Có (Bearer Token của Hành khách) |

**Request Body (JSON):**

| Param | Type | Required | Mô tả |
| --- | --- | --- | --- |
| `currentLocation.lat` | `double` | Yes | Vĩ độ hiện tại lúc bấm SOS |
| `currentLocation.lng` | `double` | Yes | Kinh độ hiện tại lúc bấm SOS |
| `reasonCategory` | `string` | Yes | `ACCIDENT` (Tai nạn), `THREAT` (Đe dọa), `OTHER` |

**Response `200 OK` (Gửi SOS thành công):**

```json
{
  "status": 200,
  "message": "TÍN HIỆU SOS ĐÃ ĐƯỢC GỬI. HỆ THỐNG ĐANG GỌI HỖ TRỢ.",
  "data": {
    "tripId": 5001,
    "tripStatus": "EMERGENCY_ABORTED",
    "securityReportId": "SEC-2026-9912",
    "instructions": "Vui lòng giữ bình tĩnh. Đội an ninh đang liên hệ với bạn.",
    "hotline": "1900-1111"
  },
  "meta": {
    "recordedLocation": {
       "latitude": 10.8652,
       "longitude": 106.8011
    }
  }
}

```

---


## UC07: Đặt chuyến đi chung

### 7.1. Tìm kiếm chuyến đi chung (Bản fix chuẩn Phân trang)

**`GET /api/v1/shared-routes/search`**

**Query Parameters (Đã bổ sung chuẩn phân trang):**

| Param | Type | Required | Default | Mô tả |
| --- | --- | --- | --- | --- |
| `pickupLat` | `double` | Yes | - | Vĩ độ điểm đón mong muốn |
| `pickupLng` | `double` | Yes | - | Kinh độ điểm đón mong muốn |
| `dropoffLat` | `double` | Yes | - | Vĩ độ điểm đích mong muốn |
| `dropoffLng` | `double` | Yes | - | Kinh độ điểm đích mong muốn |
| `page` | `integer` | No | `0` | **Trang hiện tại (Bắt đầu từ 0)** |
| `limit` | `integer` | No | `10` | **Số lượng kết quả mỗi trang** |

**Response `200 OK` (Thành công):**

```json
{
  "status": 200,
  "message": "Tìm kiếm chuyến đi thành công.",
  "data": [
    {
      "sharedRouteId": 1024,
      "driver": {
        "fullName": "Trần Văn Tài",
        "avatarUrl": "https://s3.example.com/avatar_881.jpg",
        "rating": 4.9
      },
      "tier": 1, 
      "overlapPercentage": 100.0,
      "distanceToPickupMeters": 850, 
      "availableSeats": 2
    },
    {
      "routeId": 1025,
      "driver": {
        "fullName": "Nguyễn Hữu B",
        "avatarUrl": "https://s3.example.com/avatar_882.jpg",
        "rating": 4.7
      },
      "tier": 2,
      "overlapPercentage": 85.5, 
      "distanceToPickupMeters": 1200,
      "availableSeats": 1
    }
  ],
  "meta": {
    "page": 0,           
    "limit": 10,         
    "totalItems": 45,    
    "totalPages": 5      
  }
}

```
---

### 7.2. Xem chi tiết Chuyến đi (Trực quan hóa Bản đồ)

**`GET /api/v1/shared-routes/{routeId}/details`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Phản ánh **Sự kiện 2: xemChiTietChuyenDi**. Tổng hợp thông tin Tài xế, Xe và render Polyline liền mạch nối 3 điểm (Tài xế hiện tại -> Đón -> Thả). |
| **Auth** | Có (Bearer Token) |
| **Role** | Passenger |

**Query Parameters:**
| Param | Type | Required | Mô tả |
| --- | --- | --- | --- |
| `pickupLat` | `double` | Yes | Vĩ độ điểm đón mong muốn |
| `pickupLng` | `double` | Yes | Kinh độ điểm đón mong muốn |
| `dropoffLat` | `double` | Yes | Vĩ độ điểm đích (Đích thật hoặc Điểm thả ảo) |
| `dropoffLng` | `double` | Yes | Kinh độ điểm đích |

**Response `200 OK` (Trả về `ChiTietChuyenDiDTO`):**

```json
{
  "status": 200,
  "message": "Lấy chi tiết chuyến đi thành công.",
  "data": {
    "routeId": 1025,
    "driverInfo": {
      "driverId": 882,
      "fullName": "Nguyễn Hữu B",
      "avatarUrl": "https://s3.example.com/avatar_882.jpg",
      "rating": 4.7,
      "totalCompletedTrips": 150
    },
    "vehicleInfo": {
      "plateNumber": "59E1-123.45",
      "model": "Honda AirBlade 150",
      "color": "Đen"
    },
    "routingDetails": {
      "pickupMarker": { "lat": 10.8652, "lng": 106.8011 },
      "dropoffMarker": { "lat": 10.7732, "lng": 106.6593 },
      "overviewPolyline": "mtm_C{cudSG@eAfDc@xAq@", // Chuỗi vẽ đường liền mạch
      "totalEstimatedDistanceMeters": 15400 // Tổng quãng đường của khách
    }
  },
  "meta": null
}

```

---

### 7.3. Gửi Yêu cầu Ghép cặp (Đặt chuyến)

**`POST /api/v1/ride-requests`**

| Thuộc tính | Giá trị |
| --- | --- |
| **Summary** | Phản ánh **Sự kiện 3: guiYeuCauGhepCap**. Qua 4 rào cản Fail-Fast, tạo mới Yêu Cầu Ghép Cặp (trạng thái PENDING) và các Điểm dừng. |
| **Auth** | Có (Bearer Token) |
| **Role** | Passenger (Yêu cầu Permission: `ACTION_BOOK_RIDE`) |

**Request Body (JSON):**

| Param | Type | Required | Mô tả |
| --- | --- | --- | --- |
| `sharedRouteId` | `integer` | Yes | ID của chuyến xe được chọn (`chuyenDiDaChon`) |
| `supportAmount` | `double` | Yes | Mức chi phí hỗ trợ đề xuất (`mucHoTro`) |
| `pickupLocation` | `object` | Yes | `{ lat, lng }` (Điểm đón thật) |
| `realDropoffLocation` | `object` | Yes | `{ lat, lng }` (Điểm đích thật) |
| `virtualDropoffLocation` | `object` | No | `{ lat, lng }` (Điểm thả ảo, chỉ bắt buộc nếu chọn chuyến Tầng 2) |

**Example Request:**

```json
{
  "sharedRouteId": 1025,
  "supportAmount": 30000,
  "pickupLocation": { "lat": 10.8652, "lng": 106.8011 },
  "realDropoffLocation": { "lat": 10.7732, "lng": 106.6593 },
  "virtualDropoffLocation": { "lat": 10.7712, "lng": 106.6600 } 
}

```

**Response `201 Created` (Thành công qua 4 rào cản):**

```json
{
  "status": 201,
  "message": "Gửi yêu cầu đi chung thành công. Đang chờ tài xế duyệt.",
  "data": {
    "requestId": 3001,
    "sharedRouteId": 1025,
    "supportAmount": 30000,
    "status": "PENDING",
    "createdAt": "2026-07-27T17:30:00Z"
  },
  "meta": {}
}

```

**Response Các luồng Ngoại lệ (4 Rào cản Fail-Fast):**

```json
// Lỗi 1: Anti-Self (Tài xế tự đặt chuyến của mình) -> 409 Conflict
{
  "status": 409,
  "message": "Không thể gửi yêu cầu.",
  "errors": { "businessRule": "Bạn không thể tự đặt chuyến xe do chính mình tạo ra." }
}

// Lỗi 2: Active Request (Đang có chuyến chạy hoặc chờ duyệt khác) -> 409 Conflict
{
  "status": 409,
  "message": "Không thể gửi yêu cầu.",
  "errors": { "businessRule": "Bạn đang có một yêu cầu chờ duyệt hoặc chuyến đi đang diễn ra." }
}

// Lỗi 3: Anti-Spam (Đã từng bị tài xế này từ chối trước đó) -> 403 Forbidden
{
  "status": 403,
  "message": "Không thể gửi yêu cầu.",
  "errors": { "businessRule": "Tài xế của chuyến xe này đã từ chối yêu cầu của bạn trước đó." }
}

// Lỗi 4: Availability (Chuyến xe đã đầy hoặc tài xế đã xuất phát) -> 409 Conflict
{
  "status": 409,
  "message": "Không thể gửi yêu cầu.",
  "errors": { "businessRule": "Chuyến xe này đã hết ghế trống hoặc không còn ở trạng thái chờ khách." }
}

```