-- =============================================================
--  data_v001.sql — dados iniciais para o sistema de reservas
--  Compatível com H2 (dev) e PostgreSQL (prod)
--  Execute via: spring.sql.init.data-locations=classpath:data.sql
--               spring.sql.init.mode=always
-- =============================================================

-- ── SALAS ─────────────────────────────────────────────────────
INSERT INTO salas (nome, capacidade, localizacao, ativa) VALUES
  ('Sala Apollo',    12, 'Bloco A — 1º andar', true),
  ('Sala Vega',       8, 'Bloco A — 2º andar', true),
  ('Sala Orion',     20, 'Bloco B — Térreo',   true),
  ('Sala Sirius',     6, 'Bloco B — 1º andar', true),
  ('Sala Altair',    30, 'Auditório Central',  true),
  ('Sala Hélio',      4, 'Bloco C — 2º andar', false);
--  ^ inativa propositalmente para testar a regra de sala inativa

-- ── USUÁRIOS ──────────────────────────────────────────────────
INSERT INTO usuarios (nome, email, departamento) VALUES
  ('Ana Lima',       'ana.lima@empresa.com',      'Tecnologia'),
  ('Bruno Carvalho', 'bruno.carvalho@empresa.com','Produto'),
  ('Carla Mendes',   'carla.mendes@empresa.com',  'Recursos Humanos'),
  ('Diego Souza',    'diego.souza@empresa.com',   'Financeiro'),
  ('Elena Rocha',    'elena.rocha@empresa.com',   'Tecnologia'),
  ('Felipe Torres',  'felipe.torres@empresa.com', 'Jurídico');

-- ── RESERVAS ──────────────────────────────────────────────────
-- Usando datas fixas futuras para não conflitar com @Future do Bean Validation
-- Sala Apollo (id=1): Ana — 09:00-11:00 e Bruno — 14:00-16:00 (sem conflito)
-- Sala Vega   (id=2): Carla — 10:00-11:30
-- Sala Orion  (id=3): Diego — manhã inteira 08:00-12:00
-- Sala Sirius (id=4): Elena — 13:00-14:00
-- Sala Sirius (id=4): Felipe — 14:00-15:30 (início = fim anterior → permitido)
-- Uma reserva cancelada para demonstrar o status

INSERT INTO reservas (sala_id, usuario_id, inicio, fim, motivo, status, criada_em) VALUES

  -- Sala Apollo — duas reservas no mesmo dia, sem conflito
  (1, 1,
   PARSEDATETIME('2025-10-15 09:00', 'yyyy-MM-dd HH:mm'),
   PARSEDATETIME('2025-10-15 11:00', 'yyyy-MM-dd HH:mm'),
   'Planejamento de sprint Q4', 'ATIVA', CURRENT_TIMESTAMP),

  (1, 2,
   PARSEDATETIME('2025-10-15 14:00', 'yyyy-MM-dd HH:mm'),
   PARSEDATETIME('2025-10-15 16:00', 'yyyy-MM-dd HH:mm'),
   'Review de roadmap com stakeholders', 'ATIVA', CURRENT_TIMESTAMP),

  -- Sala Vega
  (2, 3,
   PARSEDATETIME('2025-10-16 10:00', 'yyyy-MM-dd HH:mm'),
   PARSEDATETIME('2025-10-16 11:30', 'yyyy-MM-dd HH:mm'),
   'Entrevista técnica — vaga backend', 'ATIVA', CURRENT_TIMESTAMP),

  -- Sala Orion — bloco longo de manhã
  (3, 4,
   PARSEDATETIME('2025-10-17 08:00', 'yyyy-MM-dd HH:mm'),
   PARSEDATETIME('2025-10-17 12:00', 'yyyy-MM-dd HH:mm'),
   'Workshop de OKRs — time de finanças', 'ATIVA', CURRENT_TIMESTAMP),

  -- Sala Sirius — duas reservas adjacentes (borda semiaberta)
  (4, 5,
   PARSEDATETIME('2025-10-18 13:00', 'yyyy-MM-dd HH:mm'),
   PARSEDATETIME('2025-10-18 14:00', 'yyyy-MM-dd HH:mm'),
   'Alinhamento jurídico contrato X', 'ATIVA', CURRENT_TIMESTAMP),

  (4, 6,
   PARSEDATETIME('2025-10-18 14:00', 'yyyy-MM-dd HH:mm'),
   PARSEDATETIME('2025-10-18 15:30', 'yyyy-MM-dd HH:mm'),
   'Revisão de cláusulas contratuais', 'ATIVA', CURRENT_TIMESTAMP),

  -- Reserva cancelada — demonstra que não entra na checagem de conflito
  (1, 3,
   PARSEDATETIME('2025-10-15 09:00', 'yyyy-MM-dd HH:mm'),
   PARSEDATETIME('2025-10-15 10:00', 'yyyy-MM-dd HH:mm'),
   'Reunião cancelada (conflito de agenda)', 'CANCELADA', CURRENT_TIMESTAMP);

-- Nota: para PostgreSQL, substitua PARSEDATETIME por CAST('...' AS TIMESTAMP)
-- Exemplo: CAST('2025-10-15 09:00:00' AS TIMESTAMP)