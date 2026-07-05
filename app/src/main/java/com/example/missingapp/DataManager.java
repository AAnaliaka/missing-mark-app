package com.example.missingapp;

import java.util.ArrayList;
import java.util.List;

public class DataManager {
    public static class Report {
        public String courseName, courseCode, status, date;
        public Report(String n, String c, String s, String d) {
            courseName = n; courseCode = c; status = s; date = d;
        }
    }

    public static class Alert {
        public String title, message, time;
        public Alert(String t, String m, String tm) {
            title = t; message = m; time = tm;
        }
    }

    public static class CourseUnit {
        public String code, name, grade;
        public CourseUnit(String c, String n, String g) {
            code = c; name = n; grade = g;
        }
    }

    private static List<Report> reports = new ArrayList<>();
    private static List<Alert> alerts = new ArrayList<>();
    private static List<CourseUnit> registeredUnits = new ArrayList<>();

    static {
        // Initialize with default data
        registeredUnits.add(new CourseUnit("BIT 311", "Mobile Application Development", "M"));
        registeredUnits.add(new CourseUnit("CCS 202", "Operating Systems", "A"));
        registeredUnits.add(new CourseUnit("MATH 301", "Calculus III", "B+"));
        registeredUnits.add(new CourseUnit("BIT 312", "Database Systems", "M"));
    }

    public static void addReport(Report r) { reports.add(r); }
    public static List<Report> getReports() { return reports; }

    public static void addAlert(Alert a) { alerts.add(0, a); } 
    public static List<Alert> getAlerts() { return alerts; }
    
    public static List<CourseUnit> getUnits() { return registeredUnits; }
}