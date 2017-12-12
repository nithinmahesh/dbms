-- No of rows in result: 3
select c.name as carrier, avg(f.price) as average_price
	from FLIGHTS f, MONTHS m, CARRIERS c
	where m.mid = f.month_id and c.cid = f.carrier_id and
	((f.origin_city = 'Seattle WA' and
	f.dest_city = 'New York NY') or
	(f.dest_city = 'Seattle WA' and
	f.origin_city = 'New York NY'))
	group by f.carrier_id, f.month_id;