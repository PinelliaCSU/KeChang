# kechang/runner-cpp — C++ 编译执行沙箱镜像
# 构建: docker build -f docker/sandbox/runner-cpp.Dockerfile -t kechang/runner-cpp docker/sandbox/

FROM alpine:3.19

RUN apk add --no-cache \
    g++ \
    libstdc++ \
    musl-dev \
    libc-dev \
    coreutils

# 创建非 root 用户用于执行代码
RUN adduser -D -u 1000 sandbox

WORKDIR /code
