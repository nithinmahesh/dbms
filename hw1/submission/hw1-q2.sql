-- Set env settings
.mode column
.headers on
.nullvalue NULL

-- Next, you will create a table with attributes of types integer, varchar, date, and Boolean. 
create table MyRestaurants(name varchar(256), type varchar(256), distance int, dateOfLastVisit varchar(10), doILikeIt int);

select date('2011-03-28');  
select date('now');  
select date('now', '-5 year');  
select date('now', '-5 year', '+24 hour');  

-- The below query returns 'Taking classes' if today is before 2011-12-09,
-- returns if today is between 2011-12-09 and 2011-12-15 both inclusive,
-- else returns 'Vacation'
-- Summarizing, it is a query to identify what is the state of the class
select case when date('now') < date('2011-12-09') then 'Taking classes' when date('now') < date('2011-12-16') then 'Exams' else 'Vacation' end; 