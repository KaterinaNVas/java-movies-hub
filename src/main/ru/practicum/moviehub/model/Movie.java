package ru.practicum.moviehub.model;

public class Movie {
    private String title;
    private Integer year;
    private int id;

    public Movie(String title, Integer year) {
        this.title = title;
        this.year = year;
    }
    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}