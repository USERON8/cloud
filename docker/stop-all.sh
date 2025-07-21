#!/bin/bash

echo "停止微服务基础设施..."

# 停止Nacos
echo "停止Nacos服务..."
cd nacos
docker-compose down

# 停止MySQL
echo "停止MySQL服务..."
cd ../mysql
docker-compose down

echo "所有服务已停止！"