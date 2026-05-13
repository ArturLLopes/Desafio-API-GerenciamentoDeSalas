-- =============================================================
--  init-db.sql — executado pelo PostgreSQL na primeira inicialização
--  do volume. Cria extensões e configurações iniciais do banco.
--  O schema das tabelas é criado pelo Hibernate (ddl-auto: create).
-- =============================================================

-- Extensão para UUIDs (útil se migrar para UUIDs como chave primária no futuro)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Timezone padrão do banco alinhado com a aplicação
SET timezone = 'America/Sao_Paulo';

-- Configuração de locale para ordenação correta de strings em português
-- (já definido na criação do banco via POSTGRES_DB, mas reforçado aqui)
