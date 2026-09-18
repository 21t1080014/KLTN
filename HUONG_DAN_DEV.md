# Hướng dẫn dev CSS (Tailwind)

Dự án dùng Tailwind CSS (v3) build ra file tĩnh, KHÔNG dùng CDN script.
File nguồn: `src/main/resources/static/css/input.css`
File output (Spring Boot phục vụ trực tiếp): `src/main/resources/static/css/output.css`

## Lần đầu sau khi clone project
```bash
npm install
```

## Khi sửa CSS / thêm class Tailwind mới trong template
Chạy song song với `mvn spring-boot:run`, để Tailwind tự rebuild mỗi khi lưu file:
```bash
npm run css:dev
```

## Build bản production (nén, dùng khi deploy)
```bash
npm run css:build
```

## Cấu trúc liên quan
- `tailwind.config.js` — khai báo design system (màu, font) trong `theme.extend`, và `content` trỏ tới
  toàn bộ `templates/**/*.html` + `static/js/**/*.js` để Tailwind quét đúng class đang dùng.
- `postcss.config.js` — cấu hình autoprefixer, do `tailwindcss init -p` sinh ra.
- `src/main/resources/static/css/input.css` — chứa 3 directive `@tailwind` + các pattern lặp lại
  được gom vào `@layer components` (`.btn-primary`, `.btn-secondary`, `.product-card`...) để tránh
  lặp chuỗi class dài ở nhiều file HTML.
- `output.css` — **có commit vào git** (khác với `node_modules/`), vì server chạy trực tiếp file
  tĩnh này, không có bước build CSS riêng khi deploy lên server thật (không có pipeline CI/CD build
  frontend). Nhớ chạy `npm run css:build` và commit lại `output.css` trước khi deploy nếu có sửa CSS.

## Bootstrap vẫn còn dùng để làm gì?
Xem `CHANGES_FRONTEND_REDESIGN.md` — tóm tắt: giữ `bootstrap.bundle.min.js` (JS thuần, không kèm
CSS) cho các hành vi modal/carousel/offcanvas/dropdown, phần giao diện dùng Tailwind toàn bộ.
