-- Set env settings
.mode column
.headers on
.nullvalue NULL

-- Write a SQL query that returns only the name and distance of all restaurants within and 
-- including 20 minutes of your house. The query should list the restaurants in alphabetical order of names.
select name, distance from MyRestaurants where distance <= 20 order by name asc;