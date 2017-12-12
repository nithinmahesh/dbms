-- Part 1
CREATE TABLE frumbleData (
name varchar(256),
discount int,
month varchar(10),
price int
)

-- Part 2
-- name -> price
SELECT * FROM frumbleData A, frumbleData B
WHERE A.name = B.name and A.price != B.price

-- month -> discount
SELECT * FROM frumbleData A, frumbleData B
WHERE A.month = B.month and A.discount != B.discount

-- name -> price and month -> discount implies
-- name, month -> price, discount

-- name, discount -> price, month
SELECT * FROM frumbleData A, frumbleData B
WHERE A.name = B.name and A.discount = B.discount
and A.price != B.price and A.month != B.month

-- month, price -> discount, name
SELECT * FROM frumbleData A, frumbleData B
WHERE A.month = B.month and A.price = B.price
and A.discount != B.discount and A.name != B.name

-- FDs:
-- name -> price
-- month -> discount
-- name, month -> price, discount
-- name, discount -> price, month
-- month, price -> discount, name

-- Part 3

--S(name,price,discount,month)
--{name}+ = {name,price} 
--{name}+ != {name}
--{name}+ != {name,price,discount,month}

--So S(name,price,discount,month) can be decomposed as S1(name,price) and S2(name,discount,month)

--For S2(name,discount,month)
--{month}+ = {month, discount}
--{month}+ != {month}
--{month}+ != {name,discount,month}

--So S2(name,discount,month) can be decomposed as S21(month,discount) and S22 (month, name)

--So overall BCNF is S1(name,price), S21(month,discount) and S22 (month, name)

CREATE TABLE namePriceMapping (
name varchar(256) PRIMARY KEY,
price int
)

CREATE TABLE monthDiscountMapping (
discount int,
month varchar(10)  PRIMARY KEY
)

CREATE TABLE monthNameMapping (
name varchar(256),
month varchar(10),
FOREIGN KEY(name) REFERENCES namePriceMapping(name),
FOREIGN KEY(month) REFERENCES monthDiscountMapping(month)
)

-- Part 4
INSERT INTO namePriceMapping  
SELECT DISTINCT name, Price FROM frumbleData
-- 36 rows
SELECT COUNT(*)  FROM namePriceMapping

INSERT INTO monthDiscountMapping 
SELECT DISTINCT discount, month FROM frumbleData
-- 12 rows
SELECT COUNT(*)  FROM monthDiscountMapping

INSERT INTO monthNameMapping 
SELECT DISTINCT name, month FROM frumbleData
-- 426 rows
SELECT COUNT(*)  FROM monthNameMapping

-- 426 rows (lossless decomposition by BCNF)
SELECT A.name, A.price, B.month, B.discount
FROM namePriceMapping A, monthDiscountMapping B, monthNameMapping C
WHERE A.name = C.name and B.month = C.month