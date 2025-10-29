## API Endpoints

### POST `/api/auth/login`
- Mô tả: Đăng nhập bằng email và mật khẩu.
- Body (JSON):
  - `email`: string
  - `password`: string
- 200 OK: Trả về access/refresh token.
- 401: Sai thông tin đăng nhập.

Ví dụ response:
```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### POST `/api/auth/refresh`
- Mô tả: Cấp mới access token từ refresh token.
- Body (JSON):
  - `refreshToken`: string
- 200 OK: Trả về cặp token mới.
- 401: Refresh token không hợp lệ/hết hạn.

Ví dụ response: như `/login`.

### POST `/api/auth/register/start`
- Mô tả: Bắt đầu quy trình đăng ký bằng OTP email.
- Body (JSON):
  - `email`: string
  - `fullName`: string
  - `password`: string
  - `countryCode` (tuỳ hệ thống): string
- 204 No Content: Gửi OTP tới email.
- 400: Dữ liệu không hợp lệ hoặc email đã tồn tại.

### POST `/api/auth/register/verify`
- Mô tả: Xác minh OTP và hoàn tất đăng ký.
- Body (JSON):
  - `email`: string
  - `otp`: string
- 200 OK: Trả về token sau khi đăng ký thành công.
- 400/401: OTP không hợp lệ/hết hạn.

Ví dụ response: như `/login`.

### POST `/api/auth/forgot-password`
- Mô tả: Yêu cầu OTP đặt lại mật khẩu gửi tới email.
- Body (JSON):
  - `email`: string
- 204 No Content: Đã gửi OTP.

### POST `/api/auth/reset-password`
- Mô tả: Đặt lại mật khẩu bằng OTP.
- Body (JSON):
  - `email`: string
  - `otp`: string
  - `newPassword`: string
- 204 No Content: Đặt lại mật khẩu thành công.
- 400/401: OTP không hợp lệ/hết hạn.

### POST `/api/auth/google`
- Mô tả: Đăng nhập/đăng ký bằng Google ID Token.
- Body (JSON):
  - `idToken`: string (Google ID token phía client nhận được)
- 200 OK: Trả về token.
- 401: ID token không hợp lệ.

Ví dụ response: như `/login`.

### POST `/api/auth/register`
- Mô tả: (Tuỳ chọn) Đăng ký 1 bước cổ điển.
- Body (JSON):
  - `email`: string
  - `fullName`: string
  - `password`: string
  - (các trường khác nếu có)
- 201 Created: Trả về token.
- 400: Dữ liệu không hợp lệ/Email đã tồn tại.

