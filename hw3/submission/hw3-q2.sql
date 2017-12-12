-- run time on S0 DB: 4 sec
-- result row count: 109 rows
select origin_city as city from FLIGHTS
group by origin_city
having max(actual_time) < 180
order by origin_city asc

-- Partial output:
--Aberdeen SD
--Abilene TX
--Alpena MI
--Ashland WV
--Augusta GA
--Barrow AK
--Beaumont/Port Arthur TX
--Bemidji MN
--Bethel AK
--Binghamton NY
--Brainerd MN
--Bristol/Johnson City/Kingsport TN
--Butte MT
--Carlsbad CA
--Casper WY
--Cedar City UT
--Chico CA
--College Station/Bryan TX
--Columbia MO
--Columbus GA

