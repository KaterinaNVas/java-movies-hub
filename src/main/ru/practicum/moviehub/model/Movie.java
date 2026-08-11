package ru.practicum.moviehub.model;

public class Movie {
    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    private String title;
    private int year;

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(int year) {
        this.year = year;
    }
}