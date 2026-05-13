#!/bin/sh
# =============================================================
#  docker-entrypoint.sh
#  Inicializa a aplicação com configurações seguras de JVM
#  e suporte a graceful shutdown via sinais SIGTERM/SIGINT.
# =============================================================
set -e

echo "==> Iniciando API de Reservas v1.0.0"
echo "==> Perfil ativo: ${SPRING_PROFILES_ACTIVE:-prod}"
echo "==> Porta: ${SERVER_PORT:-8080}"

# Configurações de JVM para container:
#
# -XX:+UseContainerSupport
#   Faz a JVM respeitar os limites de CPU/memória do container
#   (sem isso, ela lê os recursos do host e aloca demais)
#
# -XX:MaxRAMPercentage=75.0
#   Usa no máximo 75% da RAM do container para o heap.
#   Deixa 25% para o SO, threads nativas e metaspace.
#
# -XX:+UseG1GC
#   Garbage collector recomendado para aplicações web:
#   bom equilíbrio entre throughput e latência de pausa.
#
# -Djava.security.egd=file:/dev/./urandom
#   Evita bloqueio na geração de números aleatórios em containers
#   (o /dev/random pode bloquear se a entropia for baixa).
#
# -Dspring.profiles.active
#   Perfil Spring injetado via variável de ambiente.
JVM_OPTS="${JVM_OPTS} \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"

# Executa com "exec" para que o processo Java seja o PID 1.
# Isso garante que sinais SIGTERM do Docker cheguem diretamente à JVM,
# ativando o graceful shutdown do Spring Boot (fecha conexões abertas,
# finaliza transações em andamento antes de encerrar).
exec java ${JVM_OPTS} -jar /app/app.jar "$@"
