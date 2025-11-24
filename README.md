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

### Prescriptions

#### POST `/api/prescriptions`
- Mô tả: Tạo đơn thuốc mới kèm lịch uống tự động sinh.
- Quyền: Người dùng đã đăng nhập.
- Body (JSON `PrescriptionRequest`):
  - `name`: string (bắt buộc) – tên đơn thuốc.
  - `hospital`, `doctorName`: string (tuỳ chọn).
  - `consultationDate`, `followUpDate`: string, `yyyy-MM-dd`.
  - `drugs`: array (ít nhất 1 phần tử), mỗi phần tử gồm:
    - `drug_id`, `unit_id`: number (bắt buộc).
    - `start_date`, `end_date`: string (`yyyy-MM-dd`, có thể bỏ `end_date` → mặc định +7 ngày).
    - `note`: string.
    - `frequency_type`: `DAILY` | `INTERVAL` | `WEEKLY`.
    - `interval_days`: number (khi `frequency_type=INTERVAL`).
    - `days_of_week`: string[] (ví dụ: `["MONDAY","FRIDAY"]` cho `WEEKLY`).
    - `schedules`: array (tuỳ chọn) các khung giờ:
      - `time`: string `HH:mm`.
      - `dosage`: number (mặc định 1.0 nếu bỏ trống).
- 200 OK: chuỗi thông báo chứa `ID` đơn thuốc.
- 400: Thiếu thuốc, thiếu thông tin thuốc hoặc người dùng không hợp lệ.

#### DELETE `/api/prescriptions/{id}`
- Mô tả: Xoá đơn thuốc và toàn bộ thuốc/lịch con.
- Quyền: Người dùng sở hữu đơn.
- Path params: `id` (number, bắt buộc).
- 200 OK: Thông báo xoá thành công.
- 404/403: Không tìm thấy hoặc không có quyền.

#### GET `/api/prescriptions/status/{status}`
- Mô tả: Danh sách đơn theo trạng thái (0 = ẩn, 1 = hiển thị).
- Quyền: Người dùng sở hữu.
- Path params: `status`: number.
- 200 OK: `PrescriptionSummaryResponse[]` gồm:
  - `id`, `prescriptionName`, `totalDrugs`,
  - `drugs`: `[{ "drugName": string, "nearestTime": ISO datetime|null }]`.

#### PUT `/api/prescriptions/{id}`
- Mô tả: Cập nhật thông tin, danh sách thuốc và lịch (xoá toàn bộ cũ rồi tạo lại).
- Quyền: Người dùng sở hữu.
- Body: Giống `PrescriptionRequest`.
- 200 OK: Trả về nội dung đơn thuốc mới (`PrescriptionRequest`).

#### GET `/api/prescriptions/{id}`
- Mô tả: Lấy chi tiết đơn thuốc để hiển thị lại form chỉnh sửa.
- Quyền: Người dùng sở hữu.
- 200 OK: `PrescriptionRequest` (bao gồm `drugs[].schedules[]` theo giờ).

#### PUT `/api/prescriptions/{id}/status`
- Mô tả: Chuyển trạng thái đơn (1↔0).
- Quyền: Người dùng sở hữu.
- 200 OK: Chuỗi thông báo kèm trạng thái mới.

#### GET `/api/prescriptions/schedules`
- Mô tả: Lấy tất cả liều uống trong ngày.
- Quyền: Người dùng sở hữu.
- Query params: `date` (string `yyyy-MM-dd`, bắt buộc).
- 200 OK:
  - Nếu ngày quá khứ: `{ "message": "..." }`.
  - Nếu hợp lệ: `ScheduleResponseDTO[]` gồm `scheduleId`, `drugName`, `dosage`, `time`, `status` (0=chưa uống, 1=đúng giờ, 2=uống trễ), `edited`, `prescriptionName`.

#### PUT `/api/prescriptions/schedules/status`
- Mô tả: Ghi nhận đã uống/chưa uống 1 lịch.
- Quyền: Người dùng sở hữu.
- Body (`UpdateScheduleStatusRequest`):
  - `scheduleId`: number (bắt buộc).
  - `status`: number (`0` = bỏ qua, `1` = xác nhận uống; hệ thống tự đổi 1→2 nếu trễ >10 phút).
- 200 OK: `{ "message": string }`.

#### GET `/api/prescriptions/schedules/history`
- Mô tả: Lịch sử uống thuốc đã xác nhận (`edited = true`) kèm thống kê.
- Quyền: Người dùng sở hữu.
- Query params (tuỳ chọn):
  - `filter`: `7days` | `month` (bắt buộc kèm `year`, `month`) | bỏ trống = toàn bộ.
  - `year`, `month`: number (khi `filter=month`).
- 200 OK: Object:
  - `history`: `ScheduleHistoryDTO[]` (`date`, `schedules[]` theo cấu trúc `ScheduleResponseDTO`).
  - `statistics`: `{ "totalTaken", "onTime", "late", "skipped" }`.


