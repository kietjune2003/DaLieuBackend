## Prescription API Reference

Tài liệu này mô tả toàn bộ API nằm trong `PrescriptionController` cùng nghiệp vụ từ `PrescriptionServiceImpl`. Các endpoint ở dưới đều chạy dưới prefix `/api/prescriptions` và yêu cầu người dùng đã đăng nhập (trừ endpoint xoá dữ liệu test).

### DTO sử dụng chung

| DTO | Trường chính |
| --- | --- |
| `PrescriptionRequest` | `name`, `hospital`, `doctorName`, `consultationDate`, `followUpDate`, `drugs: DrugInPresRequest[]` |
| `DrugInPresRequest` | `drug_id` (hoặc `drugName` khi tạo thuốc lẻ), `unit_id`, `start_date`, `end_date`, `note`, `frequency_type` (`DAILY`/`INTERVAL`/`WEEKLY`), `interval_days`, `days_of_week[]`, `schedules[]` (mỗi phần tử có `time` `HH:mm`, `dosage`) |
| `UpdateScheduleStatusRequest` | `scheduleId`, `status` (0 = bỏ qua, 1 = uống; service tự set 2 nếu uống trễ >10 phút) |
| `ScheduleResponseDTO` | `scheduleId`, `drugName`, `dosage`, `time`, `status`, `edited`, `prescriptionName` |
| `ScheduleHistoryDTO` | `date`, `schedules: ScheduleResponseDTO[]` |
| `PrescriptionSummaryResponse` | `id`, `prescriptionName`, `totalDrugs`, `drugs: DrugSummaryResponse[]` (`drugName`, `nearestTime`) |

### API chi tiết

#### POST `/api/prescriptions`
- Tạo đơn thuốc mới kèm danh sách thuốc. Service kiểm tra người dùng, validate từng thuốc, lưu prescription rồi sinh schedule tự động dựa trên `frequency_type` và `schedules`.
- Body: `PrescriptionRequest`.
- Response 200: `"✅ Đã tạo đơn thuốc thành công! ID: <prescriptionId>"`.
- 400: Khi thiếu user, không có danh sách thuốc, thiếu `drug_id`/`unit_id`...

#### DELETE `/api/prescriptions/{id}`
- Xoá đơn thuốc và toàn bộ `DrugInPrescription` + `Schedule` con nhờ cascade.
- Path: `id` (Long).
- Response 200: `"🗑️ Đã xoá đơn thuốc thành công!"`.

#### GET `/api/prescriptions/status/{status}`
- Lọc đơn theo trạng thái (0 = ẩn, 1 = hiển thị). Kết quả được sắp theo `createdAt` giảm dần.
- Response 200: `PrescriptionSummaryResponse[]`.

#### PUT `/api/prescriptions/{id}`
- Ghi đè toàn bộ thông tin đơn và danh sách thuốc. Service xoá dữ liệu cũ (bao gồm schedule) rồi tạo lại bằng payload mới.
- Body: `PrescriptionRequest`.
- Response 200: Payload đã lưu (kiểu `PrescriptionRequest`).

#### GET `/api/prescriptions/{id}`
- Trả về đơn thuốc dưới dạng `PrescriptionRequest` để fill form edit. Service convert entity → DTO và gộp `Schedule` theo giờ để tạo `drugs[].schedules[]`.
- Response 200: `PrescriptionRequest`.

#### PUT `/api/prescriptions/{id}/status`
- Đảo trạng thái đơn (1↔0).
- Response 200: `"✅ Đã thay đổi trạng thái đơn thuốc ID <id> → status = <status>"`.

#### GET `/api/prescriptions/schedules`
- Lấy tất cả liều uống trong một ngày.
- Query: `date` (`yyyy-MM-dd`, bắt buộc).
- Response:
  - Nếu ngày < hôm nay: `{ "message": "Ngày bạn chọn đã ở trong quá khứ, không có liều uống nào." }`
  - Ngược lại: `ScheduleResponseDTO[]` (sắp xếp theo giờ).

#### PUT `/api/prescriptions/schedules/status`
- Đánh dấu đã/không uống một lịch cụ thể. Service kiểm tra quyền sở hữu, set `edited = true`, gán `status = 0/1/2`.
- Body: `UpdateScheduleStatusRequest`.
- Response 200: `{ "message": "Đã cập nhật: Không uống thuốc." }` hoặc `"Đã xác nhận uống thuốc."`.

#### GET `/api/prescriptions/schedules/history`
- Lịch sử các lịch đã chỉnh (`edited = true`) kèm thống kê.
- Query:
  - `filter`: `7days` hoặc `month`
  - `year`, `month`: bắt buộc khi `filter=month`
- Response 200:
  ```json
  {
    "history": [ ScheduleHistoryDTO, ... ],
    "statistics": {
      "totalTaken": number,
      "onTime": number,
      "late": number,
      "skipped": number
    }
  }
  ```

#### DELETE `/api/prescriptions/clear-all`
- Dùng trong môi trường test để xoá sạch toàn bộ `Schedule`, `DrugInPrescription`, `Prescription`.
- Response 200: `{ "message": "Đã xoá toàn bộ dữ liệu đơn thuốc (prescription, drug_in_prescriptions, schedules)." }`.

#### POST `/api/prescriptions/single-drug`
- Tạo một thuốc lẻ chỉ gắn với user (không thuộc đơn nào). Service yêu cầu `unit_id`, `start_date`, tự sinh schedule giống createPrescription.
- Body: `DrugInPresRequest` (dùng `drugName` nếu không chọn từ kho thuốc).
- Response 200: `"✅ Đã tạo  thuốc lẻ thành công! ID: <drugInPresId>"`.

#### PUT `/api/prescriptions/drugs/{id}`
- Cập nhật bản ghi `DrugInPrescription` bất kỳ (thuốc trong đơn hoặc thuốc lẻ). Service validate quyền dựa trên `drug.user`, cập nhật thông tin, xoá schedule cũ, generate mới.
- Path: `id`.
- Body: `DrugInPresRequest`.
- Response 200: `"✅ Đã cập nhật thuốc thành công! ID: <id>"`.

#### DELETE `/api/prescriptions/drugs/{id}`
- Xoá một `DrugInPrescription` và schedule liên quan (nhờ orphan removal).
- Path: `id`.
- Response 200: `"🗑️ Đã xoá thuốc thành công!".

