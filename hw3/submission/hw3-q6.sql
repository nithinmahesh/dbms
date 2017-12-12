-- run time on S0 DB: 1 sec
-- result row count: 4 rows
select name as carrier from CARRIERS
where cid in (
select distinct carrier_id from FLIGHTS
where origin_city = 'Seattle WA' and dest_city = 'San Francisco CA'
)
-- Partial output:
--Alaska Airlines Inc.
--SkyWest Airlines Inc.
--United Air Lines Inc.
--Virgin America