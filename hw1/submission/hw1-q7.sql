-- Set env settings
.mode column
.headers on
.nullvalue NULL

-- Write a SQL query that returns all restaurants that are within and including 10 mins from your house.
select * from MyRestaurants where distance <= 10;


