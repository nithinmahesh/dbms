-- No of rows in result: 3
select distinct(f.flight_num) as flight_num 
	from FLIGHTS f, CARRIERS c, WEEKDAYS w 
	where f.day_of_week_id = w.did and c.cid = f.carrier_id and 
	f.origin_city = 'Seattle WA' and 
	f.dest_city = 'Boston MA' and 
	c.name = 'Alaska Airlines Inc.' and 
	w.day_of_week = 'Monday';