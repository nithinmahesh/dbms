-- No of rows in result: 6
select c.name as name, (sum(f.canceled)*100.0/count(f.canceled)) as percent
	from FLIGHTS f, CARRIERS c
	where c.cid = f.carrier_id and 
	f.origin_city = 'Seattle WA'
	group by f.carrier_id
	having (sum(f.canceled)*100.0/count(f.canceled)) > 0.5
	order by (sum(f.canceled)*100.0/count(f.canceled)) asc;