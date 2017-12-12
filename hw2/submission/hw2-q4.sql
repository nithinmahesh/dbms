-- No of rows in result: 11
select distinct(c.name) as name
	from FLIGHTS f, CARRIERS c
	where c.cid = f.carrier_id
	group by f.carrier_id, f.year, f.month_id, f.day_of_month
	having count(*) > 1000;