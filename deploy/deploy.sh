#!/usr/bin/env bash
# DMS 一键部署脚本：up / down / logs / backup
set -euo pipefail
cd "$(dirname "$0")"
COMPOSE="docker compose -f docker-compose.yml"

case "${1:-}" in
  up)
    [ -f .env ] || { cp .env.example .env; echo "已生成 .env，请先填写 DMS_JWT_SECRET"; exit 1; }
    $COMPOSE up -d --build
    echo "DMS 启动中，健康检查通过后访问 http://localhost:${DMS_HTTP_PORT:-80}"
    ;;
  down)
    $COMPOSE down
    ;;
  logs)
    $COMPOSE logs -f ${2:-}
    ;;
  backup)
    mkdir -p backup
    F="backup/dms-$(date +%Y%m%d-%H%M%S).sql.gz"
    $COMPOSE exec -T mysql mysqldump -proot dms | gzip > "$F"
    echo "备份完成: deploy/$F"
    ;;
  *)
    echo "用法: $0 {up|down|logs [service]|backup}"; exit 1;;
esac
