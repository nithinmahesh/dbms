-- Set env settings
.mode column
.headers on
.nullvalue NULL

-- Write a SQL query that returns all restaurants that you like, but have not visited 
-- since more than 3 months ago.
select * from MyRestaurants where doILikeIt = 1 and date('now', '-3 month') > date(dateOfLastVisit);