----------------------------------
-- SELECT TEST TABLES
----------------------------------
DROP TABLE IF EXISTS SELECT_TABLE;
CREATE TABLE SELECT_TABLE(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', AGE INT);

-- SELECT_TABLE
INSERT INTO SELECT_TABLE VALUES(1, 'Ada', 10);
INSERT INTO SELECT_TABLE VALUES(2, 'Kate', 99);
INSERT INTO SELECT_TABLE VALUES(3, 'Brenda', 30);
INSERT INTO SELECT_TABLE VALUES(4, 'Anne', 45);
INSERT INTO SELECT_TABLE VALUES(5, 'Nick', 99);
INSERT INTO SELECT_TABLE VALUES(6, 'Perry', 60);


----------------------------------
-- INSERT TEST TABLES
----------------------------------
DROP TABLE IF EXISTS INSERT_TABLE;
CREATE TABLE INSERT_TABLE(ID INT AUTO_INCREMENT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');


----------------------------------
-- UPDATE TEST TABLES
----------------------------------
DROP TABLE IF EXISTS UPDATE_TABLE;
CREATE TABLE UPDATE_TABLE(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

-- SELECT_TABLE
INSERT INTO UPDATE_TABLE VALUES(1, 'Ada');


----------------------------------
-- DELETE TEST TABLES
----------------------------------
DROP TABLE IF EXISTS DELETE_TABLE;
CREATE TABLE DELETE_TABLE(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

-- SELECT_TABLE
INSERT INTO DELETE_TABLE VALUES(1, 'Ada');


----------------------------------
-- JOIN TEST TABLES
----------------------------------
DROP TABLE IF EXISTS JOIN_TABLE_1;
CREATE TABLE JOIN_TABLE_1(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', TABLE_2_ID INT);

DROP TABLE IF EXISTS JOIN_TABLE_2;
CREATE TABLE JOIN_TABLE_2(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

DROP TABLE IF EXISTS JOIN_TABLE_3;
CREATE TABLE JOIN_TABLE_3(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', TABLE_2_ID INT);

-- JOIN_TABLE_1
INSERT INTO JOIN_TABLE_1 VALUES(1, 'table1_name1', 1);
INSERT INTO JOIN_TABLE_1 VALUES(2, 'table1_name2', 2);
INSERT INTO JOIN_TABLE_1 VALUES(3, 'table1_name3', 3);
-- JOIN_TABLE_2
INSERT INTO JOIN_TABLE_2 VALUES(1, 'table2_name1');
INSERT INTO JOIN_TABLE_2 VALUES(2, 'table2_name2');
INSERT INTO JOIN_TABLE_2 VALUES(3, 'table2_name3');
-- JOIN_TABLE_3
INSERT INTO JOIN_TABLE_3 VALUES(1, 'table3_name1', 1);
INSERT INTO JOIN_TABLE_3 VALUES(2, 'table3_name2', 1);
INSERT INTO JOIN_TABLE_3 VALUES(3, 'table3_name3', 2);
INSERT INTO JOIN_TABLE_3 VALUES(4, 'table3_name4', 3);


----------------------------------
-- FILTER TEST TABLES
----------------------------------
DROP TABLE IF EXISTS FILTER_TABLE_1;
CREATE TABLE FILTER_TABLE_1(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', TABLE_2_ID INT);

DROP TABLE IF EXISTS FILTER_TABLE_2;
CREATE TABLE FILTER_TABLE_2(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', TABLE_3_ID INT);

DROP TABLE IF EXISTS FILTER_TABLE_3;
CREATE TABLE FILTER_TABLE_3(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

DROP TABLE IF EXISTS FILTER_TABLE_4;
CREATE TABLE FILTER_TABLE_4(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', TABLE_3_ID INT);

-- FILTER_TABLE_1
INSERT INTO FILTER_TABLE_1 VALUES(1, 'table1_name1', 1);
INSERT INTO FILTER_TABLE_1 VALUES(2, 'table1_name2', 2);
INSERT INTO FILTER_TABLE_1 VALUES(3, 'table1_name3', 3);
-- FILTER_TABLE_2
INSERT INTO FILTER_TABLE_2 VALUES(1, 'table2_name1', 1);
INSERT INTO FILTER_TABLE_2 VALUES(2, 'table2_name2', 2);
INSERT INTO FILTER_TABLE_2 VALUES(3, 'table2_name3', 3);
-- FILTER_TABLE_3
INSERT INTO FILTER_TABLE_3 VALUES(1, 'table3_name1');
INSERT INTO FILTER_TABLE_3 VALUES(2, 'table3_name2');
INSERT INTO FILTER_TABLE_3 VALUES(3, 'table3_name3');
-- FILTER_TABLE_4
INSERT INTO FILTER_TABLE_4 VALUES(1, 'table4_name1', 1);
INSERT INTO FILTER_TABLE_4 VALUES(2, 'table4_name2', 2);
INSERT INTO FILTER_TABLE_4 VALUES(3, 'table4_name3', 3);


----------------------------------
-- TRANSACTION TEST TABLES
----------------------------------
DROP TABLE IF EXISTS TRANSACTION_TABLE_1;
CREATE TABLE TRANSACTION_TABLE_1(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

DROP TABLE IF EXISTS TRANSACTION_TABLE_2;
CREATE TABLE TRANSACTION_TABLE_2(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

DROP TABLE IF EXISTS TRANSACTION_TABLE_3;
CREATE TABLE TRANSACTION_TABLE_3(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

DROP TABLE IF EXISTS TRANSACTION_TABLE_4;
CREATE TABLE TRANSACTION_TABLE_4(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');

-- TRANSACTION_TABLE_1
INSERT INTO TRANSACTION_TABLE_1 VALUES(1, 'table1_name');
-- TRANSACTION_TABLE_2
INSERT INTO TRANSACTION_TABLE_2 VALUES(1, 'table2_name');
-- TRANSACTION_TABLE_3
INSERT INTO TRANSACTION_TABLE_3 VALUES(1, 'table3_name');
-- TRANSACTION_TABLE_4
INSERT INTO TRANSACTION_TABLE_4 VALUES(1, 'table4_name');


----------------------------------
-- ALIAS TEST TABLES
----------------------------------
DROP TABLE IF EXISTS ALIAS_TABLE_A;
CREATE TABLE ALIAS_TABLE_A(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '');
DROP TABLE IF EXISTS ALIAS_TABLE_B;
CREATE TABLE ALIAS_TABLE_B(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', A_ID INT);
DROP TABLE IF EXISTS ALIAS_TABLE_C;
CREATE TABLE ALIAS_TABLE_C(ID INT PRIMARY KEY, NAME VARCHAR(255) DEFAULT '', A_ID INT);

-- SELECT_TABLE
INSERT INTO ALIAS_TABLE_A VALUES(1, 'A');
INSERT INTO ALIAS_TABLE_B VALUES(1, 'B', 1);
INSERT INTO ALIAS_TABLE_C VALUES(1, 'B', 1);


