# =============================================================
# Dockerfile — API REST de Gerenciamento de Reservas
#
# Objetivo:
# Criar uma imagem Docker otimizada para produção usando
# build em múltiplos estágios (multistage build).
#
# Benefícios:
# - Imagem final menor
# - Mais segurança
# - Sem Maven no runtime
# - Melhor aproveitamento de cache
# - Build mais rápido
# =============================================================


# =============================================================
# ESTÁGIO 1 — BUILD / COMPILAÇÃO
# =============================================================

# Usa imagem oficial do Maven com Java 21 (Eclipse Temurin)
#
# Essa imagem já possui:
# - JDK 21
# - Maven instalado
#
# "AS build" cria um alias para este estágio.
# Depois poderemos copiar arquivos dele.
FROM maven:3.9.9-eclipse-temurin-21 AS build


# Define o diretório interno de trabalho do container
#
# Todos os comandos seguintes serão executados dentro de /app
WORKDIR /app


# =============================================================
# CÓPIA DO POM.XML
# =============================================================

# Copia apenas o pom.xml primeiro
#
# Isso melhora MUITO o cache do Docker.
#
# Se o pom.xml não mudar:
# - as dependências NÃO serão baixadas novamente
#
# Se copiássemos tudo logo de início,
# qualquer alteração no código invalidaria o cache.
COPY pom.xml .


# =============================================================
# DOWNLOAD DAS DEPENDÊNCIAS MAVEN
# =============================================================

# Baixa todas as dependências antecipadamente
#
# dependency:go-offline:
# - baixa dependências
# - plugins
# - dependências transitivas
#
# -B = modo batch (mais adequado para CI/CD)
RUN mvn dependency:go-offline -B


# =============================================================
# CÓPIA DO CÓDIGO-FONTE
# =============================================================

# Agora copiamos o diretório src
#
# Esta camada só será invalidada quando o código mudar
COPY src ./src


# =============================================================
# BUILD DA APLICAÇÃO
# =============================================================

# Compila e empacota a aplicação
#
# clean:
# - remove builds anteriores
#
# package:
# - gera o .jar final
#
# -DskipTests:
# - ignora testes no build da imagem
# - normalmente os testes rodam antes no CI/CD
RUN mvn clean package -DskipTests -B



# =============================================================
# ESTÁGIO 2 — RUNTIME / EXECUÇÃO
# =============================================================

# Usa apenas JRE (Java Runtime Environment)
#
# NÃO inclui compilador Java.
#
# Benefícios:
# - imagem menor
# - mais segura
# - menos consumo de memória
FROM eclipse-temurin:21-jre


# =============================================================
# METADADOS DA IMAGEM
# =============================================================

# Informações úteis para rastreabilidade
#
# Podem ser vistas com:
# docker inspect
LABEL maintainer="sua-equipe@empresa.com"
LABEL description="API REST de gerenciamento de reservas"
LABEL version="1.0.0"


# Diretório principal da aplicação
WORKDIR /app


# =============================================================
# INSTALAÇÃO DO CURL
# =============================================================

# curl será usado no HEALTHCHECK
#
# apt-get update:
# - atualiza lista de pacotes
#
# apt-get install:
# - instala o curl
#
# limpeza:
# - reduz tamanho da imagem
RUN apt-get update && apt-get install -y curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*


# =============================================================
# CRIAÇÃO DE USUÁRIO NÃO-ROOT
# =============================================================

# Boa prática de segurança
#
# Nunca execute aplicações como root em produção.
#
# groupadd:
# - cria grupo do sistema
#
# useradd:
# - cria usuário do sistema
#
# --no-create-home:
# - evita criar diretório home desnecessário
RUN groupadd --system appgroup \
    && useradd --system --gid appgroup --no-create-home appuser


# =============================================================
# CÓPIA DO JAR GERADO
# =============================================================

# Copia apenas o .jar gerado no estágio build
#
# --from=build:
# - pega arquivo do estágio chamado "build"
#
# target/*.jar:
# - pega o jar compilado pelo Maven
#
# app.jar:
# - nome final dentro da imagem
COPY --from=build /app/target/*.jar app.jar


# =============================================================
# CÓPIA DO SCRIPT DE ENTRADA
# =============================================================

# Script responsável por iniciar a aplicação
#
# Pode ser usado para:
# - logs
# - variáveis de ambiente
# - graceful shutdown
# - configurações extras
COPY docker-entrypoint.sh /app/docker-entrypoint.sh


# Dá permissão de execução ao script
RUN chmod +x /app/docker-entrypoint.sh


# =============================================================
# DIRETÓRIO DE LOGS
# =============================================================

# Cria diretório para logs
#
# chown:
# - altera dono da pasta
# - permite que appuser escreva nela
RUN mkdir -p /app/logs \
    && chown -R appuser:appgroup /app


# =============================================================
# USUÁRIO DE EXECUÇÃO
# =============================================================

# A partir daqui o container executa como appuser
#
# Mais seguro do que root
USER appuser


# =============================================================
# PORTA DA APLICAÇÃO
# =============================================================

# Porta exposta internamente
#
# Documenta qual porta a aplicação usa
EXPOSE 8080


# =============================================================
# HEALTHCHECK
# =============================================================

# Verifica se a aplicação está saudável
#
# Docker executa este comando periodicamente.
#
# interval:
# - frequência das verificações
#
# timeout:
# - tempo máximo da verificação
#
# start-period:
# - tempo de espera antes da primeira checagem
#
# retries:
# - quantidade de falhas antes de marcar unhealthy
#
# curl -f:
# - retorna erro se status HTTP for >= 400
#
# /actuator/health:
# - endpoint do Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1


# =============================================================
# ENTRYPOINT
# =============================================================

# Define o comando principal do container
#
# O script geralmente executa:
# java -jar app.jar
#
# ENTRYPOINT garante que:
# - sinais do sistema sejam tratados corretamente
# - graceful shutdown funcione
ENTRYPOINT ["/app/docker-entrypoint.sh"]