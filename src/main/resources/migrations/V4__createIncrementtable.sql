DROP TABLE IF EXISTS `nextpart`;
CREATE TABLE `nextpart`  (
    `sequence_name` VARCHAR(50) NOT NULL,
    `NewPart` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`sequence_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = cp1251 COLLATE = cp1251_general_ci ROW_FORMAT = Dynamic;

INSERT INTO `nextpart` (`sequence_name`, `NewPart`) VALUES ('parts_sequence', 33480);
