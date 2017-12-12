-- run time on S0 DB: 5 sec
-- result row count: 327 rows
select origin_city, sum(case when actual_time < 180 then 1 else 0 end) * 100.0 / count(*)  as percentage from FLIGHTS
group by origin_city
order by percentage asc

-- Partial output:
--Guam TT	0.000000000000
--Pago Pago TT	0.000000000000
--Aguadilla PR	28.679245283018
--Anchorage AK	31.656277827248
--San Juan PR	33.543916853474
--Charlotte Amalie VI	39.270072992700
--Ponce PR	40.322580645161
--Fairbanks AK	49.539170506912
--Kahului HI	53.341183397115
--Honolulu HI	54.533695511576
--San Francisco CA	55.223708487084
--Los Angeles CA	55.412788344799
--Seattle WA	57.410932825673
--New York NY	60.532437322305
--Long Beach CA	61.719979024646
--Kona HI	62.952799121844
--Newark NJ	63.367565254599
--Plattsburgh NY	64.000000000000
--Las Vegas NV	64.471006179920
--Christiansted VI	64.666666666666