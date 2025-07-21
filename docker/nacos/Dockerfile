# syntax=docker/dockerfile:1

# 使用指定的Nacos镜像
FROM docker.xuanyuan.me/nacos/nacos-server:v3.0.2

# 设置环境变量 - 数据库配置
ENV MODE=standalone
ENV SPRING_DATASOURCE_PLATFORM=mysql
ENV MYSQL_SERVICE_HOST=mysql
ENV MYSQL_SERVICE_PORT=3306
ENV MYSQL_SERVICE_DB_NAME=nacos
ENV MYSQL_SERVICE_USER=nacos
ENV MYSQL_SERVICE_PASSWORD=nacos123

# 设置环境变量 - 鉴权配置
ENV NACOS_AUTH_ENABLE=true
ENV NACOS_AUTH_TOKEN=VGhpc0lzTXlDdXN0b21TZWNyZXRLZXkwMTIzNDU2Nzg
ENV NACOS_AUTH_IDENTITY_KEY=ServerIdentity_Key
ENV NACOS_AUTH_IDENTITY_VALUE=ServerIdentity_Value_123
ENV NACOS_AUTH_CACHE_ENABLE=false

# 暴露端口
EXPOSE 8848 9848 9849

# 启动命令
CMD ["sh", "-c", "cd /home/nacos && sh startup.sh"]