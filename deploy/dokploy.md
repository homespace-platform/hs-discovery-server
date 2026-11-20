# Discovery Server — Compose riêng, network trong YAML

Thử nghiệm chuyển Discovery và Gateway sang Compose; giữ nguyên Compose infra đang chạy và dữ liệu của nó. Mỗi repo có `compose.production.yml` riêng. Infra tạo `hs-network`; YAML Discovery/Gateway tham chiếu external network; Docker DNS dùng tên service, không cần chỉnh Advanced → Networks.

Tên service và DNS nội bộ là `hs-discovery-server`, đồng bộ tên repo/Spring application. Các client dùng `EUREKA_CLIENT_SERVICE_URL=http://hs-discovery-server:8761/eureka/`. Nếu từng deploy Compose với service key `discovery`, Stop bản cũ trước khi chuyển, chọn Service Name `hs-discovery-server` trong domain dashboard rồi Deploy. Cập nhật Environment và redeploy các client đang chạy sau khi DNS mới sẵn sàng; file env local không tự cập nhật Dokploy. Không giữ alias cũ hoặc chạy song song hai Eureka standalone để thay thế bước chuyển đổi.

## 1. Image và network

- Environment bắt buộc có `DISCOVERY_IMAGE=nvminh162/hs-discovery-server:latest` hoặc tag SHA đã publish; Compose dùng `${DISCOVERY_IMAGE:?}`, thiếu hoặc rỗng là lỗi, không có fallback.
- Workflow `publish-image.yml` giữ nguyên: GitHub Actions test/build/push image, không gọi Dokploy.
- Deploy infra trước. Trên EC2 kiểm tra:

```bash
sudo docker network inspect hs-network --format 'driver={{.Driver}} attachable={{.Attachable}}'
```

Cần `driver=overlay attachable=true`. Không xóa/tạo lại infra, network hay volume để thử Discovery.

## 2. Tạo Compose mới trên Dokploy

1. Projects → homespace → production → **Create Service → Docker Compose**, tên `discovery-compose`, cùng server với infra. Để Application Discovery cũ nguyên trạng cho đến bước chuyển đổi; không chạy cả hai cùng tên DNS `hs-discovery-server`.
2. General → Provider **Git**, nhập:

   | Trường | Giá trị |
   | --- | --- |
   | Repository URL | `https://github.com/homespace-platform/hs-discovery-server.git` |
   | Branch | `master` |
   | Compose Path | `./compose.production.yml` |
   | Compose Type | Docker Compose, không phải Stack |
   | Autodeploy | Tắt trong lần chuyển đổi đầu |

3. Save. Repo public HTTPS không cần SSH key; private cần quyền đọc. Phải đưa file Compose lên Git trước, Dokploy không đọc file local chưa push.
4. **Environment**: dán `.env.dokploy` của Discovery với `DISCOVERY_IMAGE=nvminh162/hs-discovery-server:latest` hoặc tag SHA đã publish → Save. Biến image bắt buộc; Discovery không cần secrets runtime. File env được Git bỏ qua.
5. **Preview Compose** phải có service `hs-discovery-server`, default network external `hs-network`, không có `ports:` public. Giữ tắt Isolated Deployment cho luồng dùng mạng chung; kiểm tra bản Preview vẫn giữ `hs-network`. Không cần thêm network bằng Advanced. YAML đặt memory limit 512 MiB; healthcheck được kế thừa từ image.
6. Mặc định không public Eureka. Nếu cần giữ dashboard `discovery.homespace.id.vn`, domain thuộc **Compose mới → Domains**, Service `hs-discovery-server`, Port `8761`, Path `/`, Strip Path tắt, HTTPS/Let's Encrypt. Chỉ bật route sau khi gỡ domain trùng ở Application cũ; bảo vệ dashboard bằng xác thực/giới hạn truy cập trước khi public. Các backend vẫn dùng URL nội bộ, không dùng domain dashboard.

## 3. Chuyển từ Application cũ

1. Chờ image build/push thành công. Ghi lại image/tag, domain và cấu hình của Application cũ; không cần đổi Dockerfile hoặc rebuild chỉ vì thêm YAML.
2. Tắt Autodeploy Application Discovery cũ và vô hiệu hóa webhook Docker Hub trỏ tới nó, tránh tự khởi chạy lại bản cũ.
3. **Stop Application Discovery cũ** trên Dokploy, chờ task/container cũ đã dừng. Nếu giữ domain dashboard, chuyển domain sang Compose mới để không có hai router cùng Host.
4. **Deploy Compose `discovery-compose`**. Kiểm tra deployment Done và container healthy. Discovery có thể trống registry ngay sau chuyển đổi; chờ client đăng ký lại, không tắt self-preservation để che lỗi.
5. Chỉ xóa thẻ Application cũ sau khi bản Compose hoạt động ổn. Không xóa Compose infra, không xóa volume. Nếu lỗi, Stop Compose mới trước rồi Start lại Application cũ và khôi phục domain cũ.

## 4. Kiểm tra

Trên EC2, chọn đúng container của Compose mới nếu còn container cũ ở trạng thái stopped:

```bash
sudo docker ps --filter label=com.docker.compose.service=hs-discovery-server --format 'table {{.ID}}\t{{.Names}}\t{{.Status}}'
DISCOVERY_CONTAINER='DAN_CONTAINER_ID_DANG_CHAY_O_TREN'
sudo docker exec "$DISCOVERY_CONTAINER" curl -fsS --max-time 5 http://127.0.0.1:8761/actuator/health
sudo docker exec "$DISCOVERY_CONTAINER" curl -fsS --max-time 5 -H 'Accept: application/json' http://127.0.0.1:8761/eureka/apps
```

Health cần `UP`. Gateway Compose dùng `http://hs-discovery-server:8761/eureka/`; sau khi Gateway healthy, registry phải có `HS-GATEWAY-SERVER`. Core chỉ xuất hiện khi đã deploy và bật Eureka. Service vẫn giữ Spring name `hs-discovery-server`; tên DNS service không đổi tên ứng dụng Spring.

## 5. Cập nhật image và webhook

**Git provider (phương án thử nghiệm):** push Git → chờ Actions publish image thành công → bấm Deploy Compose. Dokploy lấy YAML mới từ Git; network được áp dụng lại tự động; DNS dùng tên service `hs-discovery-server`. Không nối webhook Docker Hub vào Compose Git trên v0.31.4 vì payload thiếu branch và bị từ chối. Webhook push Git chạy trước khi image build xong nên không bật trong thử nghiệm này.

**Nếu ưu tiên Docker Hub trigger:** dùng provider **Raw**, dán cùng nội dung `compose.production.yml`, bật Autodeploy và gắn Docker Hub webhook tới URL **của Compose mới**, dạng `/api/deploy/compose/TOKEN`. Raw không tự lấy YAML từ Git. Workflow push SHA và latest có thể gây hai delivery/deploy trên Raw Compose v0.31.4, không có lọc tag như Docker Application. Khi pin SHA, cập nhật biến image trước khi Deploy. Giữ kín webhook token.

Workflow vẫn chỉ build/push; chưa có tự động hóa chờ CI rồi deploy Compose Git. Không dùng lại webhook `/api/deploy/TOKEN` của Application cũ cho Compose.

[Dokploy Compose](https://docs.dokploy.com/docs/core/docker-compose) · [Compose webhook v0.31.4](https://github.com/Dokploy/dokploy/blob/v0.31.4/apps/dokploy/pages/api/deploy/compose/%5BrefreshToken%5D.ts)
