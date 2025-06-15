ALTER TABLE `orders`
    ADD COLUMN `TotalCost` float NULL DEFAULT NULL AFTER 'Income';
