DROP TABLE IF EXISTS `nextpart`;
CREATE TABLE `nextpart`  (
    `NewPart` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`NewPart`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = cp1251 COLLATE = cp1251_general_ci ROW_FORMAT = Dynamic;

INSERT INTO `nextpart` (`NewPart`) VALUES (33480);