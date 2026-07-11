# kechang/runner-python — Python 执行沙箱镜像
# 构建: docker build -f docker/sandbox/runner-python.Dockerfile -t kechang/runner-python docker/sandbox/

FROM python:3.12-alpine

# 创建非 root 用户用于执行代码
RUN adduser -D -u 1000 sandbox

# 限制危险模块
RUN echo 'import os; os.system = lambda x: (_ for _ in ()).throw(Exception("os.system is disabled"))' > /usr/local/lib/python3.12/site-packages/sandbox_restrictions.pth

WORKDIR /code
