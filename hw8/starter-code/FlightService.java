import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlightService
{
  public static final String DBCONFIG_FILENAME = "dbconn.properties";

  public static void usage()
  {
    /* prints the choices for commands and parameters */
    System.out.println();
    System.out.println(" *** Please enter one of the following commands *** ");
    System.out.println("> create <username> <password> <initial amount>");
    System.out.println("> login <username> <password>");
    System.out.println("> search <origin city> <destination city> <direct> <date> <num itineraries>");
    System.out.println("> book <itinerary id>");
    System.out.println("> pay <reservation id>");
    System.out.println("> reservations");
    System.out.println("> cancel <reservation id>");
    System.out.println("> quit");
  }

  public static String[] tokenize(String command)
  {
    String regex = "\"([^\"]*)\"|(\\S+)";
    Matcher m = Pattern.compile(regex).matcher(command);
    List<String> tokens = new ArrayList<>();
    while (m.find())
    {
      if (m.group(1) != null)
        tokens.add(m.group(1));
      else
        tokens.add(m.group(2));
    }
    return tokens.toArray(new String[0]);
  }

  public static String execute (Query q, String command)
  {
    String response;

    String[] tokens = tokenize(command.trim());
    if (tokens.length == 0)
      response = "Please enter a command";

    else if (tokens[0].equals("login"))
    {
      if (tokens.length == 3)
      {
        /* authenticate the user */
        String username = tokens[1];
        String password = tokens[2];
        response = q.transaction_login(username, password);
      }
      else
        response = "Error: Please provide a username and password";
    }

    else if (tokens[0].equals("create"))
    {
      /* create a new customer */
      if (tokens.length == 4)
      {
        String username = tokens[1];
        String password = tokens[2];
        double initAmount = Double.parseDouble(tokens[3]);
        response = q.transaction_createCustomer(username, password, initAmount);
      }
      else
        response = "Error: Please provide a username, password, and initial amount in the account";
    }

    else if (tokens[0].equals("search"))
    {
				/* search for flights */
      if (tokens.length == 6)
      {
        String originCity = tokens[1];
        String destinationCity = tokens[2];
        boolean direct = tokens[3].equals("1");
        Integer day;
        Integer count;
        try
        {
          day = Integer.valueOf(tokens[4]);
          count = Integer.valueOf(tokens[5]);
          //System.out.println("Searching for flights");
          //response = q.transaction_search_unsafe(originCity, destinationCity, direct, day, count);
          response = q.transaction_search(originCity, destinationCity, direct, day, count);
        }
        catch (NumberFormatException e) { response = "Failed to parse integer"; }
      }
      else
        response = "Error: Please provide all search parameters <origin_city> <destination_city> <direct> <date> <nb itineraries>";
    }

    else if (tokens[0].equals("book"))
    {
				/* book a flight ticket */
      if (tokens.length == 2)
      {
        int itinerary_id = Integer.parseInt(tokens[1]);
        //System.out.println("Booking itinerary.");
        response = q.transaction_book(itinerary_id);
      }
      else
        response = "Error: Please provide an itinerary_id";
    }

    else if (tokens[0].equals("reservations"))
    {
				/* list all reservations */
      response = q.transaction_reservations();
    }

    else if (tokens[0].equals("pay"))
    {
      /* pay for an unpaid reservation */
      if (tokens.length == 2)
      {
        int reservation_id = Integer.parseInt(tokens[1]);
        //System.out.println("Paying reservation.");
        response = q.transaction_pay(reservation_id);
      }
      else
        response = "Error: Please provide a reservation_id";
    }

    else if (tokens[0].equals("cancel"))
    {
				/* cancel a reservation */
      if (tokens.length == 2)
      {
        int reservation_id = Integer.parseInt(tokens[1]);
        //System.out.println("Canceling reservation.");
        response = q.transaction_cancel(reservation_id);
      }
      else
        response = "Error: Please provide a reservation_id";
    }

    else if (tokens[0].equals("quit"))
      response = "Goodbye\n";

    else
      response = "Error: unrecognized command '" + tokens[0] + "'";

    return response;
  }

  /* REPL (Read-Execute-Print-Loop) */
  public static void menu(Query q) throws Exception
  {
    while (true)
    {
      usage();

      BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
      System.out.print("> ");

      String command = r.readLine();
      String response = execute(q, command);
      System.out.print(response);

      if (response.equals("Goodbye\n"))
        break;
    }
  }

  public static void main(String[] args) throws Exception
  {
		/* prepare the database connection stuff */
    Query q = new Query(DBCONFIG_FILENAME);
    q.openConnection();
    q.prepareStatements();
    menu(q); /* menu(...) does the real work */
    q.closeConnection();
  }

}