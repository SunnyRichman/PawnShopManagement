DROP DATABASE `yps`;
CREATE DATABASE IF NOT EXISTS `yps`;

USE `yps`;

DROP TABLE ticket;
CREATE TABLE ticket (
  _No CHAR(5) NOT NULL,
  issueDate DATE NOT NULL,
  firstName VARCHAR(200) NOT NULL,
  lastName VARCHAR(200) NOT NULL,
  phoneNumber CHAR(10) DEFAULT NULL,
  totalPrice INT DEFAULT NULL,
  redemptPrice INT,
  duration INT NOT NULL,
  dueDate DATE NOT NULL,
  redemptDate DATE DEFAULT NULL,
  status VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'อยู่ระหว่างจำนำ',
  new_ticket_No CHAR(5) DEFAULT NULL,
  old_ticket_No CHAR(5) DEFAULT NULL,
  PRIMARY KEY (_No)
)CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP TABLE objects;
CREATE TABLE IF NOT EXISTS objects(
	ticketNo CHAR(5) NOT NULL,
    obj_id INT NOT NULL,
    amount INT NOT NULL,
    object VARCHAR(255) NOT NULL,
	weight DECIMAL(5,2) NOT NULL,
    price INT NOT NULL,
	FOREIGN KEY (ticketNo) REFERENCES ticket(_No)
)CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
