-- run time on S0 DB: 19 sec
-- result row count: 329 rows
select F1.origin_city as origin_city, F1.dest_city as dest_city , (F1.time) as time
	from (select origin_city, dest_city, max(actual_time) as time 
		from FLIGHTS
		group by origin_city, dest_city) F1, 
		(select origin_city, max(actual_time) as time 
		from FLIGHTS
		group by origin_city) F2
	where F1.origin_city = F2.origin_city and F1.time = F2.time
	order by F1.origin_city asc, F1.dest_city asc

-- Partial output:
--Aberdeen SD	Minneapolis MN	106
--Abilene TX	Dallas/Fort Worth TX	111
--Adak Island AK	Anchorage AK	471.37
--Aguadilla PR	New York NY	368.76
--Akron OH	Atlanta GA	408.69
--Albany GA	Atlanta GA	243.45
--Albany NY	Atlanta GA	390.31
--Albuquerque NM	Houston TX	492.81
--Alexandria LA	Atlanta GA	391.05
--Allentown/Bethlehem/Easton PA	Atlanta GA	456.95
--Alpena MI	Detroit MI	80
--Amarillo TX	Houston TX	390.73
--Anchorage AK	Barrow AK	490.01
--Appleton WI	Atlanta GA	405.07
--Arcata/Eureka CA	San Francisco CA	476.89
--Asheville NC	Chicago IL	279.81
--Ashland WV	Cincinnati OH	84
--Aspen CO	Los Angeles CA	304.59
--Atlanta GA	Honolulu HI	649
--Atlantic City NJ	Fort Lauderdale FL	212