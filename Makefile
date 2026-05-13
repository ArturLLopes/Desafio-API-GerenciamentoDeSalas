# =============================================================
#  Makefile — atalhos para operações Docker comuns
#  Uso: make <comando>
# =============================================================

IMAGE_NAME  = reservas-api
IMAGE_TAG   = latest
CONTAINER   = reservas-prod

.PHONY: help build run dev prod logs stop clean ps health

# ── Ajuda ─────────────────────────────────────────────────────
help:
	@echo ""
	@echo "  Comandos disponíveis:"
	@echo ""
	@echo "  make build     Compila a imagem Docker"
	@echo "  make dev       Sobe o ambiente de desenvolvimento (H2)"
	@echo "  make prod      Sobe o ambiente de produção (PostgreSQL)"
	@echo "  make logs      Exibe logs do container em tempo real"
	@echo "  make stop      Para todos os serviços"
	@echo "  make clean     Remove containers, imagens e volumes"
	@echo "  make ps        Lista containers em execução"
	@echo "  make health    Verifica saúde da aplicação"
	@echo "  make shell     Abre shell dentro do container (debug)"
	@echo ""

# ── Build ─────────────────────────────────────────────────────
build:
	docker build --target runtime -t $(IMAGE_NAME):$(IMAGE_TAG) .
	@echo "✓ Imagem $(IMAGE_NAME):$(IMAGE_TAG) criada"

# ── Desenvolvimento (H2 em memória) ───────────────────────────
dev:
	docker compose --profile dev up --build

dev-detached:
	docker compose --profile dev up --build -d
	@echo "✓ Ambiente dev iniciado em background"
	@echo "  API:        http://localhost:8080"
	@echo "  H2 Console: http://localhost:8080/h2-console"

# ── Produção (PostgreSQL) ─────────────────────────────────────
prod:
	@test -f .env || (echo "ERRO: arquivo .env não encontrado. Copie .env.example" && exit 1)
	docker compose --profile prod up -d
	@echo "✓ Ambiente prod iniciado"

# ── Logs ──────────────────────────────────────────────────────
logs:
	docker compose logs -f --tail=100

logs-app:
	docker compose logs -f --tail=100 app-prod

logs-db:
	docker compose logs -f --tail=50 postgres

# ── Controle ──────────────────────────────────────────────────
stop:
	docker compose --profile dev --profile prod down
	@echo "✓ Todos os serviços parados"

ps:
	docker compose ps

# ── Saúde ─────────────────────────────────────────────────────
health:
	@curl -sf http://localhost:8080/actuator/health | python3 -m json.tool \
	  || echo "ERRO: aplicação não está respondendo"

# ── Debug ─────────────────────────────────────────────────────
shell:
	docker exec -it $(CONTAINER) /bin/sh

# ── Limpeza ───────────────────────────────────────────────────
clean:
	docker compose --profile dev --profile prod down -v --remove-orphans
	docker rmi $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true
	@echo "✓ Containers, volumes e imagem removidos"

clean-all: clean
	docker system prune -f
	@echo "✓ Sistema Docker limpo"
