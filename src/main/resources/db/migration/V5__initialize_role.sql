-- ----------------------------
-- Records of roles
-- the higher the access level number, the fewer the rights
-- ----------------------------
SET NAMES UTF8MB4;
START TRANSACTION;
INSERT INTO `roles` (`id`, `name`)
VALUES
    (1, 'ADMIN'),
    (2, 'LEVEL1'),  -- accountant
    (3, 'LEVEL2'),  -- privileged seller
    (4, 'LEVEL3'),  -- ordinary seller
    (5, 'LEVEL4');  -- buyer
COMMIT;