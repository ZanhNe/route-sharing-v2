## Công nghệ sử dụng

### 1. TẦNG FRONTEND (ReactJS - Web App)

- **Core Framework:**
    - `ReactJS 18` + `Vite`
- **Bản đồ & Định vị:**
    - `maplibre-gl`
    - `react-map-gl`
    - **Goong Map Tiles:**
- **Quản lý Trạng thái:**
    - `jotai`
- **Giao tiếp Mạng & API:**
    - `axios`
    - `@stomp/stompjs` + `sockjs-client`
- **Giao diện (UI/UX) & Camera:**
    - `tailwindcss`
    - `react-webcam`
### 2. TẦNG BACKEND
- **Core Framework:**
    - `Java 17` + `Spring Boot 3.x`.
- **Xử lý Logic Không gian:**
    - `hibernate-spatial`
    - `JTS (Java Topology Suite)`
- **Bảo mật & Quyền:**
    - `spring-boot-starter-security`
    - `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- **Thời gian thực:**
    - `spring-boot-starter-websocket`
- **Bộ nhớ đệm:**
    - `spring-boot-starter-cache` + `Caffeine Cache`
- **Tiện ích:**
    - `mapstruct`
    - `spring-boot-starter-validation`

### 3. TẦNG DATABASE
- **PostgreSQL:**
- **PostGIS:**

### 4. DỊCH VỤ BÊN THỨ 3
- **Dịch vụ Bản đồ:** **Goong API**
- **Lưu trữ File:** **Cloudinary**
- **Nhận diện khuôn mặt**