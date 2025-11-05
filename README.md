## API Endpoints

### POST `/api/auth/login`
- Mô tả: Đăng nhập bằng email và mật khẩu.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `password`: string (bắt buộc)
- 200 OK: Trả về token.
- 401: Sai thông tin đăng nhập.

### POST `/api/auth/refresh`
- Mô tả: Cấp mới access token từ refresh token.
- Body (JSON):
  - `refreshToken`: string (bắt buộc)
- 200 OK: Trả về token mới.
- 401: Refresh token không hợp lệ/hết hạn.

### POST `/api/auth/register/start`
- Mô tả: Bắt đầu quy trình đăng ký bằng OTP email.
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

### POST `/api/auth/register/verify`
- Mô tả: Xác minh OTP và hoàn tất đăng ký.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `code`: string (OTP, bắt buộc)
- 200 OK: Trả về token sau khi đăng ký thành công.
- 400/401: OTP không hợp lệ/hết hạn.

### POST `/api/auth/forgot-password`
- Mô tả: Yêu cầu OTP đặt lại mật khẩu gửi tới email.
- Body (JSON):
  - `email`: string (bắt buộc)
- 204 No Content: Đã gửi OTP.

### POST `/api/auth/reset-password`
- Mô tả: Đặt lại mật khẩu bằng OTP.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `code`: string (OTP, bắt buộc)
  - `newPassword`: string, tối thiểu 8 ký tự (bắt buộc)
- 204 No Content: Đặt lại mật khẩu thành công.
- 400/401: OTP không hợp lệ/hết hạn.

### POST `/api/auth/google`
- Mô tả: Đăng nhập/đăng ký bằng Google ID Token.
- Body (JSON):
  - `idToken`: string (bắt buộc)
- 200 OK: Trả về token.
- 401: ID token không hợp lệ.

### POST `/api/auth/register`
- Mô tả: (Tuỳ chọn) Đăng ký 1 bước cổ điển.
- Body (JSON):
  - `email`: string (bắt buộc)
  - `password`: string, tối thiểu 8 ký tự (bắt buộc)
  - `name`: string (bắt buộc)
  - `gender`: string (tuỳ chọn)
  - `phoneNumber`: string (tuỳ chọn)
  - `dateOfBirth`: string, định dạng `yyyy-MM-dd` (tuỳ chọn)
  - `countryId`: number (tuỳ chọn)
  - `photoUrl`: string (tuỳ chọn)
- 201 Created: Trả về token.
- 400: Dữ liệu không hợp lệ/Email đã tồn tại.

## Drugs

### GET `/api/drugs`
- Mô tả: Danh sách thuốc (phân trang, lọc, sắp xếp).
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Query params:
  - `q`: string (tuỳ chọn) — tìm theo name/title chứa từ khoá.
  - `minPrice`: number (tuỳ chọn).
  - `maxPrice`: number (tuỳ chọn).
  - `inStock`: boolean (tuỳ chọn).
  - `hasImage`: boolean (tuỳ chọn).
  - Phân trang: `page` (number), `size` (number), `sort` (ví dụ: `id,desc`).
- 200 OK: Trả về `Page<Drug>`.

### GET `/api/drugs/suggest`
- Mô tả: Gợi ý autocomplete tên thuốc.
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Query params:
  - `q`: string (bắt buộc).
  - `limit`: number (tuỳ chọn, mặc định 10).
- 200 OK: Trả về danh sách chuỗi tên gợi ý.

### GET `/api/drugs/all`
- Mô tả: Danh sách toàn bộ thuốc (không phân trang).
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- 200 OK: Trả về `List<Drug>`.

### GET `/api/drugs/{id}`
- Mô tả: Lấy chi tiết 1 thuốc.
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Path params:
  - `id`: number (bắt buộc).
- 200 OK: Trả về `Drug`.

### POST `/api/drugs`
- Mô tả: Tạo thuốc mới.
- Quyền: `ADMIN`.
- Body (JSON `Drug`):
  - `name`: string (bắt buộc)
  - `title`: string (tuỳ chọn)
  - `image`: string (tuỳ chọn)
  - `price`: number (bắt buộc)
  - `stockQuantity`: number (bắt buộc)
- 200 OK: Trả về `Drug` vừa tạo.

### PUT `/api/drugs/{id}`
- Mô tả: Cập nhật thuốc.
- Quyền: `ADMIN`.
- Path params:
  - `id`: number (bắt buộc).
- Body (JSON `Drug`):
  - `name`: string (bắt buộc)
  - `title`: string (tuỳ chọn)
  - `image`: string (tuỳ chọn)
  - `price`: number (bắt buộc)
  - `stockQuantity`: number (bắt buộc)
- 200 OK: Trả về `Drug` đã cập nhật.

### DELETE `/api/drugs/{id}`
- Mô tả: Xoá thuốc.
- Quyền: `ADMIN`.
- Path params:
  - `id`: number (bắt buộc).
- 204 No Content: Xoá thành công.

## Sections

### GET `/api/drugs/{drugId}/sections`
- Mô tả: Danh sách section của một thuốc (không phân trang).
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Path params:
  - `drugId`: number (bắt buộc).
- 200 OK: Trả về `List<Section>`.

### POST `/api/drugs/{drugId}/sections`
- Mô tả: Tạo section cho thuốc.
- Quyền: `ADMIN`.
- Path params:
  - `drugId`: number (bắt buộc).
- Body (JSON `Section`):
  - `title`: string (tuỳ chọn)
  - `content`: string (tuỳ chọn)
- 200 OK: Trả về `Section` vừa tạo.

### GET `/api/sections/{id}`
- Mô tả: Lấy chi tiết 1 section.
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- Path params:
  - `id`: number (bắt buộc).
- 200 OK: Trả về `Section`.

### PUT `/api/sections/{id}`
- Mô tả: Cập nhật section (title/content).
- Quyền: `ADMIN`.
- Path params:
  - `id`: number (bắt buộc).
- Body (JSON `Section`):
  - `title`: string (tuỳ chọn)
  - `content`: string (tuỳ chọn)
- 200 OK: Trả về `Section` đã cập nhật.

### DELETE `/api/sections/{id}`
- Mô tả: Xoá section.
- Quyền: `ADMIN`.
- Path params:
  - `id`: number (bắt buộc).
- 204 No Content: Xoá thành công.

### GET `/api/sections`
- Mô tả: Danh sách tất cả sections (không phân trang).
- Quyền: `USER`, `MODERATOR`, `ADMIN`.
- 200 OK: Trả về `List<Section>`.

