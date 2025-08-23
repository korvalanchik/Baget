CREATE TABLE transaction_types (
                                   type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   code VARCHAR(20) NOT NULL UNIQUE, -- наприклад INVOICE, PAYMENT, REFUND
                                   description VARCHAR(100)          -- опис для користувача
);


CREATE TABLE transactions (
                              transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              order_no BIGINT NOT NULL,
                              type_id BIGINT NOT NULL,
                              transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              amount DECIMAL(10,2) NOT NULL,
                              reference VARCHAR(50), -- номер рахунку, чеку і т.д.
                              status VARCHAR(20),    -- Pending, Completed, Canceled
                              note TEXT,

                              CONSTRAINT fk_transaction_order FOREIGN KEY (order_no)
                                  REFERENCES orders(OrderNo),

                              CONSTRAINT fk_transaction_type FOREIGN KEY (type_id)
                                  REFERENCES transaction_types(type_id)
);
