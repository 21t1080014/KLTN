# TIKTAKUS — Website bán đồng hồ

Website bán đồng hồ gồm **trang cửa hàng cho khách** và **khu quản trị cho nhân viên** (`/admin`). Dự án khóa luận (KLTN) của **Du Ngoc Hung**.

- Khách: xem/tìm/lọc sản phẩm, giỏ hàng, đặt hàng (COD/chuyển khoản), voucher, đăng ký/đăng nhập, xem và tự hủy đơn của mình.
- Nhân viên: quản lý sản phẩm (nhiều biến thể), danh mục/thương hiệu, tồn kho, nhập hàng, đơn hàng, khách hàng, khuyến mãi/voucher, báo cáo doanh thu, nhật ký thao tác. Mỗi vai trò (admin, nhân viên sản phẩm, CSKH, kho) chỉ thấy phần việc của mình.

## Công nghệ sử dụng

| Thành phần          | Công nghệ                                                                                                        |
| ------------------- | ---------------------------------------------------------------------------------------------------------------- |
| Backend             | Java 17, Spring Boot 3.4, Spring Data JPA (Hibernate), Maven                                                     |
| Cơ sở dữ liệu       | MySQL                                                                                                            |
| Giao diện (view)    | Thymeleaf, JavaScript/jQuery gọi API JSON                                                                        |
| Định dạng giao diện | Tailwind CSS 3 (build ra `output.css`); Bootstrap chỉ dùng phần JavaScript (modal, dropdown…) và Bootstrap Icons |
| Đăng nhập / bảo mật | Session, mật khẩu băm bằng jBCrypt, phân quyền theo vai trò                                                      |
| Kiểm thử            | JUnit 5, Spring Boot Test, MockMvc                                                                               |

## Chạy dự án (tóm tắt)

```powershell
# Cần: JDK 17, MySQL (database `shopdongho`, collation utf8mb4_0900_ai_ci), Node.js
npm install
.\mvnw.cmd spring-boot:run        # http://localhost:8080  |  quản trị: /admin/login
.\mvnw.cmd clean test             # chạy test
```

Mật khẩu DB đặt bằng biến môi trường `DB_USERNAME`, `DB_PASSWORD` (không ghi trong code). Chi tiết: `HUONG_DAN_GITHUB.md`.

## Ai đã sửa giao diện của hệ thống

> Ghi chú cho chủ sở hữu dự án để sau này đọc lại biết giao diện hiện tại từ đâu mà có.

- **Giao diện gốc** (Bootstrap) có sẵn từ các commit đầu của dự án (`b834425`, `bb9dc23`).
- **Toàn bộ giao diện hiện tại** (các trang cửa hàng và các trang quản trị có sẵn lúc đó) được **thiết kế lại bằng Tailwind CSS theo phong cách tối giản – sang trọng** bởi **Claude Code (trợ lý AI của Anthropic)**, theo yêu cầu của **Du Ngoc Hung**, ngày **19/09/2026**, commit `65f0348`. Nhật ký chi tiết từng trang: `CHANGES_FRONTEND_REDESIGN.md`.
- Hệ thống lại cấu trúc theo yêu cầu chủ sở hữu muốn không sửa code ban đầu chỉ sửa lại giao diện cũ của hệ thông theo phong cách hiện đại hơn.

**Muốn sửa giao diện sau này thì sửa ở đâu:**

| Việc                                  | File                                                                            |
| ------------------------------------- | ------------------------------------------------------------------------------- |
| Màu, font toàn site                   | `tailwind.config.js`                                                            |
| Kiểu nút, thẻ, bảng, modal dùng chung | `src/main/resources/static/css/input.css`                                       |
| Một trang cụ thể                      | `src/main/resources/templates/<thư mục>/<trang>.html` và `static/js/<trang>.js` |
| Menu/khung trang quản trị             | `templates/layout/admin.html`                                                   |
| Menu/khung trang cửa hàng             | `templates/layout/storefront.html`                                              |

Lưu ý: sau khi sửa giao diện phải chạy `npm run css:build` rồi tải lại trang (Ctrl+Shift+R); không sửa tay `output.css`. Đừng đổi `id`/tên tham số mà JavaScript và API đang dùng.

## Tài liệu khác

- `docs/changes/` — nhật ký từng module đã làm; `99-tong-ket.md` tổng hợp thay đổi CSDL và **việc còn thiếu**.
- `HUONG_DAN_DEV.md` (build Tailwind), `HUONG_DAN_GITHUB.md` (tải/đẩy dự án lên GitHub).
