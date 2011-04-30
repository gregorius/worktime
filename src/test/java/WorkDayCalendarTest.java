import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static junit.framework.Assert.assertEquals;

/**
 * Author: Geir E. Hansen
 *
 * The WorkDayCalendar.java class contains 205 nominal lines.
 *
 * There are 137 source code lines.
 */
public class WorkDayCalendarTest {

  private class TestValueHolder {
    Date start;
    float increment;
    String stringResult;

    public TestValueHolder(Date time, float increment, String stringResult) {
      this.start = time;
      this.increment = increment;
      this.stringResult = stringResult;
    }
  }

  private List<TestValueHolder> testValueHolders;
  private WorkDayCalendar workdayCalendar;


  @Before
  public void setUp() throws Exception {
    testValueHolders = new ArrayList<TestValueHolder>();
    testValueHolders.add(new TestValueHolder(new GregorianCalendar(2004, Calendar.MAY, 24, 18, 5).getTime(),
                                             -5.5f, "14-05-2004 12:00"));
    testValueHolders.add(new TestValueHolder(new GregorianCalendar(2004, Calendar.MAY, 24, 19, 3).getTime(),
                                             44.723656f, "27-07-2004 13:47"));
    testValueHolders.add(new TestValueHolder(new GregorianCalendar(2004, Calendar.MAY, 24, 18, 3).getTime(),
                                             -6.7470217f, "13-05-2004 10:02"));
    testValueHolders.add(new TestValueHolder(new GregorianCalendar(2004, Calendar.MAY, 24, 8, 3).getTime(),
                                             12.782709f, "10-06-2004 14:18"));
    testValueHolders.add(new TestValueHolder(new GregorianCalendar(2004, Calendar.MAY, 24, 7, 3).getTime(),
                                             8.276628f, "04-06-2004 10:12"));
    testValueHolders.add(new TestValueHolder(new GregorianCalendar(2004, Calendar.MAY, 24, 7, 0).getTime(),
                                             -1.0f, "21-05-2004 08:00"));
    testValueHolders.add(new TestValueHolder(new GregorianCalendar(2004, Calendar.MAY, 24, 16, 30).getTime(),
                                             1.0f, "25-05-2004 08:00"));

    workdayCalendar = new WorkDayCalendar();
    workdayCalendar.setWorkdayStartAndStop(new GregorianCalendar(2004, Calendar.JANUARY, 1, 8, 0),
                                           new GregorianCalendar(2004, Calendar.JANUARY, 1, 16, 0));
    workdayCalendar.setRecurringHoliday(new GregorianCalendar(2004, Calendar.MAY, 17, 0, 0));
    workdayCalendar.setHoliday(new GregorianCalendar(2004, Calendar.MAY, 27, 0, 0));
  }

  @Test
  public void testGetWorkdayIncrement() throws Exception {
    SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    for (TestValueHolder testValueHolder : testValueHolders) {
      Date date = workdayCalendar.getWorkdayIncrement(testValueHolder.start, testValueHolder.increment);
      assertEquals(testValueHolder.stringResult, f.format(date));
    }
  }

  @Test
  public void testIncrementToDaysHoursAndMinutes() throws Exception {
    int[] days = new int[]{-5, 44, -6, 12, 8, -1, 1};
    int[] hours = new int[]{-4, 5, -5, 6, 2, 0, 0};
    int[] minutes = new int[]{0, 47, -58, 15, 12, 0, 0};

    for (int i = 0, testValueHoldersSize = testValueHolders.size(); i < testValueHoldersSize; i++) {
      TestValueHolder testValueHolder = testValueHolders.get(i);
      WorkDayCalendar.APeriod period = workdayCalendar.getAPeriod(testValueHolder.increment);
      assertEquals(days[i], period.getDays());
      assertEquals(hours[i], period.getHours());
      assertEquals(minutes[i], period.getMinutes());
    }
  }
}
