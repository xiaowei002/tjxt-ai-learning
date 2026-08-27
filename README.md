# 天机学堂 AI 课程学习项目

这是个人用于学习黑马程序员「天机学堂」及后续 AI 课程的代码仓库。

仓库以课程代码为基础，使用独立的 Git 历史进行维护。后续功能开发、实验代码和学习记录均提交到本仓库，不再向原课程仓库推送。

## 技术栈

- Java 17
- Spring Boot 3.3.5
- Spring Cloud 2023.0.3
- Spring Cloud Alibaba 2023.0.3.2
- Spring AI 1.0.0
- MyBatis-Plus
- Nacos、Seata、Redis、RabbitMQ、XXL-Job
- MySQL 8、Elasticsearch 7
- Nginx、Docker Compose

## 项目模块

| 模块 | 服务名 | 端口 | 说明 |
| --- | --- | ---: | --- |
| `tj-gateway` | `gateway-service` | 10010 | API 网关 |
| `tj-auth/tj-auth-service` | `auth-service` | 8081 | 认证与授权 |
| `tj-user` | `user-service` | 8082 | 用户中心 |
| `tj-search` | `search-service` | 8083 | 课程搜索 |
| `tj-media` | `media-service` | 8084 | 媒资服务 |
| `tj-message/tj-message-service` | `message-service` | 8085 | 消息与短信 |
| `tj-course` | `course-service` | 8086 | 课程管理 |
| `tj-pay/tj-pay-service` | `pay-service` | 8087 | 支付服务 |
| `tj-trade` | `trade-service` | 8088 | 交易服务 |
| `tj-exam` | `exam-service` | 8089 | 考试服务 |
| `tj-learning` | `learning-service` | 8090 | 学习中心 |
| `tj-remark` | `remark-service` | 8091 | 评价服务 |
| `tj-promotion` | `promotion-service` | 8092 | 营销服务 |
| `tj-data` | `data-service` | 8093 | 数据服务 |

`tj-common`、`tj-api` 以及各模块中的 `domain`、`api`、SDK 子模块属于公共依赖，不单独启动。

## 获取代码

```bash
git clone git@github.com:xiaowei002/tjxt-ai-learning.git
cd tjxt-ai-learning
```

推荐使用 IntelliJ IDEA 打开根目录的 `pom.xml`，项目 SDK 和 Maven Runner 均选择 Java 17。

## 本地部署包

仓库已经包含可复现运行所需的 Compose、挂载配置、Elasticsearch 插件，以及编译后的学员端和管理端前端。克隆仓库后无需再单独下载前端或复制课程部署包。

```text
tjxt-ai-learning/
└── depoly/
    └── tjxt-docker/
        ├── docker-compose.yml
        ├── .env.example
        ├── es/plugins/
        ├── nginx/conf/
        ├── redis/conf/
        ├── seata/
        ├── tj-portal/       # 学员端编译产物
        ├── tj-admin/        # 管理端编译产物
        └── xxl-job/application.properties
```

MySQL、Redis、MongoDB、Elasticsearch 等运行数据由 Docker 卷或本机目录生成，不会提交到 Git。Jenkins 数据、Gogs 仓库、日志、JDK、PEM 私钥及云服务凭据也会继续被忽略。

> GitHub 单文件限制为 100 MB。仓库保留运行所需文件，但不会上传课程虚拟机中的完整数据盘和中间件缓存。

## 启动中间件

首次启动前，复制环境变量示例并把 `SEATA_IP` 修改为当前电脑的 WLAN 或以太网 IPv4：

```powershell
Copy-Item depoly/tjxt-docker/.env.example depoly/tjxt-docker/.env
```

然后在项目根目录执行：

```bash
docker compose -f depoly/tjxt-docker/docker-compose.yml up -d \
  mysql nacos xxl-job seata mq es es2 kibana2 redis mongodb nginx
```

主要基础设施端口：

| 服务 | 端口 | 用途 |
| --- | --- | --- |
| MySQL | `3306` | 业务数据库、Nacos、Seata |
| Redis | `6379` | 缓存和会话记忆 |
| MongoDB 4.4 | `27017` | MongoDB 会话记忆实现 |
| Elasticsearch 7.12 | `9200` / `9300` | 天机课程搜索 |
| Elasticsearch 8.13 | `19200` / `19300` | Spring AI 知识库 |
| Kibana 8.13 | `15601` | ES8 知识库可视化控制台 |
| Nacos | `8848` / `9848` | 注册中心和配置中心 |
| Seata | `8099` / `7099` | 分布式事务 |
| RabbitMQ | `5672` / `15672` | 消息队列和控制台 |
| XXL-Job | `8880` | 定时任务 |
| Nginx | `80` | 前端和域名反向代理 |

旧版 `es` 和知识库 `es2` 用途不同，需要同时保留。Gogs、Jenkins 仅用于课程 CI/CD，已在 Compose 中注释，需要时可自行取消注释并准备对应数据目录。

ES2 和 Kibana2 使用独立的 `es2-net` 网络；Kibana 在容器内通过 `http://es2:9200` 连接 ES8。ES2 使用 `tjxt-es2-data`、`tjxt-es2-plugins` 命名卷，不会覆盖课程搜索使用的 ES7 数据。可通过 [http://localhost:15601](http://localhost:15601) 访问知识库控制台。

查看状态：

```bash
docker compose -f depoly/tjxt-docker/docker-compose.yml ps
```

停止中间件：

```bash
docker compose -f depoly/tjxt-docker/docker-compose.yml stop
```

> Windows + Docker Desktop 环境中，Java 服务访问容器映射端口通常使用 `127.0.0.1`；Nginx 容器访问宿主机网关使用 `host.docker.internal`；Seata 向本机 Java 服务公布的地址应使用宿主机可达的 LAN IPv4。课程虚拟机中的 `192.168.150.101` 不能直接照搬。

## 环境变量

云服务和支付凭据不得写入代码。启动相关服务前，请在本机配置以下环境变量：

| 环境变量 | 用途 |
| --- | --- |
| `TJ_SMS_ALI_ACCESS_ID` | 阿里云短信 AccessKey ID |
| `TJ_SMS_ALI_ACCESS_SECRET` | 阿里云短信 AccessKey Secret |
| `TJ_TENCENT_VOD_URL_KEY` | 腾讯云 VOD URL Key |
| `TJ_PAY_NOTIFY_HOST` | 支付公网回调地址 |
| `TJ_PAY_ALI_APP_ID` | 支付宝应用 ID |
| `TJ_PAY_ALI_MERCHANT_PRIVATE_KEY` | 支付宝商户私钥 |
| `TJ_PAY_ALI_PUBLIC_KEY` | 支付宝公钥 |
| `TJ_PAY_WX_APP_ID` | 微信应用 ID |
| `TJ_PAY_WX_MCH_ID` | 微信商户号 |
| `TJ_PAY_WX_MCH_SERIAL_NO` | 微信商户证书序列号 |
| `TJ_PAY_WX_PRIVATE_KEY` | 微信商户私钥 |
| `TJ_PAY_WX_API_V3_KEY` | 微信 API v3 Key |

Windows 用户级变量示例：

```powershell
[Environment]::SetEnvironmentVariable(
  'TJ_SMS_ALI_ACCESS_ID',
  '<your-access-key-id>',
  'User'
)
```

修改用户级环境变量后，需要完全退出并重新打开 IntelliJ IDEA，IDE 启动的 Java 进程才能读取新值。

## 本地域名

Nginx 使用以下域名提供前端及中间件入口：

| 地址 | 用途 |
| --- | --- |
| [http://www.tianji.com](http://www.tianji.com) | 学员端 |
| [http://manage.tianji.com](http://manage.tianji.com) | 管理端 |
| [http://api.tianji.com](http://api.tianji.com) | API 网关 |
| [http://nacos.tianji.com](http://nacos.tianji.com) | Nacos 控制台 |
| [http://mq.tianji.com](http://mq.tianji.com) | RabbitMQ 控制台 |
| [http://xxljob.tianji.com](http://xxljob.tianji.com) | XXL-Job 控制台 |
| [http://git.tianji.com](http://git.tianji.com) | Gogs（需在 Compose 中取消注释并准备数据目录） |
| [http://jenkins.tianji.com](http://jenkins.tianji.com) | Jenkins（需单独启动） |
| [http://es.tianji.com](http://es.tianji.com) | Kibana（需单独部署） |

Windows hosts 需要包含：

```text
127.0.0.1 www.tianji.com manage.tianji.com api.tianji.com
127.0.0.1 nacos.tianji.com mq.tianji.com xxljob.tianji.com
127.0.0.1 git.tianji.com jenkins.tianji.com es.tianji.com
```

如果使用 Clash 或其他系统代理，还需要将 `*.tianji.com` 加入代理绕过列表。

`cpolar.tianji.com` 仅在支付回调、内网穿透等课程阶段使用，不是前端运行的必要条件。

## 启动后端

1. 确认 Docker 中间件均已正常运行。
2. 使用 Java 17 启动所需业务服务。
3. 建议最后启动 `gateway-service`。
4. 访问学员端或管理端进行联调。

完整构建：

```bash
mvn -DskipTests package
```

若 Course 或 Exam 提示无法连接 Seata，请在 Nacos 中检查 `seata-server` 实例，不能注册成 Docker 内部地址（例如 `172.x.x.x`），必须是宿主机可达地址及映射端口 `8099`。

## Git 开发流程

日常开发：

```bash
git pull --ff-only
git add <files>
git commit -m "feat: describe the change"
git push
```

提交前请确认：

- `git status` 中没有数据库、日志、构建产物或本地密钥。
- 新增配置使用环境变量，不硬编码真实凭据。
- 使用 Java 17 完成必要的构建或测试。

## 说明

本仓库仅用于个人课程学习和技术实践。课程相关版权归原作者及课程提供方所有。
