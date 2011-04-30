/**
 * Author: Geir E. Hansen
 */

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.joda.time.*;
import org.joda.time.base.BaseDateTime;

import java.util.*;

public class WorkDayCalendar {
  private List<DateTime> holidays;
  private List<DateTime> recurringHolidays;
  private LocalTime workDayStart;
  private LocalTime workDayStop;

  /**
   * this getter is here for the test only. Not used in the WorkDayCalendar code. *
   */
  public APeriod getAPeriod(float increment) {
    return new APeriod(increment);
  }

  public class APeriod {
    int days;
    int hours;
    int minutes;
    boolean positive;    //true if increment, false if decrement

    public APeriod(float daysHoursAndMinutesAsDecimal) {
      positive = daysHoursAndMinutesAsDecimal > 0.0F;

      days = (int)daysHoursAndMinutesAsDecimal;

      float percentOfWorkday = daysHoursAndMinutesAsDecimal - days;
      float decimalHours = percentOfWorkday * 8.0F;
      hours = (int)decimalHours;

      minutes = (int)((decimalHours - (float)hours) * 60.0F);
    }

    /** returns false if the increment is actually a decrement, otherwise true **/
    public boolean isPositive() {
      return positive;
    }

    public int getDays() {
      return days;
    }

    public int getHours() {
      return hours;
    }

    public int getMinutes() {
      return minutes;
    }

    public DateTime moveToWorkDay(DateTime startDate) {
      DateTime useDate = adjustStartDayAndHour(startDate);

      //now bypass holidays and weekdays
      int passedDays = 0;
      while (passedDays < Math.abs(getDays())) {
        useDate = isPositive() ? useDate.plusDays(1) : useDate.minusDays(1);  // one day at a time
        if ((isHoliday(useDate) || isRecurringHoliday(useDate) || isWeekendDay(useDate))) {
          //bypass day without counting
        } else {
          passedDays++;
        }
      }

      return useDate;
    }

    /**
     * If the time for the start date is not inside the workday times, the hour and minute of the start date is
     * adjusted to be within. If needed the day is forwarded to next day or set back one day
     */
    private DateTime adjustStartDayAndHour(DateTime startDate) {
      MutableDateTime mutableStartDate = startDate.toMutableDateTime();

      if ((mutableStartDate.getHourOfDay() >= getWorkDayStop().getHourOfDay())
              || ((mutableStartDate.getHourOfDay() == getWorkDayStop().getHourOfDay()) && (mutableStartDate.getMinuteOfHour() > 0))) {
        //startdate is after the end of the working day.
        //Set starting point at end of day. It will then be incremented to next day further below
        mutableStartDate.setHourOfDay(getWorkDayStop().getHourOfDay());
        mutableStartDate.setMinuteOfHour(0);   //assuming a workday always start at 0 minutes
      } else if (mutableStartDate.getHourOfDay() < getWorkDayStart().getHourOfDay()) {
        //startdate is before start of day. Set forward to start.
        mutableStartDate.setHourOfDay(getWorkDayStart().getHourOfDay());
        mutableStartDate.setMinuteOfHour(0);   //assuming a workday always start at 0 minutes
      }
      //increment with hours and minutes
      MutableDateTime workDay = mutableStartDate.toDateTime().plusHours(getHours()).plusMinutes(getMinutes()).toMutableDateTime();

      //After adding hours and minutes, the new hour and minute may be outside working day. Adjust again:
      int dayIncrementValue = 0;
      if ((workDay.getHourOfDay() > getWorkDayStop().getHourOfDay())
              || ((workDay.getHourOfDay() == getWorkDayStop().getHourOfDay()) && (workDay.getMinuteOfHour() > 0))) {
        //at or after end of day, add to next day
        dayIncrementValue = +1;
        //The hour is start of day + increment
        workDay.setHourOfDay(getWorkDayStart().getHourOfDay() + getHours());
        workDay.setMinuteOfHour(getMinutes());
      } else if (workDay.getHourOfDay() < getWorkDayStart().getHourOfDay()) {
        dayIncrementValue = -1;
        workDay.setHourOfDay(getWorkDayStop().getHourOfDay() - getHours());
        workDay.setMinuteOfHour(-Math.abs(getMinutes()));
      } else if (workDay.getHourOfDay() == getWorkDayStop().getHourOfDay()) {
        //we are at the end of the day with 0 minutes. That's not a time to start working. Reset to start of day
        workDay.setHourOfDay(getWorkDayStart().getHourOfDay());
      }

      //find the day by bypassing the day increment caused by hour/minutes outside workinh hours
      return workDay.toDateTime().plusDays(dayIncrementValue);
    }
  }

  public WorkDayCalendar() {
    holidays = new ArrayList<DateTime>();
    recurringHolidays = new ArrayList<DateTime>();
  }

  /**
   * the date is a holiday not to be considered as working day
   */
  public void setHoliday(Calendar date) {
    this.holidays.add(new DateTime(date));
  }

  private boolean isHoliday(BaseDateTime day) {
    //Consider only year and date, ignoring time of day
    boolean itsContained = false;
    for (DateTime dateTime : holidays) {
      if (DateTimeComparator.getDateOnlyInstance().compare(day, dateTime) == 0) {
        itsContained = true;
        break;
      }
    }
    return itsContained;
  }

  /** The date is assumed as a yearly recurring holiday */
  public void setRecurringHoliday(Calendar date) {
    this.recurringHolidays.add(new DateTime(date));
  }

  private boolean isWeekendDay(BaseDateTime day) {
    int dayOfWeek = day.getDayOfWeek();
    return (dayOfWeek == 6) || (dayOfWeek == 7);
  }

  boolean isRecurringHoliday(BaseDateTime date) {
    //compare considering the day and month of the year only, ignoring year and time of day
    boolean itsContained = false;
    for (DateTime dateTime : recurringHolidays) {
      if ((dateTime.getDayOfMonth() == date.getDayOfMonth()) && (dateTime.getMonthOfYear() == date.getMonthOfYear())) {
        itsContained = true;
        break;
      }
    }
    return itsContained;
  }

  /**
   * set start and stop time (hour) for the working day.
   *
   * @param start if null, 08:00 is the default
   * @param stop  if null, 16:00 is the default
   */
  public void setWorkdayStartAndStop(Calendar start, Calendar stop) {
    workDayStart = start == null ? new LocalTime(8, 0, 0) : new DateTime(start).toLocalTime();
    workDayStop = stop == null ? new LocalTime(16, 0, 0) : new DateTime(stop).toLocalTime();
  }

  LocalTime getWorkDayStart() {
    return workDayStart;
  }

  LocalTime getWorkDayStop() {
    return workDayStop;
  }

  /**
   * Metoden må alltid returnere et klokkeslett mellom de 2 punktene definert i kallet
   * setWorkdayStartAndStop, selv om startDate ikke behøver å følge regelen selv.
   * <p/>
   * På denne måten blir kl 15:07 + 0,25 arbeidsdager kl 9:07, og kl 4:00 pluss 0,5 arbeidsdager lik kl 12:00.
   * <p/>
   */
  public Date getWorkdayIncrement(Date startDate, float incrementInWorkdays) {
    DateTime dateTimeStartDate = new DateTime(startDate);

    APeriod incrementPeriod = new APeriod(incrementInWorkdays);
    DateTime workDay = incrementPeriod.moveToWorkDay(dateTimeStartDate);

    return workDay.toDate();
  }

  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }
}
