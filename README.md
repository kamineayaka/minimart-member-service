# minimart-member-service

MiniMart 的会员进程（User / Address / 发牌）。领域用语与 v1 契约在编排仓 [`minimart-infra`](https://github.com/kamineayaka/minimart-infra)。

- Spring 名：`member-service`
- 端口：8081
- 库：`minimart_member`（infra `docker/mysql/init.sql` 建库；本进程 Flyway 建表）
- 不发起 Feign 调用，不访问 product / order / payment

Gateway 把 `/member/**` 去掉前缀后转到本进程。下面的路径是**服务自身**的路径。

| 方法 | 路径 | 认证 | 作用 |
|------|------|------|------|
| POST | `/users` | 无 | 注册。`loginName` 去空白并转小写，密码 BCrypt |
| POST | `/tokens` | 无 | 签发 HS256 JWT。`sub` 为 User id，`iss` 为 `member-service` |
| GET | `/me` | Bearer | 当前 User |
| GET/POST | `/addresses` | Bearer | 列出 / 新增自己的 Address |
| GET/PUT/DELETE | `/addresses/{addressId}` | Bearer | 读写自己的 Address；别人的 id 返回 `ADDRESS_NOT_FOUND` |
| GET | `/internal/v1/addresses/{addressId}?userId=` | 无（仅集群内） | 给 order-service 抄**当前**地址。改地址后这里变；已下的 Order 由 order-service 自己保存副本 |

错误体使用 `ApiError`。登录失败一律 `AUTHENTICATION_FAILED`（不区分没有这个 User 还是密码错）。未带或非法令牌是 `UNAUTHENTICATED`。

JWT 密钥：`JWT_SECRET`（至少 32 字节，默认只适合本机）。有效期 `JWT_TTL`（默认 `PT12H`）。Gateway 校验令牌还不在本仓。

本机运行：

```bash
./gradlew bootRun
```

测试使用 Testcontainers 启动 MySQL 8.4，需要本机 Docker：

```bash
./gradlew test
```

不做：Cart、目录、Order、Payment、刷新令牌、登出、改密码。
