import java.io.FileInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Runs queries against a back-end database
 */
public class Query
{
  private String configFilename;
  private Properties configProps = new Properties();

  private String jSQLDriver;
  private String jSQLUrl;
  private String jSQLUser;
  private String jSQLPassword;

  // DB Connection
  private Connection conn;

  // Logged In User
  private String username; // customer username is unique

  // Canned queries

  private static final String CHECK_FLIGHT_CAPACITY = "SELECT (capacity - booked_capacity) as capacity FROM Flights " +
          "WHERE fid = ?";
  private PreparedStatement checkFlightCapacityStatement;

  private static final String UPDATE_FLIGHT_CAPACITY = "UPDATE Flights SET booked_capacity = booked_capacity + 1 " +
          "WHERE fid = ?";
  private PreparedStatement updateFlightCapacityStatement;

  private static final String CLEAR_USERS = "DELETE FROM Users";
  private PreparedStatement clearUsersStatement;

  private static final String CLEAR_RESERVATIONS = "DELETE FROM Reservations";
  private PreparedStatement clearReservationsStatement;

  private static final String RESET_RESERVATION_IDS = "DBCC CHECKIDENT(\"Reservations\", RESEED, 0)";
  private PreparedStatement resetReservationIdsStatement;

  private static final String RESET_FLIGHT_CAPACITY = "EXEC dbo.sp_resetFlightCapacity";
  private PreparedStatement resetFlightCapacityStatement;

  private static final String VALIDATE_LOGIN = "SELECT COUNT(*) as count FROM Users WHERE username = ? AND password = ?";
  private PreparedStatement validateLoginStatement;

  private static final String CREATE_LOGIN = "INSERT INTO Users VALUES (?, ?, ?)";
  private PreparedStatement createLoginStatement;

  private static final String GET_DIRECT_FLIGHTS = "SELECT TOP (?) " +
          "fid,year,month_id,day_of_month,carrier_id,flight_num,origin_city,dest_city,actual_time,capacity,price "
          + "FROM Flights "
          + "WHERE origin_city = ? AND dest_city = ? AND day_of_month = ? " +
          "AND canceled = 0 "
          + "ORDER BY actual_time ASC, fid ASC";
  private PreparedStatement getDirectFlightsStatement;

  private static final String GET_NONDIRECT_FLIGHTS = "SELECT TOP (?) " +
          "A.fid as A_fid,A.year as A_year,A.month_id as A_month_id,A.day_of_month as A_day_of_month," +
          "A.carrier_id as A_carrier_id,A.flight_num as A_flight_num,A.origin_city as A_origin_city," +
          "A.dest_city as A_dest_city,A.actual_time as A_actual_time,A.capacity as A_capacity,A.price as A_price, " +
          "B.fid as B_fid,B.year as B_year,B.month_id as B_month_id,B.day_of_month as B_day_of_month," +
          "B.carrier_id as B_carrier_id,B.flight_num as B_flight_num,B.origin_city as B_origin_city," +
          "B.dest_city as B_dest_city,B.actual_time as B_actual_time,B.capacity as B_capacity,B.price as B_price, " +
          "A.actual_time + B.actual_time as total_time "
          + "FROM Flights A, Flights B "
          + "WHERE A.origin_city = ? AND B.dest_city = ? AND A.day_of_month = ? AND A.day_of_month = B.day_of_month" +
          " AND A.dest_city = B.origin_city AND A.canceled = 0 and B.canceled = 0 "
          + "ORDER BY total_time ASC, A_fid ASC, B_fid ASC";
  private PreparedStatement getNonDirectFlightsStatement;

  private static final String BOOK_ITINERARY = "INSERT INTO Reservations " +
          "(username, flight_id_1, flight_id_2, date,month,year, total_price) " +
          "OUTPUT Inserted.reservation_id " +
          "VALUES (?, ?, ?, ?, ?, ?, ?)";
  private PreparedStatement bookItineraryStatement;

  private static final String GET_USER_RES_ON_DATE = "SELECT COUNT(*) as count FROM view_Reservations " +
          "WHERE username = ? AND date = ? ";
  private PreparedStatement getUserReservationOnDateStatement;

  private static final String GET_SINGLE_USER_RES = "SELECT * FROM view_Reservations " +
          "WHERE username = ? AND reservation_id = ? ";
  private PreparedStatement getSingleUserReservationStatement;

  private static final String GET_USER_RESERVATION = "EXEC sp_view_Reservations_detail " +
          "@username = ? ";
  private PreparedStatement getUserReservationsStatement;

  private static final String PAY_RESERVATION = "EXEC sp_pay_Reservation " +
          "@username = ?, @res_id = ?, @price = ?";
  private PreparedStatement payReservationStatement;

  private static final String CANCEL_RESERVATION = "EXEC sp_cancel_Reservation " +
          "@username = ?, @res_id = ?, @refund = ?";
  private PreparedStatement cancelReservationStatement;

  private static final String GET_USER_BALANCE = "SELECT * FROM Users " +
          "WHERE username = ? ";
  private PreparedStatement getUserBalanceStatement;
  // transactions
  private static final String BEGIN_TRANSACTION_SQL = "SET TRANSACTION ISOLATION LEVEL SERIALIZABLE; BEGIN TRANSACTION;";
  private PreparedStatement beginTransactionStatement;

  private static final String COMMIT_SQL = "COMMIT TRANSACTION";
  private PreparedStatement commitTransactionStatement;

  private static final String ROLLBACK_SQL = "ROLLBACK TRANSACTION";
  private PreparedStatement rollbackTransactionStatement;

  private Itinerary[] searchResults;
  private int searchResultCount;

  class Itinerary
  {
    public Flight flight1;
    public Flight flight2;
    public boolean direct;

    public Itinerary(Flight f)
    {
      direct = true;
      flight1 = f;
      flight2 = null;
    }

    public Itinerary(Flight f1, Flight f2)
    {
      direct = false;
      flight1 = f1;
      flight2 = f2;
    }

    @Override
    public String toString()
    {
      int flightCount = direct ? 1 : 2;
      double flightTime = direct ? flight1.time : flight1.time + flight2.time;
      return flightCount + " flight(s), " + flightTime + " minutes\n" + flight1.toString() + "\n" +
              (direct == false ? flight2.toString() + "\n" : "");
    }
  }

  class Flight
  {
    public int fid;
    public int year;
    public int monthId;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public double time;
    public int capacity;
    public double price;

    public Flight(int fid, int year, int monthId, int dayOfMonth, String carrierId, String flightNum, String originCity,
                  String destCity, double time, int capacity, double price)
    {
      this.fid = fid;
      this.year = year;
      this.monthId = monthId;
      this.dayOfMonth = dayOfMonth;
      this.carrierId = carrierId;
      this.flightNum = flightNum;
      this.originCity = originCity;
      this.destCity = destCity;
      this.time = time;
      this.capacity = capacity;
      this.price = price;
    }

    public Flight(ResultSet r, String prefix) throws SQLException
    {
      this.fid = r.getInt(prefix + "fid");
      this.year = r.getInt(prefix + "year");
      this.monthId = r.getInt(prefix + "month_id");
      this.dayOfMonth = r.getInt(prefix + "day_of_month");
      this.carrierId = r.getString(prefix + "carrier_id");
      this.flightNum = r.getString(prefix + "flight_num");
      this.originCity = r.getString(prefix + "origin_city");
      this.destCity = r.getString(prefix + "dest_city");
      this.time = r.getDouble(prefix + "actual_time");
      this.capacity = r.getInt(prefix + "capacity");
      this.price = r.getDouble(prefix + "price");
    }

    @Override
    public String toString()
    {
      return "ID: " + fid + " Date: " + year + "-" + monthId + "-" + dayOfMonth + " Carrier: " + carrierId +
              " Number: " + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time +
              " Capacity: " + capacity + " Price: " + price;
    }
  }

  public Query(String configFilename)
  {
    this.configFilename = configFilename;
  }

  /* Connection code to SQL Azure.  */
  public void openConnection() throws Exception
  {
    configProps.load(new FileInputStream(configFilename));

    jSQLDriver = configProps.getProperty("flightservice.jdbc_driver");
    jSQLUrl = configProps.getProperty("flightservice.url");
    jSQLUser = configProps.getProperty("flightservice.sqlazure_username");
    jSQLPassword = configProps.getProperty("flightservice.sqlazure_password");

		/* load jdbc drivers */
    Class.forName(jSQLDriver).newInstance();

		/* open connections to the flights database */
    conn = DriverManager.getConnection(jSQLUrl, // database
            jSQLUser, // user
            jSQLPassword); // password

    conn.setAutoCommit(true); //by default automatically commit after each statement

		/* You will also want to appropriately set the transaction's isolation level through:
		   conn.setTransactionIsolation(...)
		   See Connection class' JavaDoc for details.
		 */
  }

  public void closeConnection() throws Exception
  {
    conn.close();
  }

  /**
   * Clear the data in any custom tables created. Do not drop any tables and do not
   * clear the flights table. You should clear any tables you use to store reservations
   * and reset the next reservation ID to be 1.
   */
  public void clearTables () throws SQLException
  {
    // your code here
    clearReservationsStatement.clearParameters();
    clearReservationsStatement.executeUpdate();
    clearUsersStatement.clearParameters();
    clearUsersStatement.executeUpdate();
    resetReservationIdsStatement.clearParameters();
    resetReservationIdsStatement.executeUpdate();
    resetFlightCapacityStatement.clearParameters();
    resetFlightCapacityStatement.executeUpdate();

    return;
  }

	/**
   * prepare all the SQL statements in this method.
   * "preparing" a statement is almost like compiling it.
   * Note that the parameters (with ?) are still not filled in
   */
  public void prepareStatements() throws Exception
  {
    beginTransactionStatement = conn.prepareStatement(BEGIN_TRANSACTION_SQL);
    commitTransactionStatement = conn.prepareStatement(COMMIT_SQL);
    rollbackTransactionStatement = conn.prepareStatement(ROLLBACK_SQL);

    checkFlightCapacityStatement = conn.prepareStatement(CHECK_FLIGHT_CAPACITY);
    updateFlightCapacityStatement = conn.prepareStatement(UPDATE_FLIGHT_CAPACITY);
    resetFlightCapacityStatement = conn.prepareStatement(RESET_FLIGHT_CAPACITY);

    /* add here more prepare statements for all the other queries you need */
		/* . . . . . . */
    clearReservationsStatement = conn.prepareStatement(CLEAR_RESERVATIONS);
    clearUsersStatement = conn.prepareStatement(CLEAR_USERS);
    resetReservationIdsStatement = conn.prepareStatement(RESET_RESERVATION_IDS);
    validateLoginStatement = conn.prepareStatement(VALIDATE_LOGIN);
    createLoginStatement = conn.prepareStatement(CREATE_LOGIN);
    getDirectFlightsStatement = conn.prepareStatement(GET_DIRECT_FLIGHTS);
    getNonDirectFlightsStatement = conn.prepareStatement(GET_NONDIRECT_FLIGHTS);
    bookItineraryStatement = conn.prepareStatement(BOOK_ITINERARY);
    getUserReservationOnDateStatement = conn.prepareStatement(GET_USER_RES_ON_DATE);
    getUserReservationsStatement = conn.prepareStatement(GET_USER_RESERVATION);
    getSingleUserReservationStatement = conn.prepareStatement(GET_SINGLE_USER_RES);
    getUserBalanceStatement = conn.prepareStatement(GET_USER_BALANCE);
    payReservationStatement = conn.prepareStatement(PAY_RESERVATION);
    cancelReservationStatement = conn.prepareStatement(CANCEL_RESERVATION);
  }

  /**
   * Takes a user's username and password and attempts to log the user in.
   *
   * @param username
   * @param password
   *
   * @return If someone has already logged in, then return "User already logged in\n"
   * For all other errors, return "Login failed\n".
   *
   * Otherwise, return "Logged in as [username]\n".
   */
  public String transaction_login(String username, String password)
  {
    try {
      if (this.username != null)
      {
        return "User already logged in\n";
      }

      validateLoginStatement.clearParameters();
      validateLoginStatement.setString(1, username);
      validateLoginStatement.setString(2, password);
      ResultSet results = validateLoginStatement.executeQuery();
      results.next();
      int count = results.getInt("count");
      results.close();

      if (count == 1) {
        this.username = username;
        this.searchResults = null;
        this.searchResultCount = 0;
        return "Logged in as " + username + "\n";
      }
    }
    catch (Exception e) {
      log(e);
      return "Login failed\n";
    }

    return "Login failed\n";
  }

  private void log(String msg) {
//    System.out.println(msg + "\n");
  }

  private void log(Exception e) {
    log(e.toString());
    log(e.getMessage());
//    e.printStackTrace(System.out);
  }

  /**
   * Implement the create user function.
   *
   * @param username new user's username. User names are unique the system.
   * @param password new user's password.
   * @param initAmount initial amount to deposit into the user's account, should be >= 0 (failure otherwise).
   *
   * @return either "Created user {@code username}\n" or "Failed to create user\n" if failed.
   */
  public String transaction_createCustomer (String username, String password, double initAmount)
  {
    try {
      createLoginStatement.clearParameters();
      createLoginStatement.setString(1, username);
      createLoginStatement.setString(2, password);
      createLoginStatement.setDouble(3, initAmount);
      createLoginStatement.executeUpdate();
      return "Created user " + username + "\n";
    }
    catch (Exception e) {
      log(e);

      return "Failed to create user\n";
    }
  }

  /**
   * Implement the search function.
   *
   * Searches for flights from the given origin city to the given destination
   * city, on the given day of the month. If {@code directFlight} is true, it only
   * searches for direct flights, otherwise is searches for direct flights
   * and flights with two "hops." Only searches for up to the number of
   * itineraries given by {@code numberOfItineraries}.
   *
   * The results are sorted based on total flight time.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight if true, then only search for direct flights, otherwise include indirect flights as well
   * @param dayOfMonth
   * @param numberOfItineraries number of itineraries to return
   *
   * @return If no itineraries were found, return "No flights match your selection\n".
   * If an error occurs, then return "Failed to search\n".
   *
   * Otherwise, the sorted itineraries printed in the following format:
   *
   * Itinerary [itinerary number]: [number of flights] flight(s), [total flight time] minutes\n
   * [first flight in itinerary]\n
   * ...
   * [last flight in itinerary]\n
   *
   * Each flight should be printed using the same format as in the {@code Flight} class. Itinerary numbers
   * in each search should always start from 0 and increase by 1.
   *
   * @see Flight#toString()
   */
  public String transaction_search(String originCity, String destinationCity, boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries)
  {
    String resultStr = "";

    try {
      getDirectFlightsStatement.clearParameters();
      getDirectFlightsStatement.setInt(1, numberOfItineraries);
      getDirectFlightsStatement.setString(2, originCity);
      getDirectFlightsStatement.setString(3, destinationCity);
      getDirectFlightsStatement.setInt(4, dayOfMonth);

      searchResults = new Itinerary[numberOfItineraries];
      int countOfItineraries = 0;
      ResultSet results = getDirectFlightsStatement.executeQuery();
      while(results.next())
      {
        searchResults[countOfItineraries] = new Itinerary(new Flight(results, ""));
        countOfItineraries++;
      }

      results.close();

      if (directFlight == false)
      {
        // find one stop flights also
        getNonDirectFlightsStatement.clearParameters();
        getNonDirectFlightsStatement.setInt(1, numberOfItineraries - countOfItineraries);
        getNonDirectFlightsStatement.setString(2, originCity);
        getNonDirectFlightsStatement.setString(3, destinationCity);
        getNonDirectFlightsStatement.setInt(4, dayOfMonth);

        ResultSet resultsND = getNonDirectFlightsStatement.executeQuery();
        while(resultsND.next())
        {
          searchResults[countOfItineraries] = new Itinerary(new Flight(resultsND, "A_"),
                  new Flight(resultsND, "B_"));
          countOfItineraries++;
        }

        resultsND.close();
      }

      if (countOfItineraries == 0)
      {
        return "No flights match your selection\n";
      }

      for (int i = 0; i < countOfItineraries; i++)
      {
        resultStr += "Itinerary " + i + ": ";
        resultStr += searchResults[i].toString();
      }

      searchResultCount = countOfItineraries;

      return resultStr;
    }
    catch (Exception e) {
      log(e);
      return "Failed to search\n";
    }
  }

  /**
   * Same as {@code transaction_search} except that it only performs single hop search and
   * do it in an unsafe manner.
   *
   * @param originCity
   * @param destinationCity
   * @param directFlight
   * @param dayOfMonth
   * @param numberOfItineraries
   *
   * @return The search results. Note that this implementation *does not conform* to the format required by
   * {@code transaction_search}.
   */
  private String transaction_search_unsafe(String originCity, String destinationCity, boolean directFlight,
                                          int dayOfMonth, int numberOfItineraries)
  {
    StringBuffer sb = new StringBuffer();

    try
    {
      // one hop itineraries
      String unsafeSearchSQL =
              "SELECT TOP (" + numberOfItineraries + ") year,month_id,day_of_month,carrier_id,flight_num,origin_city,actual_time "
                      + "FROM Flights "
                      + "WHERE origin_city = \'" + originCity + "\' AND dest_city = \'" + destinationCity + "\' AND day_of_month =  " + dayOfMonth + " "
                      + "ORDER BY actual_time ASC";

      Statement searchStatement = conn.createStatement();
      ResultSet oneHopResults = searchStatement.executeQuery(unsafeSearchSQL);

      while (oneHopResults.next())
      {
        int result_year = oneHopResults.getInt("year");
        int result_monthId = oneHopResults.getInt("month_id");
        int result_dayOfMonth = oneHopResults.getInt("day_of_month");
        String result_carrierId = oneHopResults.getString("carrier_id");
        String result_flightNum = oneHopResults.getString("flight_num");
        String result_originCity = oneHopResults.getString("origin_city");
        int result_time = oneHopResults.getInt("actual_time");
        sb.append("Flight: " + result_year + "," + result_monthId + "," + result_dayOfMonth + "," + result_carrierId + "," + result_flightNum + "," + result_originCity + "," + result_time);
      }
      oneHopResults.close();
    } catch (SQLException e) { e.printStackTrace(); }

    return sb.toString();
  }

  /**
   * Implements the book itinerary function.
   *
   * @param itineraryId ID of the itinerary to book. This must be one that is returned by search in the current session.
   *
   * @return If the user is not logged in, then return "Cannot book reservations, not logged in\n".
   * If try to book an itinerary with invalid ID, then return "No such itinerary {@code itineraryId}\n".
   * If the user already has a reservation on the same day as the one that they are trying to book now, then return
   * "You cannot book two flights in the same day\n".
   * For all other errors, return "Booking failed\n".
   *
   * And if booking succeeded, return "Booked flight(s), reservation ID: [reservationId]\n" where
   * reservationId is a unique number in the reservation system that starts from 1 and increments by 1 each time a
   * successful reservation is made by any user in the system.
   */
  public String transaction_book(int itineraryId)
  {
    if (username == null)
    {
      return "Cannot book reservations, not logged in\n";
    }

    if (searchResultCount <= itineraryId || itineraryId < 0)
    {
      return "No such itinerary " + itineraryId + "\n";
    }

    String errorMsg = "Booking failed\n";

    Itinerary itToBook = searchResults[itineraryId];
    try {
      beginTransaction();
      getUserReservationOnDateStatement.clearParameters();
      getUserReservationOnDateStatement.setString(1, username);
      getUserReservationOnDateStatement.setInt(2, itToBook.flight1.dayOfMonth);
      ResultSet reservations = getUserReservationOnDateStatement.executeQuery();
      reservations.next();
      int count = reservations.getInt("count");
      reservations.close();

      if (count != 0) {
        rollbackTransaction();
        return "You cannot book two flights in the same day\n";
      }

      // CHeck capacity
      boolean hasCapacity = checkFlightCapacity(itToBook.flight1.fid) > 0;
      if(hasCapacity && itToBook.direct == false)
      {
        hasCapacity = checkFlightCapacity(itToBook.flight2.fid) > 0;
      }

      if(hasCapacity)
      {
        updateFlightCapacity(itToBook.flight1.fid);
        if(itToBook.direct == false) {
          updateFlightCapacity(itToBook.flight2.fid);
        }

        bookItineraryStatement.clearParameters();
        bookItineraryStatement.setString(1, username);
        bookItineraryStatement.setInt(2, itToBook.flight1.fid);
        double totalPrice = itToBook.flight1.price;
        if(itToBook.direct)
        {
          bookItineraryStatement.setNull(3, Types.INTEGER);
        }
        else
        {
          totalPrice += itToBook.flight2.price;
          bookItineraryStatement.setInt(3, itToBook.flight2.fid);
        }
        bookItineraryStatement.setInt(4, itToBook.flight1.dayOfMonth);
        bookItineraryStatement.setInt(5, itToBook.flight1.monthId);
        bookItineraryStatement.setInt(6, itToBook.flight1.year);
        bookItineraryStatement.setDouble(7, totalPrice);
        ResultSet insertRes = bookItineraryStatement.executeQuery();
        insertRes.next();
        int res_id = insertRes.getInt("reservation_id");
        insertRes.close();

        commitTransaction();
        return "Booked flight(s), reservation ID: " + res_id + "\n";
      }
      else
      {
        log("No capacity");
      }
    }
    catch (Exception e) {
      log(e);
      if (e.getMessage().contains("Violation of UNIQUE KEY constraint 'CK_User_Per_Day'."))
        // unique constraint is hit on reservation
      {
        errorMsg = "You cannot book two flights in the same day\n";
      }
    }

    try
    {
      rollbackTransaction();
    } catch(Exception e)
    {
      log("Exception during rollback tran");
      log(e);
    }
    return errorMsg;
  }

  /**
   * Implements the reservations function.
   *
   * @return If no user has logged in, then return "Cannot view reservations, not logged in\n"
   * If the user has no reservations, then return "No reservations found\n"
   * For all other errors, return "Failed to retrieve reservations\n"
   *
   * Otherwise return the reservations in the following format:
   *
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * Reservation [reservation ID] paid: [true or false]:\n"
   * [flight 1 under the reservation]
   * [flight 2 under the reservation]
   * ...
   *
   * Each flight should be printed using the same format as in the {@code Flight} class.
   *
   * @see Flight#toString()
   */
  public String transaction_reservations()
  {
    if (username == null)
    {
      return "Cannot view reservations, not logged in\n";
    }

    try {
      getUserReservationsStatement.clearParameters();
      getUserReservationsStatement.setString(1, username);
      ResultSet results = getUserReservationsStatement.executeQuery();

      int resCount = 0;
      String resultStr = "";
      while(results.next())
      {
        resultStr += "Reservation " + results.getInt("reservation_id") + " paid: ";
        resultStr += results.getInt("paid") == 0 ? "false" : "true";
        resultStr += ":\n";
        resultStr += (new Flight(results, "A_")).toString();
        resultStr += "\n";
        if(results.getInt("B_fid") != 0)
        {
          resultStr += (new Flight(results, "B_")).toString();
          resultStr += "\n";
        }

        resCount++;
      }

      if (resCount == 0)
      {
        return "No reservations found\n";
      }

      return resultStr;
    }
    catch(Exception e)
    {
      log(e);
    }
    return "Failed to retrieve reservations\n";
  }

  /**
   * Implements the cancel operation.
   *
   * @param reservationId the reservation ID to cancel
   *
   * @return If no user has logged in, then return "Cannot cancel reservations, not logged in\n"
   * For all other errors, return "Failed to cancel reservation [reservationId]\n"
   *
   * If successful, return "Canceled reservation [reservationId]\n"
   *
   * Even though a reservation has been canceled, its ID should not be reused by the system.
   */
  public String transaction_cancel(int reservationId)
  {
    if (username == null)
    {
      return "Cannot cancel reservations, not logged in\n";
    }

    try {
      beginTransaction();
      getSingleUserReservationStatement.clearParameters();
      getSingleUserReservationStatement.setString(1, username);
      getSingleUserReservationStatement.setInt(2, reservationId);
      ResultSet resToCancel = getSingleUserReservationStatement.executeQuery();
      if(!resToCancel.next())// reservation does not exist
      {
        resToCancel.close();
        rollbackTransaction();
        return "Failed to cancel reservation " + reservationId + "\n";
      }

      double refund = resToCancel.getInt("paid") == 1 ? resToCancel.getDouble("total_price") : 0;
      resToCancel.close();

      cancelReservationStatement.clearParameters();
      cancelReservationStatement.setString(1, username);
      cancelReservationStatement.setInt(2, reservationId);
      cancelReservationStatement.setDouble(3, refund);
      cancelReservationStatement.executeUpdate();

      commitTransaction();
      return "Canceled reservation " + reservationId + "\n";
    }
    catch(Exception e)
    {
      log(e);
    }

    try
    {
      rollbackTransaction();
    }
    catch(Exception e)
    {
      log(e);
    }

    return "Failed to cancel reservation " + reservationId + "\n";
  }

  /**
   * Implements the pay function.
   *
   * @param reservationId the reservation to pay for.
   *
   * @return If no user has logged in, then return "Cannot pay, not logged in\n"
   * If the reservation is not found / not under the logged in user's name, then return
   * "Cannot find unpaid reservation [reservationId] under user: [username]\n"
   * If the user does not have enough money in their account, then return
   * "User has only [balance] in account but itinerary costs [cost]\n"
   * For all other errors, return "Failed to pay for reservation [reservationId]\n"
   *
   * If successful, return "Paid reservation: [reservationId] remaining balance: [balance]\n"
   * where [balance] is the remaining balance in the user's account.
   */
  public String transaction_pay (int reservationId)
  {
    if (username == null)
    {
      return "Cannot pay, not logged in\n";
    }

    String errorMsg = "Failed to pay for reservation " + reservationId + "\n";

    try
    {
      beginTransaction();
      getSingleUserReservationStatement.clearParameters();
      getSingleUserReservationStatement.setString(1, username);
      getSingleUserReservationStatement.setInt(2, reservationId);
      ResultSet resToPay = getSingleUserReservationStatement.executeQuery();
      if(!resToPay.next() || resToPay.getInt("paid") != 0)// reservation does not exist or paid for already
      {
        resToPay.close();
        rollbackTransaction();
        return "Cannot find unpaid reservation " + reservationId + " under user: " + username + "\n";
      }
      double price = resToPay.getDouble("total_price");
      resToPay.close();

      getUserBalanceStatement.clearParameters();
      getUserBalanceStatement.setString(1, username);
      ResultSet userBalRes = getUserBalanceStatement.executeQuery();
      userBalRes.next();
      double balance = userBalRes.getDouble("balance");
      userBalRes.close();

      if(balance < price) // not enough balance
      {
        rollbackTransaction();
        return "User has only " + balance + " in account but itinerary costs " + price + "\n";
      }

      payReservationStatement.clearParameters();
      payReservationStatement.setString(1, username);
      payReservationStatement.setInt(2, reservationId);
      payReservationStatement.setDouble(3, price);
      payReservationStatement.executeUpdate();

      commitTransaction();
      return "Paid reservation: " + reservationId + " remaining balance: " + (balance - price) + "\n";
    }
    catch(Exception e)
    {
      log(e);
      if (e.getMessage().contains("The UPDATE statement conflicted with the CHECK constraint \"CK_Paid\""))
      {
        errorMsg = "Cannot find unpaid reservation " + reservationId + " under user: " + username + "\n";
      }
    }

    try
    {
      rollbackTransaction();
    }
    catch(Exception e)
    {
      log(e);
    }

    return errorMsg;
  }

  /* some utility functions below */

  public void beginTransaction() throws SQLException
  {
    conn.setAutoCommit(false);
    beginTransactionStatement.executeUpdate();
  }

  public void commitTransaction() throws SQLException
  {
    commitTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  public void rollbackTransaction() throws SQLException
  {
    rollbackTransactionStatement.executeUpdate();
    conn.setAutoCommit(true);
  }

  /**
   * Shows an example of using PreparedStatements after setting arguments. You don't need to
   * use this method if you don't want to.
   */
  private int checkFlightCapacity(int fid) throws SQLException
  {
    checkFlightCapacityStatement.clearParameters();
    checkFlightCapacityStatement.setInt(1, fid);
    ResultSet results = checkFlightCapacityStatement.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;
  }

  private void updateFlightCapacity(int fid) throws SQLException
  {
    updateFlightCapacityStatement.clearParameters();
    updateFlightCapacityStatement.setInt(1, fid);
    updateFlightCapacityStatement.executeUpdate();

    return;
  }
}
