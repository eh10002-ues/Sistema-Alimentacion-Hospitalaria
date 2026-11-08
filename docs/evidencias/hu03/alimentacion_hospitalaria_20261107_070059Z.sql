-- HU-03: respaldo de una sola base MySQL 8.4
-- MySQL dump 10.13  Distrib 8.4.9, for Linux (aarch64)
--
-- Host: 127.0.0.1    Database: hu03_origen
-- ------------------------------------------------------
-- Server version	8.4.9

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `administracion_usuarios_lock`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `administracion_usuarios_lock` (
  `id` tinyint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_administracion_usuarios_lock` CHECK ((`id` = 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `administracion_usuarios_lock`
--

LOCK TABLES `administracion_usuarios_lock` WRITE;
/*!40000 ALTER TABLE `administracion_usuarios_lock` DISABLE KEYS */;
INSERT INTO `administracion_usuarios_lock` VALUES (1);
/*!40000 ALTER TABLE `administracion_usuarios_lock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asignaciones_menu`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asignaciones_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paciente_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  `asignado_por_id` bigint NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date DEFAULT NULL,
  `activa` tinyint(1) NOT NULL,
  `aprobado_por_id` bigint DEFAULT NULL,
  `fecha_aprobacion` date DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_asig_menu_asignador` (`asignado_por_id`),
  KEY `fk_asig_menu_aprobador` (`aprobado_por_id`),
  KEY `idx_asig_menu_menu` (`menu_id`,`activa`),
  KEY `idx_asig_menu_paciente` (`paciente_id`,`activa`),
  CONSTRAINT `fk_asig_menu_aprobador` FOREIGN KEY (`aprobado_por_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_asig_menu_asignador` FOREIGN KEY (`asignado_por_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_asig_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `menus` (`id`),
  CONSTRAINT `fk_asig_menu_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `pacientes` (`id`),
  CONSTRAINT `ck_asig_menu_activa` CHECK ((`activa` in (0,1))),
  CONSTRAINT `ck_asig_menu_aprobacion` CHECK ((((`aprobado_por_id` is null) and (`fecha_aprobacion` is null)) or ((`aprobado_por_id` is not null) and (`fecha_aprobacion` is not null)))),
  CONSTRAINT `ck_asig_menu_fechas` CHECK (((`fecha_fin` is null) or (`fecha_fin` >= `fecha_inicio`)))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asignaciones_menu`
--

LOCK TABLES `asignaciones_menu` WRITE;
/*!40000 ALTER TABLE `asignaciones_menu` DISABLE KEYS */;
INSERT INTO `asignaciones_menu` VALUES (1,1,1,2,'2026-11-07','2026-12-07',1,2,'2026-11-07','2026-11-07 07:01:17.945056','hu03-prueba','2026-11-07 07:01:17.945056','hu03-prueba');
/*!40000 ALTER TABLE `asignaciones_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `asignaciones_piso`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asignaciones_piso` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `piso_id` bigint NOT NULL,
  `usuario_id` bigint NOT NULL,
  `turno` varchar(20) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asig_piso_turno` (`piso_id`,`turno`),
  KEY `fk_asig_piso_usuario` (`usuario_id`),
  CONSTRAINT `fk_asig_piso_piso` FOREIGN KEY (`piso_id`) REFERENCES `pisos` (`id`),
  CONSTRAINT `fk_asig_piso_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_asig_piso_turno` CHECK ((`turno` in (_utf8mb4'MATUTINO',_utf8mb4'VESPERTINO',_utf8mb4'NOCTURNO')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `asignaciones_piso`
--

LOCK TABLES `asignaciones_piso` WRITE;
/*!40000 ALTER TABLE `asignaciones_piso` DISABLE KEYS */;
/*!40000 ALTER TABLE `asignaciones_piso` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bitacora`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bitacora` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accion` varchar(50) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `tabla_afectada` varchar(60) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `id_afectado` bigint DEFAULT NULL,
  `fecha_accion` datetime(6) NOT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_bitacora_usuario` (`usuario_id`),
  KEY `idx_bitacora_registro` (`tabla_afectada`,`id_afectado`),
  KEY `idx_bitacora_fecha` (`fecha_accion`),
  CONSTRAINT `fk_bitacora_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bitacora`
--

LOCK TABLES `bitacora` WRITE;
/*!40000 ALTER TABLE `bitacora` DISABLE KEYS */;
INSERT INTO `bitacora` VALUES (1,'APROBAR','prescripciones_dieta',1,'2026-11-07 07:01:17.946867',2);
/*!40000 ALTER TABLE `bitacora` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `camas`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `camas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `piso_id` bigint NOT NULL,
  `numero` varchar(10) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_camas_piso_numero` (`piso_id`,`numero`),
  CONSTRAINT `fk_camas_piso` FOREIGN KEY (`piso_id`) REFERENCES `pisos` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `camas`
--

LOCK TABLES `camas` WRITE;
/*!40000 ALTER TABLE `camas` DISABLE KEYS */;
INSERT INTO `camas` VALUES (1,1,'HU03-01');
/*!40000 ALTER TABLE `camas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','HU01 crear usuarios','SQL','V1__HU01_crear_usuarios.sql',1302414,'hu03_app','2026-11-07 07:01:15',10,1),(2,'2','HU01 serializar administracion usuarios','SQL','V2__HU01_serializar_administracion_usuarios.sql',1223306502,'hu03_app','2026-11-07 07:01:15',7,1),(3,'3','agregar tablas para ubicaciones','SQL','V3__agregar_tablas_para_ubicaciones.sql',1371005512,'hu03_app','2026-11-07 07:01:15',27,1),(4,'4','agregar tablas para pacientes','SQL','V4__agregar_tablas_para_pacientes.sql',903704211,'hu03_app','2026-11-07 07:01:15',39,1),(5,'5','agregar tablas para bitacora','SQL','V5__agregar_tablas_para_bitacora.sql',1815664633,'hu03_app','2026-11-07 07:01:15',10,1),(6,'6','agregar tablas para insumos','SQL','V6__agregar_tablas_para_insumos.sql',542639765,'hu03_app','2026-11-07 07:01:15',9,1),(7,'7','agregar tablas para menus','SQL','V7__agregar_tablas_para_menus.sql',-57586571,'hu03_app','2026-11-07 07:01:15',27,1),(8,'8','agregar tablas para asignaciones','SQL','V8__agregar_tablas_para_asignaciones.sql',1190970284,'hu03_app','2026-11-07 07:01:15',14,1),(9,'9','SAH02 crear prescripciones dieta','SQL','V9__SAH02_crear_prescripciones_dieta.sql',-115231452,'hu03_app','2026-11-07 07:01:15',10,1),(10,'10','representar merma como fraccion','SQL','V10__representar_merma_como_fraccion.sql',1537451741,'hu03_app','2026-11-07 07:01:15',37,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `insumos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `insumos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `unidad` varchar(10) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `merma` decimal(5,4) DEFAULT '0.0000',
  `presentacion` varchar(50) COLLATE utf8mb4_0900_as_cs DEFAULT NULL,
  `cantidad_presentacion` decimal(10,2) DEFAULT NULL,
  `costo_unitario` decimal(10,4) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_insumos_nombre` (`nombre`),
  CONSTRAINT `ck_insumos_cantidad_presentacion` CHECK (((`cantidad_presentacion` is null) or (`cantidad_presentacion` >= 0))),
  CONSTRAINT `ck_insumos_costo` CHECK (((`costo_unitario` is null) or (`costo_unitario` >= 0))),
  CONSTRAINT `ck_insumos_merma` CHECK (((`merma` is null) or ((`merma` >= 0) and (`merma` < 1)))),
  CONSTRAINT `ck_insumos_nombre` CHECK ((char_length(`nombre`) > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `insumos`
--

LOCK TABLES `insumos` WRITE;
/*!40000 ALTER TABLE `insumos` DISABLE KEYS */;
INSERT INTO `insumos` VALUES (1,'Arroz sintético HU-03','g',0.1000,'Paquete sintético',1000.00,0.0020);
/*!40000 ALTER TABLE `insumos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menu_detalle_insumos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu_detalle_insumos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_detalle_id` bigint NOT NULL,
  `insumo_id` bigint NOT NULL,
  `cantidad_por_racion` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_det_insumo` (`menu_detalle_id`,`insumo_id`),
  KEY `fk_mdi_insumo` (`insumo_id`),
  CONSTRAINT `fk_mdi_detalle` FOREIGN KEY (`menu_detalle_id`) REFERENCES `menu_detalles` (`id`),
  CONSTRAINT `fk_mdi_insumo` FOREIGN KEY (`insumo_id`) REFERENCES `insumos` (`id`),
  CONSTRAINT `ck_mdi_cantidad` CHECK ((`cantidad_por_racion` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu_detalle_insumos`
--

LOCK TABLES `menu_detalle_insumos` WRITE;
/*!40000 ALTER TABLE `menu_detalle_insumos` DISABLE KEYS */;
INSERT INTO `menu_detalle_insumos` VALUES (1,1,1,80.00);
/*!40000 ALTER TABLE `menu_detalle_insumos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menu_detalles`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menu_detalles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `menu_id` bigint NOT NULL,
  `tiempo_comida` varchar(30) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `hora_servicio` time NOT NULL,
  `platillo` varchar(150) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_detalle` (`menu_id`,`tiempo_comida`),
  CONSTRAINT `fk_menu_det_menu` FOREIGN KEY (`menu_id`) REFERENCES `menus` (`id`),
  CONSTRAINT `ck_menu_det_tiempo` CHECK ((`tiempo_comida` in (_utf8mb4'DESAYUNO',_utf8mb4'COLACION_MATUTINA',_utf8mb4'ALMUERZO',_utf8mb4'COLACION_VESPERTINA',_utf8mb4'CENA')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menu_detalles`
--

LOCK TABLES `menu_detalles` WRITE;
/*!40000 ALTER TABLE `menu_detalles` DISABLE KEYS */;
INSERT INTO `menu_detalles` VALUES (1,1,'ALMUERZO','12:00:00','Platillo sintético');
/*!40000 ALTER TABLE `menu_detalles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menus`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menus` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(150) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `tipo` varchar(20) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `tipo_dieta` varchar(30) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `estado` varchar(20) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_menus_tipo_dieta` (`tipo_dieta`,`estado`),
  CONSTRAINT `ck_menus_estado` CHECK ((`estado` in (_utf8mb4'BORRADOR',_utf8mb4'ACTIVO',_utf8mb4'INACTIVO'))),
  CONSTRAINT `ck_menus_nombre` CHECK ((char_length(`nombre`) > 0)),
  CONSTRAINT `ck_menus_tipo` CHECK ((`tipo` in (_utf8mb4'GENERAL',_utf8mb4'PERSONALIZADO'))),
  CONSTRAINT `ck_menus_tipo_dieta` CHECK ((`tipo_dieta` in (_utf8mb4'NORMAL',_utf8mb4'BLANDA',_utf8mb4'LIQUIDA',_utf8mb4'HIPOSODICA',_utf8mb4'HIPOCALORICA',_utf8mb4'PAPILLA')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menus`
--

LOCK TABLES `menus` WRITE;
/*!40000 ALTER TABLE `menus` DISABLE KEYS */;
INSERT INTO `menus` VALUES (1,'Menú sintético HU-03','GENERAL','NORMAL','ACTIVO','2026-11-07 07:01:17.941271','hu03-prueba','2026-11-07 07:01:17.941271','hu03-prueba');
/*!40000 ALTER TABLE `menus` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `paciente_alergias`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `paciente_alergias` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paciente_id` bigint NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `severidad` varchar(20) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pac_ale_paciente` (`paciente_id`),
  CONSTRAINT `fk_pac_ale_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `pacientes` (`id`),
  CONSTRAINT `ck_pac_ale_severidad` CHECK ((`severidad` in (_utf8mb4'LEVE',_utf8mb4'MODERADA',_utf8mb4'SEVERA')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `paciente_alergias`
--

LOCK TABLES `paciente_alergias` WRITE;
/*!40000 ALTER TABLE `paciente_alergias` DISABLE KEYS */;
INSERT INTO `paciente_alergias` VALUES (1,1,'Alergia sintética','LEVE');
/*!40000 ALTER TABLE `paciente_alergias` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `paciente_enfermedades`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `paciente_enfermedades` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paciente_id` bigint NOT NULL,
  `nombre` varchar(150) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pac_enf_paciente` (`paciente_id`),
  CONSTRAINT `fk_pac_enf_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `pacientes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `paciente_enfermedades`
--

LOCK TABLES `paciente_enfermedades` WRITE;
/*!40000 ALTER TABLE `paciente_enfermedades` DISABLE KEYS */;
INSERT INTO `paciente_enfermedades` VALUES (1,1,'Condición sintética');
/*!40000 ALTER TABLE `paciente_enfermedades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `paciente_medicamentos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `paciente_medicamentos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paciente_id` bigint NOT NULL,
  `nombre` varchar(150) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `dosis` varchar(50) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `frecuencia_horas` smallint NOT NULL,
  `hora_primera_dosis` time NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_pac_med_paciente` (`paciente_id`),
  CONSTRAINT `fk_pac_med_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `pacientes` (`id`),
  CONSTRAINT `ck_pac_med_frecuencia` CHECK ((`frecuencia_horas` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `paciente_medicamentos`
--

LOCK TABLES `paciente_medicamentos` WRITE;
/*!40000 ALTER TABLE `paciente_medicamentos` DISABLE KEYS */;
INSERT INTO `paciente_medicamentos` VALUES (1,1,'Medicamento sintético','Dosis de prueba',8,'06:00:00');
/*!40000 ALTER TABLE `paciente_medicamentos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pacientes`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pacientes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(20) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `nombres` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `apellidos` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `fecha_nacimiento` date NOT NULL,
  `peso_kg` decimal(5,2) NOT NULL,
  `altura_cm` decimal(5,2) NOT NULL,
  `estado` varchar(20) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `cama_id` bigint DEFAULT NULL,
  `medico_responsable_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pacientes_codigo` (`codigo`),
  UNIQUE KEY `uk_pacientes_cama` (`cama_id`),
  KEY `fk_pacientes_medico` (`medico_responsable_id`),
  CONSTRAINT `fk_pacientes_cama` FOREIGN KEY (`cama_id`) REFERENCES `camas` (`id`),
  CONSTRAINT `fk_pacientes_medico` FOREIGN KEY (`medico_responsable_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_pacientes_codigo` CHECK ((char_length(`codigo`) > 0)),
  CONSTRAINT `ck_pacientes_estado` CHECK ((`estado` in (_utf8mb4'INGRESADO',_utf8mb4'ALTA'))),
  CONSTRAINT `ck_pacientes_peso` CHECK ((`peso_kg` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pacientes`
--

LOCK TABLES `pacientes` WRITE;
/*!40000 ALTER TABLE `pacientes` DISABLE KEYS */;
INSERT INTO `pacientes` VALUES (1,'HU03-PAC-001','Paciente sintético','Recuperación','1990-01-01',65.00,170.00,'INGRESADO',1,2,'2026-11-07 07:01:17.934528','hu03-prueba','2026-11-07 07:01:17.934528','hu03-prueba');
/*!40000 ALTER TABLE `pacientes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pisos`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pisos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `codigo` varchar(10) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `nombre` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pisos_codigo` (`codigo`),
  CONSTRAINT `ck_pisos_codigo` CHECK ((char_length(`codigo`) > 0))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pisos`
--

LOCK TABLES `pisos` WRITE;
/*!40000 ALTER TABLE `pisos` DISABLE KEYS */;
INSERT INTO `pisos` VALUES (1,'HU03','Piso sintético HU-03');
/*!40000 ALTER TABLE `pisos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `prescripciones_dieta`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prescripciones_dieta` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paciente_id` bigint NOT NULL,
  `tipo_dieta` varchar(30) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `estado` varchar(20) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `autorizado_por_id` bigint NOT NULL,
  `fecha_autorizacion` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_presc_dieta_autorizador` (`autorizado_por_id`),
  KEY `idx_presc_dieta_paciente` (`paciente_id`,`fecha_autorizacion`),
  CONSTRAINT `fk_presc_dieta_autorizador` FOREIGN KEY (`autorizado_por_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_presc_dieta_paciente` FOREIGN KEY (`paciente_id`) REFERENCES `pacientes` (`id`),
  CONSTRAINT `ck_presc_dieta_estado` CHECK ((`estado` = _utf8mb4'APROBADO')),
  CONSTRAINT `ck_presc_dieta_tipo` CHECK ((`tipo_dieta` in (_utf8mb4'NORMAL',_utf8mb4'BLANDA',_utf8mb4'HIPOSODICA')))
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `prescripciones_dieta`
--

LOCK TABLES `prescripciones_dieta` WRITE;
/*!40000 ALTER TABLE `prescripciones_dieta` DISABLE KEYS */;
INSERT INTO `prescripciones_dieta` VALUES (1,1,'NORMAL','APROBADO',2,'2026-11-07 07:01:17.946026','2026-11-07 07:01:17.946026','hu03-prueba','2026-11-07 07:01:17.946026','hu03-prueba');
/*!40000 ALTER TABLE `prescripciones_dieta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre_usuario` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_ci NOT NULL,
  `contrasena_hash` varchar(255) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `nombre_completo` varchar(150) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `rol` varchar(40) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `activo` tinyint(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) COLLATE utf8mb4_0900_as_cs NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuarios_nombre_usuario` (`nombre_usuario`),
  CONSTRAINT `ck_usuarios_activo` CHECK ((`activo` in (0,1))),
  CONSTRAINT `ck_usuarios_nombre_usuario` CHECK (((char_length(`nombre_usuario`) > 0) and (not(regexp_like(`nombre_usuario`,_utf8mb4'[[:space:]]'))) and (cast(`nombre_usuario` as char charset binary) = cast(lower(`nombre_usuario`) as char charset binary)))),
  CONSTRAINT `ck_usuarios_rol` CHECK ((cast(`rol` as char charset binary) in (_utf8mb4'ROLE_ADMIN',_utf8mb4'ROLE_MEDICO',_utf8mb4'ROLE_NUTRICIONISTA',_utf8mb4'ROLE_ENCARGADO_PISO',_utf8mb4'ROLE_COCINA')))
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'hu03.admin','{pbkdf2@SpringSecurity_v5_8}376d158bd08d1f412426008cdb2456f809db9a7913dee0b084d36e72032d4b7f1af49d9ac2052116d42264926b49dd0d','Administrador sintético HU-03','ROLE_ADMIN',1,'2026-11-07 07:01:17.684888','system','2026-11-07 07:01:17.684888','system'),(2,'hu03.medico','{pbkdf2@SpringSecurity_v5_8}376d158bd08d1f412426008cdb2456f809db9a7913dee0b084d36e72032d4b7f1af49d9ac2052116d42264926b49dd0d','Médico sintético HU-03','ROLE_MEDICO',1,'2026-11-07 07:01:17.930442','hu03-prueba','2026-11-07 07:01:17.930442','hu03-prueba');
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-07  7:01:18
