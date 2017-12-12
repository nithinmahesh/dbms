-- No of rows in result: 22
select c.name as name, sum(f.departure_delay) as delay
	from FLIGHTS f, CARRIERS c, MONTHS m
	where c.cid = f.carrier_id and f.month_id = m.mid 
	group by f.carrier_id, f.month_id;