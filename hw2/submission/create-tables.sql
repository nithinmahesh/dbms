PRAGMA foreign_keys=ON;
create table FLIGHTS (fid int PRIMARY KEY, year int, month_id int, day_of_month int, 
   day_of_week_id int, carrier_id varchar(3), flight_num int,
   origin_city varchar(34), origin_state varchar(47), 
   dest_city varchar(34), dest_state varchar(46), 
   departure_delay double, taxi_out double, arrival_delay double,
   canceled int, actual_time double, distance double, capacity int, 
   price double,
   FOREIGN KEY(carrier_id) REFERENCES CARRIERS(cid),
   FOREIGN KEY(month_id) REFERENCES MONTHS(mid),
   FOREIGN KEY(day_of_week_id) REFERENCES WEEKDAYS(did));
create table CARRIERS (cid varchar(7) PRIMARY KEY, name varchar(83));
create table MONTHS (mid int PRIMARY KEY, month varchar(9));
create table WEEKDAYS (did int PRIMARY KEY, day_of_week varchar(9));
.separator ,
.import carriers.csv CARRIERS
.import months.csv MONTHS
.import weekdays.csv WEEKDAYS
.import flights-small.csv FLIGHTS
