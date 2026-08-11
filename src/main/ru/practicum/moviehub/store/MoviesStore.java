package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int currentId = 1;

    public Map<Integer, Movie> getMovies() {
        return movies;
    }

    public int getCurrentId() {
        return currentId;
    }

    public void setCurrentId(int currentId) {
        this.currentId = currentId;
    }

    public Movie addMovie(Movie movie) {
        int id = currentId++;
        movies.put(id, movie);
        return movie;
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public boolean deleteMovie(int id) {
        return movies.remove(id) != null;
    }

    public boolean containsMovie(int id) {
        return movies.containsKey(id);
    }

    public Map<Integer, Movie> getAllMoviesWithIds() {
        return new HashMap<>(movies);
    }

    public List<Movie> getMoviesByYear(int year) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }
        return result;
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public void clear() {
        movies.clear();
        currentId = 1;
    }

    public int size() {
        return movies.size();
    }
}