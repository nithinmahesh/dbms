-- Set env settings
.mode column
.headers on
.nullvalue NULL

-- Insert at least five tuples using the SQL INSERT command five (or more) times. 
-- You should insert at least one restaurant you liked, at least one restaurant you did not like, 
-- and at least one restaurant where you leave the “I like” field `NULL`.
insert into MyRestaurants values('BurgerKing', 'American', 2, '2017-06-01', 0);
insert into MyRestaurants values('McDonalds', 'FastFood', 4, '2017-07-01', 0);
insert into MyRestaurants values('TacoBell', 'Mexican', 6, '2017-08-01', 1);
insert into MyRestaurants values('Chipotle', 'Mexican', 8, '2017-09-01', 1);
insert into MyRestaurants values('RedRobin', 'American', 20, '2017-09-08', null);
insert into MyRestaurants values('Dennys', 'Breakfast', 22, '2017-06-08', 1);
insert into MyRestaurants values('DQ', 'American', 10, '2017-05-08', null);
