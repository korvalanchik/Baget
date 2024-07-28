SET NAMES UTF8MB4;
INSERT INTO `nextrecord` VALUES
    ('parts_sequence', 33479),
    ('vendors_sequence', 21999);

DELIMITER //

CREATE PROCEDURE increment_vendor_id()
BEGIN
    UPDATE `nextrecord`
    SET new_record = new_record + 999
    WHERE sequence_name = 'vendors_sequence';
END //

DELIMITER ;
