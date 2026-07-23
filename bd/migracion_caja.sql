-- =====================================================================
--  ERP "Mamma Tomato" (pizzeria-erp) - Migracion incremental: caja / mesas
--
--  Para bases YA creadas con una estructura anterior al ciclo de vida de
--  mesa. (En instalaciones nuevas no hace falta: bd/MamaTomato_V0.21.sql y
--  bd/data.sql ya incluyen la tabla `mesa` y su seed.)
--
--  Anade:
--   - `mesa`: mesas numeradas del salon con su estado
--     (LIBRE -> OCUPADA al vender -> POR_LIMPIAR -> LIBRE).
--   - Seed de las 4 mesas (numero 1..4, capacidad 4, LIBRE) solo si la
--     tabla esta vacia.
--
--  Idempotente: re-ejecutable sin efectos secundarios.
-- =====================================================================

USE `erp_mamatomato`;

-- Mesas del salon: solo las numeradas tienen ciclo de vida en la caja.
-- La barra y "para llevar" no se modelan aqui (no se bloquean).
CREATE TABLE IF NOT EXISTS `mesa` (
  `id_mesa` int NOT NULL AUTO_INCREMENT,
  `numero` int NOT NULL,
  `capacidad` int NOT NULL DEFAULT 4,
  `estado` enum('LIBRE','OCUPADA','POR_LIMPIAR') NOT NULL DEFAULT 'LIBRE',
  PRIMARY KEY (`id_mesa`),
  UNIQUE KEY `uk_mesa_numero` (`numero`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed de las 4 mesas solo si la tabla esta vacia (no pisa estados en uso).
INSERT INTO `mesa` (`numero`, `capacidad`, `estado`)
SELECT * FROM (
  SELECT 1 AS n, 4 AS c, 'LIBRE' AS e
  UNION ALL SELECT 2, 4, 'LIBRE'
  UNION ALL SELECT 3, 4, 'LIBRE'
  UNION ALL SELECT 4, 4, 'LIBRE'
) s
WHERE NOT EXISTS (SELECT 1 FROM `mesa`);

-- =====================================================================
--  Fin de la migracion de caja / mesas
-- =====================================================================
