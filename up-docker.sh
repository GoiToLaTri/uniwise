#!/bin/bash

SCRIPT_DIR="$HOME/projects/uniwise"

cd "$SCRIPT_DIR" || { echo "Loi: Khong tim thay thu muc"; exit 1; }

echo "Dang chay tai: $(pwd)"

# Kiem tra docker
if ! command -v docker &> /dev/null; then
	echo "Loi: Docker chua duoc cai dat"
	exit 1
fi

if docker compose version &> /dev/null; then
	COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
	COMPOSE_CMD="docker-compose"
else
	echo "Loi: Khong tim thay docker compose hoac docker-compose"
	exit 1
fi

SERVICE_NAME="uniwise"

#Chay
if [ ! -n "$SERVICE_NAME" ]; then
	$COMPOSE_CMD up -d
else
	$COMPOSE_CMD -p $SERVICE_NAME up -d
fi

# Kiem tra trang thai
echo ""
echo "::: Trang thai container :::"
$COMPOSE_CMD ps
