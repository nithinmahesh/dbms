-- No of rows in result: 488
select f1.flight_num as f1_flight_num, f1.origin_city as f1_origin_city, f1.dest_city as f1_dest_city, f1.actual_time as f1_actual_time,
	f2.flight_num as f2_flight_num, f2.origin_city as f2_origin_city, f2.dest_city as f2_dest_city, f2.actual_time as f2_actual_time, (f1.actual_time + f2.actual_time) as actual_time
	from FLIGHTS f1, FLIGHTS f2, MONTHS m
	where f1.month_id = m.mid and f2.month_id = m.mid and m.month = 'July' and -- Join for July Month
	f1.year = 2015 and f1.day_of_month = 15 and f2.year = 2015 and f2.day_of_month = 15 and -- Filter both flights for given specific date
	f1.origin_city = 'Seattle WA' and 
	f2.dest_city = 'Boston MA' and 
	f1.dest_city = f2.origin_city and 
	f1.carrier_id = f2.carrier_id and 
	f1.actual_time + f2.actual_time < 420;
-- First 20 rows output
-- 42|Seattle WA|Chicago IL|228.0|26|Chicago IL|Boston MA|150.0|378.0
-- 42|Seattle WA|Chicago IL|228.0|186|Chicago IL|Boston MA|137.0|365.0
-- 42|Seattle WA|Chicago IL|228.0|288|Chicago IL|Boston MA|137.0|365.0
-- 42|Seattle WA|Chicago IL|228.0|366|Chicago IL|Boston MA|150.0|378.0
-- 42|Seattle WA|Chicago IL|228.0|1205|Chicago IL|Boston MA|128.0|356.0
-- 42|Seattle WA|Chicago IL|228.0|1240|Chicago IL|Boston MA|130.0|358.0
-- 42|Seattle WA|Chicago IL|228.0|1299|Chicago IL|Boston MA|133.0|361.0
-- 42|Seattle WA|Chicago IL|228.0|1435|Chicago IL|Boston MA|133.0|361.0
-- 42|Seattle WA|Chicago IL|228.0|1557|Chicago IL|Boston MA|122.0|350.0
-- 42|Seattle WA|Chicago IL|228.0|2503|Chicago IL|Boston MA|127.0|355.0
-- 44|Seattle WA|New York NY|322.0|84|New York NY|Boston MA|74.0|396.0
-- 44|Seattle WA|New York NY|322.0|199|New York NY|Boston MA|80.0|402.0
-- 44|Seattle WA|New York NY|322.0|235|New York NY|Boston MA|91.0|413.0
-- 44|Seattle WA|New York NY|322.0|1443|New York NY|Boston MA|80.0|402.0
-- 44|Seattle WA|New York NY|322.0|2118|New York NY|Boston MA||322.0
-- 44|Seattle WA|New York NY|322.0|2121|New York NY|Boston MA|74.0|396.0
-- 44|Seattle WA|New York NY|322.0|2122|New York NY|Boston MA|65.0|387.0
-- 44|Seattle WA|New York NY|322.0|2126|New York NY|Boston MA|60.0|382.0
-- 44|Seattle WA|New York NY|322.0|2128|New York NY|Boston MA|83.0|405.0
-- 44|Seattle WA|New York NY|322.0|2131|New York NY|Boston MA|70.0|392.0
