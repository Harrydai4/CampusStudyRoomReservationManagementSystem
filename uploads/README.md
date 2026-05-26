# uploads 目录说明

本目录存放运行时上传的文件，**不提交 Git**。

| 子目录 | 用途 |
|---|---|
| `layout/` | 管理员上传的自习室座位分布图 |

访问 URL 示例：`http://localhost:8080/uploads/layout/xxxxxxxx.jpg`

由 `UploadController` 写入，`WebConfig` 提供静态访问。
