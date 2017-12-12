-- run time on S0 DB: 54 sec
-- result row count: 4 rows
select distinct origin_city as city from FLIGHTS
where origin_city not in 
(select distinct F2.dest_city as city from FLIGHTS F1, FLIGHTS F2
where F1.dest_city = F2.origin_city and F1.origin_city = 'Seattle WA' )
and origin_city not in
(select distinct dest_city from FLIGHTS where origin_city = 'Seattle WA')

-- Partial output:
--Devils Lake ND
--Hattiesburg/Laurel MS
--St. Augustine FL
--Victoria TX