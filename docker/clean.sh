#!/bin/bash

echo "清理Docker资源..."

# 停止所有服务
echo "停止所有服务..."
cd nacos && docker-compose down 2>/dev/null
cd ../mysql && docker-compose down 2>/dev/null
cd ..

# 清理未使用的镜像和容器
echo "清理未使用的Docker资源..."
docker system prune -f

# 清理未使用的卷（可选，会删除数据）
read -p "是否清理数据卷？这会删除所有数据 (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker volume prune -f
    echo "数据卷已清理"
else
    echo "保留数据卷"
fi

echo "清理完成！"