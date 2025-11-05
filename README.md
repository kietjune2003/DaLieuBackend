## Important API Endpoints

### Auth

#### POST `/api/auth/login`
- Mô tả: Đăng nhập bằng email và mật khẩu.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `password`: string (bắt buộc)
- 200 OK: Trả về token.
- 401: Sai thông tin đăng nhập.

#### POST `/api/auth/refresh`
- Mô tả: Cấp mới access token từ refresh token.
- Body (JSON):
  - `refreshToken`: string (bắt buộc)
- 200 OK: Trả về token mới.
- 401: Refresh token không hợp lệ/hết hạn.

#### POST `/api/auth/register/start`
- Mô tả: Bắt đầu đăng ký bằng OTP email.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `password`: string, tối thiểu 8 ký tự (bắt buộc)
  - `name`: string (bắt buộc)
  - `gender`: string (tuỳ chọn)
  - `phoneNumber`: string (tuỳ chọn)
  - `dateOfBirth`: string, định dạng `yyyy-MM-dd` (tuỳ chọn)
  - `countryId`: number (tuỳ chọn)
  - `photoUrl`: string (tuỳ chọn)
- 204 No Content: Đã gửi OTP đến email.
- 400: Dữ liệu không hợp lệ hoặc email đã tồn tại.

#### POST `/api/auth/register/verify`
- Mô tả: Xác minh OTP và hoàn tất đăng ký.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `code`: string (OTP, bắt buộc)
- 200 OK: Trả về token sau khi đăng ký thành công.
- 400/401: OTP không hợp lệ/hết hạn.

#### POST `/api/auth/forgot-password`
- Mô tả: Yêu cầu OTP đặt lại mật khẩu gửi tới email.
- Body (JSON):
  - `email`: string (bắt buộc)
- 204 No Content: Đã gửi OTP.

#### POST `/api/auth/reset-password`
- Mô tả: Đặt lại mật khẩu bằng OTP.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `code`: string (OTP, bắt buộc)
  - `newPassword`: string, tối thiểu 8 ký tự (bắt buộc)
- 204 No Content: Đặt lại mật khẩu thành công.
- 400/401: OTP không hợp lệ/hết hạn.

#### GET `/api/auth/login/google`
- Mô tả: Chuyển hướng sang luồng OAuth2 Google (HTTP 302).
- Body: không có.
- 302 Found: Redirect tới `/oauth2/authorization/google`.

#### GET `/api/auth/me`
- Mô tả: Trả về trạng thái đăng nhập hiện tại và hồ sơ người dùng.
- Body: không có.
- Header (tuỳ chọn): `Authorization: Bearer <accessToken>` nếu dùng JWT.
- 200 OK: Trả về `AuthProfileDto` gồm: `authenticated` (boolean), `provider` (string), `email` (string), `name` (string), `picture` (string), `sub` (string), `authorities` (string[]).

### Drugs

#### GET `/api/drugs`
- Mô tả: Danh sách thuốc (phân trang, lọc, sắp xếp).
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Query params:
  - `q`: string (tuỳ chọn) — tìm theo name/title chứa từ khoá.
  - `minPrice`: number (tuỳ chọn)
  - `maxPrice`: number (tuỳ chọn)
  - `inStock`: boolean (tuỳ chọn)
  - `hasImage`: boolean (tuỳ chọn)
  - Phân trang: `page` (number), `size` (number), `sort` (ví dụ: `id,desc`).
- 200 OK: Trả về `Page<Drug>`.

#### GET `/api/drugs/{id}`
- Mô tả: Lấy chi tiết 1 thuốc.
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Path params:
  - `id`: number (bắt buộc)
- 200 OK: Trả về `Drug`.

#### GET `/api/drugs/suggest`
- Mô tả: Gợi ý autocomplete tên thuốc.
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Query params:
  - `q`: string (bắt buộc)
  - `limit`: number (tuỳ chọn, mặc định 10)
- 200 OK: Trả về danh sách chuỗi tên gợi ý.

### Sections

#### GET `/api/drugs/{drugId}/sections`
- Mô tả: Danh sách section của một thuốc (không phân trang).
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Path params:
  - `drugId`: number (bắt buộc)
- 200 OK: Trả về `List<Section>`.

#### GET `/api/sections/{id}`
- Mô tả: Lấy chi tiết 1 section.
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Path params:
  - `id`: number (bắt buộc)
- 200 OK: Trả về `Section`.


