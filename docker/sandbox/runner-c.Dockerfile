# kechang/runner-c — C 语言编译执行沙箱镜像
# 构建: docker build -f docker/sandbox/runner-c.Dockerfile -t kechang/runner-c docker/sandbox/

FROM alpine:3.19

RUN apk add --no-cache \
    gcc \
    musl-dev \
    libc-dev \
    coreutils

# 创建非 root 用户用于执行代码
RUN adduser -D -u 1000 sandbox

# 工作目录
WORKDIR /code

# 编译脚本: gcc source.c -o program 2>compile_error.txt
# 执行脚本: timeout {N}s ./program < input.txt
