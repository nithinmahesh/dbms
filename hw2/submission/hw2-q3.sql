-- No of rows in result: 1
select w.day_of_week,  avg(f.arrival_delay) as delay
	from FLIGHTS f, WEEKDAYS w 
	where f.day_of_week_id = w.did 
	group by f.day_of_week_id
	order by avg(f.arrival_delay) desc
	LIMIT 1;