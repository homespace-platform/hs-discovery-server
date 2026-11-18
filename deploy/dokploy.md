# Discovery Server — Application riêng

Luồng CI/CD: **push master → GitHub Actions test/build → Docker Hub → Docker Hub webhook → Dokploy Application Discovery**. Workflow chỉ publish image, không gọi API deploy.

## 1. Publish image

1. Docker Hub: tạo repository `nvminh162/hs-discovery-server`.
2. GitHub repo `homespace-platform/hs-discovery-server` → Settings → Secrets and variables → Actions: cho repo dùng hai org secrets `DOCKERHUB_USERNAME` và `DOCKERHUB_TOKEN` có quyền push. Nếu repo không dùng được org secrets, thêm trực tiếp vào repository secrets.
3. Commit/push các file build lên `master`, chờ Actions **Build and publish Discovery Server** thành công. Image có tag `latest` và SHA commit; pull request chỉ build/test.

## 2. Tạo Application trên Dokploy

1. Projects → homespace → production → **Create Service → Application**, name **`hs-discovery-server`**, cùng server với infra.
2. General → Provider **Docker**, Image **`nvminh162/hs-discovery-server:latest`**, Save. Nếu image private, thêm Registry credential có quyền pull.
3. Advanced → Networks: gắn **`homespace-backend`**, network alias **`discovery`**, Save. Nếu dùng Advanced → Swarm Settings → Network, nhập Target `homespace-backend`, Aliases `discovery`. Mạng này phải được tạo trước với driver **overlay**, **attachable** để infra Compose và Applications kết nối được.
4. Giữ replica **1**. Advanced → Resources → Memory Limit: **`536870912`** bytes (512 MiB) nếu trường nhận raw bytes.
5. Không tạo domain public hoặc publish port 8761. Core/Gateway dùng **`http://discovery:8761/eureka/`** trên mạng backend.
6. Repo Discovery hiện không có `.env` và không yêu cầu secrets runtime, nên không cần tạo/dán `.env.dokploy`. Các thiết lập Eureka standalone và actuator health đã có trong `application.properties`.
7. General → **Deploy**, chờ Done, xem Logs/Containers để xác nhận healthy.

## 3. Gắn webhook tại Docker Hub

1. Dokploy Application **`hs-discovery-server`** → General: bật **Autodeploy**; Deployments: copy **Webhook URL** của riêng Application này. Dạng URL là `/api/deploy/TOKEN`, không có `/compose/`.
2. Docker Hub → **nvminh162/hs-discovery-server → Webhooks → New webhook**.
3. Webhook name: `dokploy-discovery`; Webhook URL: dán URL Application vừa copy → **Create/Add webhook**.
4. GitHub Actions → **Build and publish Discovery Server → Run workflow → master** để kiểm tra luồng. Docker Hub gửi webhook khi tag được push; Dokploy đang dùng tag `latest` sẽ triển khai đúng tag đó.
5. Kiểm tra Docker Hub webhook delivery và Dokploy deployment/health. Không thêm `DOKPLOY_DEPLOY_WEBHOOK` vào GitHub secrets, không tạo webhook GitHub song song.

Sau khi Discovery khỏe, Core và Gateway cần bật Eureka client và dùng URL trên. Trong cấu hình Core production đặt cả `SPRING_CLOUD_DISCOVERY_ENABLED=true` và `EUREKA_CLIENT_ENABLED=true`, rồi redeploy riêng Core.

Để kiểm tra từ Ubuntu, copy App Name của Application vào `DISCOVERY_SERVICE`:

```bash
DISCOVERY_SERVICE='DAN_APP_NAME_CUA_DISCOVERY'
sudo docker service ps "$DISCOVERY_SERVICE" --no-trunc
DISCOVERY_CONTAINER=$(sudo docker ps -q --filter "label=com.docker.swarm.service.name=$DISCOVERY_SERVICE")
if [ -n "$DISCOVERY_CONTAINER" ]; then
  sudo docker exec "$DISCOVERY_CONTAINER" curl --fail --silent --show-error \
    http://127.0.0.1:8761/actuator/health
fi
```

Health cần `UP`; registry sẽ có `hs-core-api` và `hs-gateway-server` sau khi hai Application đó đăng ký thành công. Không sửa service discovery hoặc tắt health check để vượt lỗi triển khai.
