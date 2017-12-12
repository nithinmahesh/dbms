import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Autograder for the transaction assignment
 *
 */
@RunWith(Parameterized.class)
public class Grader
{
  /**
   * Models a single user. Callable from a thread.
   */
  static class User implements Callable<String>
  {
    Query q;
    List<String> cmds; // commands that this user will execute
    List<String> results; // the expected results from those commands

    User (List<String> cmds, List<String> results) throws Exception
    {
      this.q = new Query(FlightService.DBCONFIG_FILENAME);
      q.openConnection();
      q.prepareStatements();

      this.cmds = cmds;
      this.results = results;
    }

    public List<String> results () { return results; }

    @Override
    public String call ()
    {
      StringBuffer sb = new StringBuffer();
      for (String c : cmds)
        sb.append(FlightService.execute(q, c));

      return sb.toString();
    }

    public void shutdown () throws Exception
    {
      this.q.closeConnection();
    }
  }

  static final String COMMENTS = "#";
  static final String DELIMITER = "*"; // delimiter between command inputs and expected results
  static final String SEPARATOR = "|"; // delimiter between command inputs and expected results

  /**
   * Parse the input test case
   * @param filename test case's path and file name
   * @return new User objects with commands to run and expected results
   * @throws Exception
   */
  static List<User> parse (String filename) throws Exception
  {
    List<User> users = new ArrayList<>();

    List<String> cmds = new ArrayList<>();
    List<String> results = new ArrayList<>();
    String r = "";
    boolean isCmd = true;

    BufferedReader fr = new BufferedReader(new FileReader(filename));
    String l;
    int lineNumber = 0;
    while ( (l = fr.readLine()) != null)
    {
      lineNumber++;
      if (l.startsWith(COMMENTS))
        continue;

      else if (l.startsWith(DELIMITER))
      {
        if (isCmd)
          isCmd = false;
        else
        {
          results.add(r);
          users.add(new User(cmds, results));
          cmds = new ArrayList<>();
          results = new ArrayList<>();
          r = "";
          isCmd = true;
        }
      }

      else if (l.startsWith(SEPARATOR))
      {
        if (isCmd)
          throw new Exception("input file is malformatted on line: " + lineNumber);
        else
        {
          results.add(r);
          r = "";
        }
      }

      else
      {
        // remove trailing comments
        l = l.split(COMMENTS, 2)[0];

        if (isCmd)
          cmds.add(l);
        else
          r = r + l + "\n";
      }
    }

    fr.close();

    // everything should be parsed by now and put into user objects
    if (cmds.size() > 0 || r.length() > 0 || results.size() > 0)
      throw new Exception("input file is malformatted, cmds.size()=" + cmds.size() + ", r.length()=" + r.length() + ", results.size()=" + results.size());

    // check that all users have the same number of possible scenarios
    int n = users.get(0).results().size();
    for (int i = 1; i < users.size(); ++i)
    {
      int u = users.get(i).results().size();
      if (u != n)
        throw new Exception("user " + i + " should have " + n + " possible results rather than " + u);
    }

    return users;
  }

  // maximum number of concurrent users we will be testing
  protected static final int MAX_USERS = 5;
  // thread pool used to run different users
  protected static ExecutorService pool;
  // folder name and path that contains the test cases
  protected static String casesFolder;

  /**
   * Creates the thread pool to execute test cases with multiple users.
   * This method is called before the entire test suite is executed.
   */
  @BeforeClass
  public static void setup()
  {
    System.out.println("running setup");
    pool = Executors.newFixedThreadPool(MAX_USERS);
  }

  protected String file;

  public Grader (String file)
  {
    this.file = file;
  }

  @Parameterized.Parameters
  public static List<String> files () throws IOException
  {
    casesFolder = System.getProperty("folder");
    if (casesFolder == null || casesFolder.length() == 0)
      casesFolder = "cases"; // default, override with -Dfolder=xxx

    System.out.println("running cases from: " + casesFolder + "\n");
    try (Stream<Path> paths = Files.walk(Paths.get(casesFolder)))
    {
      return paths.filter(Files::isRegularFile).map(p -> p.toAbsolutePath().toString())
              .collect(Collectors.toList());
    }

    // comment out the above and use this to run individual test cases
//    return Arrays.asList( Paths.get("/home/nithinm/study/csep544-2017au-nithinm/hw8/starter-code/cases/simul_payment.txt").toString() );
  }

  @Before
  public void clearDB ()
  {
    try
    {
      Query q = new Query(FlightService.DBCONFIG_FILENAME);
      q.openConnection();
      q.prepareStatements();
      q.clearTables();
      q.closeConnection();
    } catch (Exception e) { e.printStackTrace(); }
  }

  @Test
  public void runTest () throws Exception
  {
    System.out.println("running test: " + this.file);
    List<User> users = parse(this.file);
    List<Future<String>> futures = new ArrayList<>();

    for (User u : users)
      futures.add(pool.submit(u));

    List<String> outputs = new ArrayList<>();
    for (Future<String> f : futures)
      outputs.add(f.get());

    boolean passed = false;
    // record all possible outcomes to display for debugging
    Map<Integer, List<String>> outcomes = new HashMap<Integer, List<String>>();
    int n = users.get(0).results().size();
    // there are n possible scenarios. Does the returned output correspond to any of them?
    for (int i = 0; i < n; ++i)
    {
      boolean isSame = true;
      // check whether the output from all users match outcome i
      for (int j = 0; j < users.size(); ++j) {
        isSame = isSame && outputs.get(j).equals(users.get(j).results().get(i));
        if (!outcomes.containsKey(i)) {
          outcomes.put(i, new ArrayList<String>());
        }
        outcomes.get(i).add(users.get(j).results().get(i));
      }
      // the test is passed if the output from all users match one of the possible n scenarios.
      passed = passed || isSame;
    }

    if (passed)
      System.out.println("passed");
    else
      System.out.println("failed");
    
    String outcomesFormatted = "";
    // if we failed, print the outcomes we were looking for
    if (!passed)
    {
      for (Map.Entry<Integer, List<String>> outcome : outcomes.entrySet())
      {
        outcomesFormatted += "=====Outcome " + outcome.getKey() + "=====\n";
        outcomesFormatted += outcome.getValue().toString() + "\n";
      }
    }
    // print out the returned outputs if test is not passed
    assertTrue("Failed: actual outputs for " + this.file + " were: \n" + outputs + "\n\nPossible outcomes were: \n" + outcomesFormatted, passed);
       
    for (User u : users)
      u.shutdown();
  }
}
