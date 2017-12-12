-- No of rows in result: 1
select sum(f.capacity) as capacity
	from FLIGHTS f, MONTHS m
	where f.month_id = m.mid and m.month = 'July' and 
	f.year = 2015 and f.day_of_month = 10 and
	((f.origin_city = 'Seattle WA' and
	f.dest_city = 'San Francisco CA') or
	(f.dest_city = 'Seattle WA' and
	f.origin_city = 'San Francisco CA'));