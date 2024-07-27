DROP TABLE IF EXISTS `nextrecord`;
CREATE TABLE `nextrecord`  (
    `sequence_name` VARCHAR(50) NOT NULL,
    `new_record` bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (`sequence_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = cp1251 COLLATE = cp1251_general_ci ROW_FORMAT = Dynamic;

INSERT INTO `nextrecord` (`sequence_name`, `new_record`) VALUES ('parts_sequence', 33480);
INSERT INTO `nextrecord` (`sequence_name`, `new_record`) VALUES ('vendors_sequence', 22000);
