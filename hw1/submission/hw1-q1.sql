-- Set env settings
.mode column
.headers on
.nullvalue NULL

-- Create a table Edges(Source, Destination) where both Source and Destination are integers.
create table Edges(Source int, Destination int);

-- Insert the tuples `(10,5)`, `(6,25)`, `(1,3)`, and `(4,4)`
insert into Edges values(10,5),(6,25),(1,3),(4,4);

-- Write a SQL statement that returns all tuples.
select * from Edges;

-- Write a SQL statement that returns only column Source for all tuples.
select Source from Edges;

-- Write a SQL statement that returns all tuples where Source > Destination.
select Source from Edges where Source > Destination;

-- Now insert the tuple `('-1','2000')`. Do you get an error? Why? This is a tricky question, you might want to [check the documentation](http://www.sqlite.org/datatype3.html).
insert into Edges values('-1','2000');
-- We do not get an error on running this. This is because SQLite3 does nto use static, rigid typing. SQLite uses a more general dynamic type system. 
-- In SQLite, the datatype of a value is associated with the value itself, not with its container. 
-- The type affinity of a column specified at create time is the recommended type for data stored in that column. 
-- The important idea here is that the type is recommended, not required. Any column can still store any type of data. 
-- This is the reason why we can insert any data type into a column of a different type as long as it is not an Integer Primary Key



