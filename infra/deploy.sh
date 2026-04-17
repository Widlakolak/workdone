#!/bin/sh

export DOCKER_CONFIG=$HOME/.docker

LOG_FILE="/share/CACHEDEV1_DATA/Container/deploy.log"
COMPOSE_DIR="/share/CACHEDEV1_DATA/Container/poetrystream"

log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

log "========================================"
log "🚀 Starting deployment of ALL services"
log "========================================"

cd "$COMPOSE_DIR" || { log "❌ Cannot cd into $COMPOSE_DIR"; exit 1; }

log "🔄 Pulling latest images..."
docker compose pull 2>&1 | tee -a "$LOG_FILE"

log "🚀 Updating changed services only..."
docker compose up -d --remove-orphans 2>&1 | tee -a "$LOG_FILE"

log "🧹 Cleaning old unused images..."
docker image prune -f 2>&1 | tee -a "$LOG_FILE"

log "🔎 Current status:"
docker compose ps | tee -a "$LOG_FILE"

log "✅ Deployment finished."