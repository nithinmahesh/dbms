-- Set env settings
.mode column
.headers on
.nullvalue NULL

-- Write a SQL query that returns all restaurants in your table. 
select * from MyRestaurants;

-- Experiment with a few of SQLite's output formats and show the command you use to format the output along with your query:
-- print the results in comma-separated form
.mode csv
select * from MyRestaurants;

-- print the results in list form, delimited by "` | `"
.separator , |
select * from MyRestaurants;

-- print the results in column form, and make each column have width 15
.separator , \n
.mode column
.width 15
select * from MyRestaurants;

-- for each of the formats above, try printing/not printing the column headers with the results
-- headers on is done above. next we will repeat with headers off
.headers off
-- print the results in comma-separated form
.mode csv
select * from MyRestaurants;

-- print the results in list form, delimited by "` | `"
.separator , |
select * from MyRestaurants;

-- print the results in column form, and make each column have width 15
.separator , \n
.mode column
.width 15
select * from MyRestaurants;