# 课程级 RAG 向量库本地运行说明

项目现在支持两种课程知识库向量存储：

- `local-json`：本地轻量开发兜底模式，向量保存在 `uploads/vector-store/course-knowledge-vectors.json`。
- `redis`：正式方案，使用 Redis Stack / RediSearch 的 `HNSW VECTOR` 索引。

## 本地开发默认模式

不做额外配置时，`application-dev.yml` 默认：

```yaml
kechang:
  rag:
    vector-store: ${RAG_VECTOR_STORE:local-json}
```

这样普通本地前后端可以直接运行，不依赖 Redis Stack。

## 本地启用 Redis Vector

Redis Vector 不是普通 Redis 的能力，必须使用 Redis Stack 或带 RediSearch 模块的 Redis。

启动后，把环境变量改成：

```powershell
$env:RAG_VECTOR_STORE="redis"
```

或者直接在 `application-dev.yml` 中改成：

```yaml
kechang:
  rag:
    vector-store: redis
```

然后重启后端，再进入教师端智能知识库，选择课程并点击“重建知识库”。

## 注意

- 如果本机还是普通 Redis，配置成 `redis` 后重建知识库会提示 Redis Vector / RediSearch 命令不可用。
- 已经用 `local-json` 建过的向量不会自动迁移到 Redis Vector，需要重新点击“重建知识库”。
- MySQL 仍然保存课程知识文档和 chunk 元信息，Redis 只保存用于相似度检索的 embedding 向量。
- Redis Vector 索引维度由第一次写入的 embedding 决定；如果切换 embedding 模型或维度，建议清空 Redis 向量数据后重新重建课程知识库。
