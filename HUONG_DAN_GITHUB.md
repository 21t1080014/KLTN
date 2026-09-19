# Hướng dẫn tải dự án từ GitHub và cập nhật lên GitHub

Repo của dự án: **https://github.com/21t1080014/KLTN** (remote tên `origin`).

---

## 0. Trạng thái Git hiện tại (kiểm tra ngày 19/09/2026)

| Mục | Kết quả |
|---|---|
| Thư mục gốc repo | `D:\DuAnCN\CNV\KLTN` |
| Nhánh đang làm việc | `feature/admin-commerce-overhaul` |
| File chưa commit | **Không có** (working tree sạch) |
| Người commit | Du Ngoc Hung `<dunghung158@gmail.com>` |
| Nhánh `origin/master` (trên GitHub) | dừng ở commit `bb9dc23 new 1` |
| Nhánh `master` (trong máy) | hơn GitHub **1 commit** (`65f0348` – sửa bảo mật + thiết kế lại Tailwind), **chưa đẩy** |
| Nhánh `feature/admin-commerce-overhaul` | hơn `origin/master` **11 commit** (gồm cả commit thêm file hướng dẫn này), **chưa có trên GitHub** |
| File nhạy cảm bị commit nhầm | Không thấy (đã quét mật khẩu/.env/.sql/.pem). `db-backups/`, `node_modules/`, `target/` đã nằm trong `.gitignore` |

**Kết luận:** code trong máy đã được commit đầy đủ nhưng **GitHub vẫn chưa nhận được gì mới** — cần làm bước ở mục 3 để đẩy lên.

11 commit trên nhánh `feature/admin-commerce-overhaul` (mới → cũ):

```
(commit)  Thêm HUONG_DAN_GITHUB.md
cd0c5ee Brand create: friendly error when logo missing; static assets revalidate
9b103e8 Fix admin/storefront collapse menus (Tailwind .collapse)
9b4a708 Fix admin sidebar colors
9a4ada3 Module 6: dashboard theo vai trò, báo cáo doanh thu
d7a17b2 Module 5: phân quyền, nhật ký thao tác
6eee56a Module 4: quản lý khách hàng
45bf802 Module 3: luồng đơn hàng
aec353d Module 2: biến thể + tồn kho
61adbdc Module 1: danh mục sản phẩm
65f0348 Sửa bảo mật + thiết kế lại Tailwind (cũng là commit hơn của master)
```

Xem lại bất cứ lúc nào bằng: `git status -sb`, `git log --oneline -15`, `git branch -a`, `git remote -v`.

---

## 1. Tải dự án từ GitHub về máy khác (clone)

### 1.1 Cần cài trước
| Phần mềm | Ghi chú |
|---|---|
| Git | https://git-scm.com |
| JDK 17 trở lên | dự án dùng `java.version=17` |
| MySQL 8/9 | tạo database tên `shopdongho` |
| Node.js + npm | chỉ để build Tailwind CSS |

### 1.2 Lệnh tải
```bash
git clone https://github.com/21t1080014/KLTN.git
cd KLTN
```
Mặc định sẽ ở nhánh `master`. Muốn lấy đúng bản mới đã làm (sau khi bạn đã push nhánh ở mục 3):
```bash
git checkout feature/admin-commerce-overhaul
```
(Sau khi nhánh đã được merge vào `master` thì chỉ cần `git clone` là có bản mới nhất.)

### 1.3 Chuẩn bị để chạy
1. **Database** — GitHub **không chứa dữ liệu DB**. Có 2 cách:
   - Máy mới còn trống: tạo DB rỗng, app tự tạo bảng (`ddl-auto=update`):
     ```sql
     CREATE DATABASE shopdongho CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
     ```
     ⚠ Phải đúng collation `utf8mb4_0900_ai_ci` (xem `docs/changes/02-product-variant-inventory.md`), nếu không sẽ lỗi "Illegal mix of collations".
   - Muốn có sẵn dữ liệu cũ: trên máy cũ xuất DB rồi nhập vào máy mới:
     ```bash
     mysqldump -u root -p --routines shopdongho > shopdongho.sql        # máy cũ
     mysql -u root -p shopdongho < shopdongho.sql                        # máy mới (DB đã tạo sẵn)
     ```
     (Không đưa file `.sql` lên GitHub vì có dữ liệu thật; chép bằng USB/Drive.)
2. **Ảnh sản phẩm/thương hiệu** nằm ngoài repo (mặc định `D:/uploadImgshop`). Copy thư mục này sang máy mới nếu cần, hoặc đặt đường dẫn khác bằng biến môi trường `UPLOAD_DIR`.
3. **Biến môi trường** (không ghi mật khẩu vào code):
   | Biến | Ý nghĩa | Mặc định |
   |---|---|---|
   | `DB_USERNAME` | user MySQL | `root` |
   | `DB_PASSWORD` | mật khẩu MySQL | `root` |
   | `UPLOAD_DIR` | thư mục lưu ảnh | `D:/uploadImgshop` |
   | `MAIL_USERNAME` / `MAIL_PASSWORD` | gửi mail xác thực đăng ký | (mật khẩu để trống) |

   PowerShell ví dụ: `$env:DB_PASSWORD = "mat-khau-cua-ban"`
4. **Tailwind**: `npm install` (lần đầu). File `output.css` đã có sẵn trong repo; chỉ cần `npm run css:build` khi bạn sửa giao diện/thêm class mới (xem `HUONG_DAN_DEV.md`).

### 1.4 Chạy và kiểm tra
```powershell
.\mvnw.cmd spring-boot:run        # chạy app tại http://localhost:8080
.\mvnw.cmd clean test             # chạy toàn bộ test (cần DB đang chạy)
```
- Trang khách: `http://localhost:8080/`
- Trang quản trị: `http://localhost:8080/admin/login`
- Tài khoản quản trị nằm trong **database** (không nằm trong Git). Máy mới nhập DB cũ thì dùng lại tài khoản cũ; DB rỗng thì cần tạo user admin mới.

### 1.5 Cập nhật máy đã clone khi có code mới
```bash
git pull                          # kéo code mới về (đang ở nhánh nào thì cập nhật nhánh đó)
npm install                       # chỉ khi package.json thay đổi
```
Sau đó khởi động lại app. Nếu giao diện vẫn cũ hãy nhấn Ctrl+Shift+R.

---

## 2. Quy trình hằng ngày để lưu thay đổi (commit)

```bash
git status                        # xem file nào đã đổi
git add .                         # đưa tất cả thay đổi vào vùng chuẩn bị (hoặc git add <tên file>)
git commit -m "Mô tả ngắn việc đã làm"
```
Trước khi commit nên:
1. `.\mvnw.cmd clean test` → phải **BUILD SUCCESS**.
2. Nếu sửa template/JS có thêm class Tailwind mới: `npm run css:build`.
3. Không thêm file có mật khẩu/dữ liệu thật (`.gitignore` đã chặn `db-backups/`, `node_modules/`, `target/`).

---

## 3. Đưa dự án lên GitHub (làm ngay cho lần này)

Đang đứng ở thư mục `D:\DuAnCN\CNV\KLTN`.

### Cách A — Khuyên dùng: đẩy nhánh riêng rồi tạo Pull Request
An toàn vì `master` trên GitHub chưa bị đổi cho tới khi bạn bấm Merge.
```bash
git push -u origin feature/admin-commerce-overhaul
```
Sau đó vào https://github.com/21t1080014/KLTN → GitHub sẽ hiện nút **"Compare & pull request"** → kiểm tra danh sách thay đổi → **Create pull request** → **Merge pull request**.

Sau khi merge, cập nhật `master` trong máy:
```bash
git checkout master
git pull
```
(`master` cục bộ đang có 1 commit chưa đẩy — commit này cũng nằm trong nhánh feature nên sẽ được đưa lên qua Pull Request; `git pull` sẽ tự hòa nhập, nếu Git hỏi về merge thì chọn `git pull --rebase`.)

### Cách B — Merge trong máy rồi đẩy thẳng `master`
```bash
git checkout master
git merge feature/admin-commerce-overhaul     # master đi thẳng tới commit mới nhất (fast-forward)
git push origin master
```
Nhanh hơn nhưng không có bước xem lại trên GitHub.

### Đăng nhập GitHub khi push
GitHub **không cho dùng mật khẩu tài khoản** để push. Lần đầu Git sẽ mở cửa sổ đăng nhập trình duyệt (Git Credential Manager) — cứ đăng nhập là được. Nếu bị hỏi mật khẩu trong terminal, hãy tạo **Personal Access Token**:
GitHub → Settings → Developer settings → Personal access tokens → Generate (quyền `repo`) → dán token vào ô "Password".

### Kiểm tra sau khi đẩy
```bash
git status -sb                     # không còn "ahead"
git log origin/master --oneline -3
```
Rồi mở trang repo trên GitHub, chọn đúng nhánh, xem file `docs/changes/` và `HUONG_DAN_GITHUB.md` đã có.

---

## 4. Lỗi thường gặp

| Thông báo | Cách xử lý |
|---|---|
| `rejected ... non-fast-forward` / `fetch first` | Trên GitHub có commit mà máy bạn chưa có: `git pull --rebase` rồi `git push`. |
| `failed to push some refs` sau khi sửa trực tiếp trên web GitHub | Như trên: `git pull --rebase` trước. |
| `Authentication failed` | Đăng nhập lại qua trình duyệt hoặc dùng Personal Access Token (mục 3). |
| `warning: LF will be replaced by CRLF` | Chỉ là cảnh báo xuống dòng trên Windows, bỏ qua. |
| Xung đột khi merge/pull (`CONFLICT`) | Mở file bị báo, giữ đoạn đúng (xóa các dòng `<<<<<<<`, `=======`, `>>>>>>>`), rồi `git add <file>` và `git rebase --continue` (hoặc `git commit` nếu đang merge). |
| Lỡ `git add` nhầm file | `git restore --staged <file>` |
| Bỏ thay đổi chưa commit của 1 file | `git restore <file>` (⚠ mất thay đổi đó) |
| Muốn hoàn tác 1 commit đã push | `git revert <mã commit>` rồi push (không xóa lịch sử) |
| Lỡ đưa mật khẩu/secret lên GitHub | Đổi mật khẩu đó **ngay lập tức** (xóa khỏi commit sau là chưa đủ vì lịch sử vẫn còn). |

---

## 5. Lệnh Git hay dùng

```bash
git status -sb          # tình trạng nhanh
git log --oneline -15   # lịch sử gọn
git diff                # xem thay đổi chưa commit
git branch -a           # các nhánh (kể cả trên GitHub)
git checkout <nhánh>    # chuyển nhánh
git checkout -b <tên>   # tạo nhánh mới từ nhánh hiện tại
git fetch               # tải thông tin mới từ GitHub, chưa đổi code
git pull                # tải và cập nhật code
git push                # đẩy commit lên GitHub
```

Gợi ý làm việc: mỗi tính năng/sửa lỗi lớn tạo 1 nhánh riêng (`git checkout -b fix/ten-loi`), làm xong đẩy lên rồi tạo Pull Request vào `master`.

---

## 6. Tài liệu liên quan trong repo
- `HUONG_DAN_DEV.md` — build Tailwind CSS.
- `docs/changes/00-audit.md` … `06-reports-dashboard.md` — chi tiết từng module đã làm; `99-tong-ket.md` — tổng hợp thay đổi DB, việc còn thiếu.
- `CHANGES_FRONTEND_REDESIGN.md`, `FRONTEND_INVENTORY.md` — thiết kế lại giao diện.
