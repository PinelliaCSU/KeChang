# kechang/runner-sql — SQL 执行沙箱镜像（基于 SQLite）
# 构建: docker build -f docker/sandbox/runner-sql.Dockerfile -t kechang/runner-sql docker/sandbox/

FROM alpine:3.19

RUN apk add --no-cache \
    sqlite \
    coreutils

# 创建非 root 用户
RUN adduser -D -u 1000 sandbox

WORKDIR /code
