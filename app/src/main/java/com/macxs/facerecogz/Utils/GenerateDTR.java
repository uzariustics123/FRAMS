package com.macxs.facerecogz.Utils;

import android.content.Intent;
import android.util.Log;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class GenerateDTR {

    String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    public String getHTMLreport(Map<String, Object> employeeData, ArrayList<Map<String, Object>> attendances, int month, int yr) {
        String empName = employeeData.get("lastname").toString() + ", " + employeeData.get("firstname").toString();
        String monthOfDate = months[month] + " " + String.valueOf(yr);
        LocalDate preferedDate = LocalDate.of(yr, month + 1, 1);
        Log.e("attndances", String.valueOf(attendances.size()));
        int numberOfDays = preferedDate.lengthOfMonth();
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("h:mm a")
                .optionalStart()
                .appendPattern("hh:mm a")
                .optionalEnd()
                .toFormatter();
        String defInTimeAMstr  = String.valueOf(employeeData.get("am-in-time"));
        String defOutTimeAMstr  = String.valueOf(employeeData.get("am-out-time"));
        String defInTimePMstr  = String.valueOf(employeeData.get("pm-in-time"));
        String defOutTimePMstr  = String.valueOf(employeeData.get("pm-out-time"));

        LocalTime defInTimeAM = LocalTime.parse(defInTimeAMstr, formatter);
        LocalTime defOutTimeAM = LocalTime.parse(defOutTimeAMstr, formatter);
        LocalTime defInTimePM = LocalTime.parse(defInTimePMstr, formatter);
        LocalTime defOutTimePM = LocalTime.parse(defOutTimePMstr, formatter);

        Duration defAmDuration = Duration.between(defInTimeAM, defOutTimeAM);
        Duration defPmDuration = Duration.between(defInTimePM, defOutTimePM);
        Duration totalDefDuration = defAmDuration.plus(defPmDuration);
        long defHrDur = totalDefDuration.toHours();
        long defMinDur = totalDefDuration.toMinutes() % 60;

        Map<String, String> dtrTimeSched = new HashMap<>();
        dtrTimeSched.put("am-in-time", defInTimeAMstr);
        dtrTimeSched.put("am-out-time", defOutTimeAMstr);
        dtrTimeSched.put("pm-in-time", defInTimePMstr);
        dtrTimeSched.put("pm-out-time", defOutTimePMstr);
        String dtrRow = "";
        Log.e("for the month of", preferedDate.getMonth().toString());
        Log.e("attends", String.valueOf(attendances.size()));

        //count number of days of the month
        for (int day = 1; day < numberOfDays; day++) {
            dtrRow += "<tr>";
            dtrRow += "<td class='borderd'>" + String.valueOf(day) + "</td>";
            String arrivalAM = "<td class='borderd'>--:-- --</td>";
            String departureAM = "<td class='borderd'>--:-- --</td>";
            String arrivalPM = "<td class='borderd'>--:-- --</td>";
            String departurePM = "<td class='borderd'>--:-- --</td>";
            String totalLate = "0hr : 0mins";
            String undertime = "0hr : 0mins";
            boolean amAttendanceIsDone = false;
            boolean pmAttendanceIsDone = false;
            int totalLateHr = 0;
            int totalLateMin = 0;
            int totalUndertimeHr = 0;
            int totalUndertimeMin = 0;

            LocalTime attendedAmInTime = null;
            LocalTime attendedAmOutTime = null;
            LocalTime attendedPmInTime = null;
            LocalTime attendedPmOutTime = null;


            LocalTime amEndTime = LocalTime.parse("11:59 AM", formatter);
            LocalTime pmEndTime = LocalTime.parse("5:00 PM", formatter);
//                        LocalTime amStartTime = LocalTime.parse("8:00 AM", formatter);
            LocalTime pmStartTime = LocalTime.parse("1:00 PM", formatter);
            //each attendance
            for (int attTimes = 0; attTimes < attendances.size(); attTimes++) {
                try {
                    Map attendance = attendances.get(attTimes);
                    if (Integer.parseInt(attendance.get("day").toString()) == day) {

                        String timeStr = attendance.get("time").toString();
                        LocalTime time = LocalTime.parse(timeStr, formatter);
                        String type = attendance.get("type").toString();

                        //am in
                        if (timeStr.contains("AM") && type.equalsIgnoreCase("arrival")) {
                            arrivalAM = "<td class='borderd'>" + timeStr + "</td>";
                            //late calc
                            if (time.isAfter(defInTimeAM) ){
                                int amLateHr = time.minusHours(defInTimeAM.getHour()).getHour();
                                int amLateMin = time.minusMinutes(defInTimeAM.getMinute()).getMinute();
                                totalLateHr += amLateHr;
                                totalLateMin += amLateMin;
                            }
                            attendedAmInTime = time;

                        }
                        //am out
                        else if (timeStr.contains("AM") || (time.isAfter(amEndTime) && time.isBefore(pmStartTime)) && type.equalsIgnoreCase("departure")) {
                            departureAM = "<td class='borderd'>" + timeStr + "</td>";
                            amAttendanceIsDone = true;
                            attendedAmOutTime = time;
                        }
                        //pm in
                        else if (timeStr.contains("PM") && type.equalsIgnoreCase("arrival")) {
                            arrivalPM = "<td class='borderd'>" + timeStr + "</td>";
                            if (time.isAfter(defInTimePM) ){
                                int pmLateHr = time.minusHours(defInTimePM.getHour()).getHour();
                                int pmLateMin = time.minusMinutes(defInTimePM.getMinute()).getMinute();
                                totalLateHr += pmLateHr;
                                totalLateMin += pmLateMin;
                            }
                            attendedPmInTime = time;
                        }
                        //out pm
                        else if (timeStr.contains("PM") && type.equalsIgnoreCase("departure")) {
                            departurePM = "<td class='borderd'>" + timeStr + "</td>";
                            pmAttendanceIsDone = true;
                            attendedPmOutTime = time;
                        }

                        Log.e("dtr", "day: " + String.valueOf(day) + " time:" + timeStr + " type:" + type + " month:" + String.valueOf(month));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if (amAttendanceIsDone && pmAttendanceIsDone){
                Duration amDuration = Duration.between(attendedAmInTime, attendedAmOutTime);
                Duration pmDuration = Duration.between(attendedPmInTime, attendedPmOutTime);
                Duration totalRenderedTime = amDuration.plus(pmDuration);
                Log.e("total rendered hr time", String.valueOf(totalRenderedTime.toHours()));
                Log.e("total rendered min time", String.valueOf(totalRenderedTime.toMinutes() % 60));
                Duration undertimeDuration = totalDefDuration.minus(totalRenderedTime);
                long hrDur = undertimeDuration.toHours();
                long minDur = undertimeDuration.toMinutes() % 60;
                if (hrDur <= 0){
                    undertime = String.valueOf(hrDur)  +"hrs : " + String.valueOf(minDur % 60)+ "mins";
                }else {
                    undertime = String.valueOf(hrDur)  +"hrs : " + String.valueOf(minDur % 60)+ "mins";
                }


            } else if (amAttendanceIsDone) {
                Duration amDuration = Duration.between(attendedAmInTime, attendedAmOutTime);
                Duration undertimeDuration = totalDefDuration.minus(amDuration);
                long hrDur = undertimeDuration.toHours();
                long minDur = undertimeDuration.toMinutes() % 60;
                undertime = String.valueOf(hrDur)  +"hrs : " + String.valueOf(minDur % 60)+ "mins";
            } else if (pmAttendanceIsDone) {
                Duration pmDuration = Duration.between(attendedPmInTime, attendedPmOutTime);
                Duration undertimeDuration = totalDefDuration.minus(pmDuration);
                long hrDur = undertimeDuration.toHours();
                long minDur = undertimeDuration.toMinutes() % 60;
                undertime = String.valueOf(hrDur)  +"hrs : " + String.valueOf(minDur % 60)+ "mins";
            }
            totalLate = String.valueOf(totalLateHr)  +"hrs : " + String.valueOf(totalLateMin % 60)+ "mins";
            String totalLatestr = "<td class='borderd'>"+totalLate+"</td>";
            String underTime = "<td class='borderd'>"+undertime+ "</td>";

            dtrRow += arrivalAM + departureAM + arrivalPM + departurePM + totalLatestr + underTime;

        }
        dtrRow += "</tr>";
        return dtrV2(empName, monthOfDate, dtrRow, dtrTimeSched);
    }


    private String dtrV2(String empname, String date, String dtr, Map<String, String> dtrSched) {

        return "<!DOCTYPE html>\n" +
                "<html dir='ltr' lang='en'>\n" +
                "  <head>\n" +
                "    <meta charset='UTF-8' />\n" +
                "    <meta http-equiv='X-UA-Compatible' content='IE=edge' />\n" +
                "<meta name='viewport' content='width=device-width, maximum-scale=0, user-scalable=yes'>" +
                "    <style type='text/css'>\n" +
                "      body {\n" +
                "        background-color: #f7f7f7;\n" +
                "        font-family: 'Gill Sans', 'Gill Sans MT', Calibri, 'Trebuchet MS',\n" +
                "          sans-serif;\n" +
                "        color: #333;\n" +
                "        text-align: left;\n" +
                "        font-size: 12px;\n" +
                "        margin: 0;\n" +
                "      }\n" +
                "      .container {\n" +
                "        margin: 0 auto;\n" +
                "        margin-top: 35px;\n" +
                "        padding: 40px;\n" +
                "        width: 750px;\n" +
                "        height: auto;\n" +
                "        background-color: #fff;\n" +
                "      }\n" +
                "      th {\n" +
                "        font-weight: normal;\n" +
                "        padding: opx;\n" +
                "        margin: 0px auto;\n" +
                "      }\n" +
                "      td {\n" +
                "        padding-top: 4px;\n" +
                "        padding-bottom: 4px;\n" +
                "        padding-left: 4px;\n" +
                "        padding-right: 8px;\n" +
                "        text-align: center;\n" +
                "        font-size: 10px;\n" +
                "        max-lines: 1;\n" +
                "        white-space: nowrap;\n" +
                "      }\n" +
                "      .center-text {\n" +
                "        text-align: center;\n" +
                "      }\n" +
                "      .left-text{\n" +
                "        padding-top: 8px;\n" +
                "        text-align: start;\n" +
                "      }\n" +
                "      .table-root {\n" +
                "        display: flex;\n" +
                "      }\n" +
                "      table {\n" +
                "        /* border: 1px solid #333; */\n" +
                "        border-collapse: collapse;\n" +
                "        margin: 0 auto;\n" +
                "        width: 740px;\n" +
                "      }\n" +
                "      .dtr-title {\n" +
                "        font-size: 12sp;\n" +
                "        margin-bottom: 15px;\n" +
                "        margin-top: 18px;\n" +
                "        max-lines: 1;\n" +
                "        font-weight: bold;\n" +
                "      }\n" +
                "      .employee-name {\n" +
                "        font-size: 11ps;\n" +
                "        margin-top: 18px;\n" +
                "        max-lines: 1;\n" +
                "        font-weight: bold;\n" +
                "        text-align: center;\n" +
                "      }\n" +
                "      .underline {\n" +
                "        border-bottom: 1px solid black;\n" +
                "      }\n" +
                "      .space-top {\n" +
                "        margin-top: 18px;\n" +
                "      }\n" +
                "      .space-bot {\n" +
                "        padding-bottom: 18px;\n" +
                "      }\n" +
                "      .borderd {\n" +
                "        border: 1px solid black;\n" +
                "      }\n" +
                "      .padd {\n" +
                "        padding: 12px;\n" +
                "      }\n" +
                "      .space-top-table {\n" +
                "        padding-top: 15px;\n" +
                "      }\n" +
                "      .dtr-col {\n" +
                "        border: 1px solid black;\n" +
                "        padding: 4px;\n" +
                "        \n" +
                "        font-size: 12px;\n" +
                "        text-align: center;\n" +
                "      }\n" +
                "      h4,\n" +
                "      p {\n" +
                "        margin: 0px;\n" +
                "      }\n" +
                "      .keys{\n" +
                "        text-align: center;\n" +
                "        /* padding-top: 1px;\n" +
                "        padding-bottom: 1px; */\n" +
                "        border: 1px solid black;\n" +
                "      }\n" +
                "      .total-key{\n" +
                "        text-align: center;\n" +
                "        font-size: 12px;\n" +
                "        font-weight: bold;\n" +
                "        border: 1px solid black;\n" +
                "      }\n" +
                "      .signature{\n" +
                "        padding-top: 40px;\n" +
                "        border-bottom: 1px solid black;\n" +
                "      }\n" +
                "      .signature-below{\n" +
                "        font-weight: bolder;\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <div class='container'>\n" +
                "      <p>Civil Service Form No. 48</p>\n" +
                "      <div class='table-root'>\n" +
                "        <table class='table-left'>\n" +
                "            <caption class='dtr-title'>\n" +
                "                DAILY TIME RECORD\n" +
                "              </caption>\n" +
                "              <thead>\n" +
                "                <tr>\n" +
                "                  <th colspan='7' class='employee-name underline'>\n" + empname +
                "                  </th>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <th colspan='7' class='center-text space-bot'>(NAME)</th>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <th class='space-top' colspan='2'>For the month of</td>\n" +
                "                  <th class='underline center-text' colspan='5'>" + date + "</td>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                  <th class='space-top space-top-table space-bot' colspan='4'>\n" +
                "                    Official hours for arrival and departure\n" +
                "                  </th>\n" +
                "                  <th class='space-top-table space-bot center-text' colspan='5'>\n" +
                                    dtrSched.get("am-in-time")+ " to " + dtrSched.get("am-out-time") + "<br />" +dtrSched.get("pm-in-time") + " to " + dtrSched.get("pm-out-time") +"\n" +
                "                  </th>\n" +
                "                </tr>\n" +
                "              </thead>\n" +
                "              <tbody>\n" +
                "                <tr>\n" +
                "                  <th class='dtr-col'>Day</th>\n" +
                "                  <th class='dtr-col' colspan='2'>AM</th>\n" +
                "                  <th class='dtr-col' colspan='2'>PM</th>\n" +
                "                  <th class='dtr-col' colspan='2'>Late & Undertime</th>\n" +
                "                </tr>\n" +
                "                <tr>\n" +
                "                    <td class='keys'>Date</td>\n" +
                "                    <td class='keys'>Arrival</td>\n" +
                "                    <td class='keys'>Departure</td>\n" +
                "                    <td class='keys'>Arrival</td>\n" +
                "                    <td class='keys'>Departure</td>\n" +
                "                    <td class='keys'>Late</td>\n" +
                "                    <td class='keys'>Undertime</td>\n" +
                "                  </tr>\n" +
                "                <tr>\n" +
                dtr +
                "                </tr>\n" +
//                "                <tr>\n" +
//                "                    <td class='total-key' colspan='5'>TOTAL</td>\n" +
//                "                    <td class='keys'>0</td>\n" +
//                "                    <td class='keys'>0</td>\n" +
//                "                  </tr>\n" +
                "              </tbody>\n" +
                "              <tfoot>\n" +
                "                <tr>\n" +
                "                  <td colspan='7' class='left-text'>I certify on my honor that the above is a true and correct report of the<br>\n" +
                "                    hours work performed, record of which was a daily at the time of arrival<br>\n" +
                "                    and departure from office.</td>\n" +
                "                </tr>\n" +
                "                <tr><td colspan='7' class='signature'></td></tr>\n" +
                "            <tr><td colspan='7' class='signature-below'>VERIFIED as to the prescribed office hours:</td></tr>\n" +
                "            <tr><td colspan='7' class='signature'></td></tr>\n" +
                "            <tr><td colspan='7' class='signature-below'>In Charge</td></tr>\n" +
                "              </tfoot>\n" +
                "            </table>\n" +
                "        &nbsp;\n" +
                "        <!-- 2nd table -->\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </body>\n" +
                "</html>\n";
    }
}
