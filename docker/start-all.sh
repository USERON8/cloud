#!/bin/bash

echo "启动微服务基础设施..."

# 创建共享网络
echo "创建共享网络..."
docker network create microservice-network --driver bridge --subnet=172.20.0.0/16 2>/dev/null || echo "网络已存在"

# 启动MySQL
echo "启动MySQL服务..."
cd mysql
docker-compose up -d
echo "等待MySQL启动完成..."
sleep 30

# 启动Nacos
echo "启动Nacos服务..."
cd ../nacos
docker-compose up -d
echo "等待Nacos启动完成..."
sleep 30

echo "所有服务启动完成！"
echo "Nacos控制台: http://localhost:8848/nacos"
echo "MySQL端口: 3306"
echo "默认用户名/密码: nacos/nacos"