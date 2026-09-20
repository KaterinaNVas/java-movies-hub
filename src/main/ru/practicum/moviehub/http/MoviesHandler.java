package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            handleGetMovies(ex);
            return;
        }

        if (method.equalsIgnoreCase("POST")) {
            handleCreateMovie(ex);
            return;
        }

        if (method.equalsIgnoreCase("DELETE")) {
            handleDeleteMovie(ex);
            return;
        }

        ErrorResponse errorResponse = new ErrorResponse("Метод не поддерживается");

        sendJson(ex, 405, gson.toJson(errorResponse));
    }


    private void handleDeleteMovie(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        String idPart = path.substring("/movies/".length());

        int id;

        try {
            id = Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный ID");

            sendJson(ex, 400, gson.toJson(errorResponse));
            return;
        }

        boolean deleted = store.deleteMovie(id);

        if (!deleted) {
            ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");

            sendJson(ex, 404, gson.toJson(errorResponse));
            return;
        }

        sendNoContent(ex);
    }

    private void handleGetMovies(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if (!path.equals("/movies")) {
            handleGetMovieById(ex, path);
            return;
        }

        String query = ex.getRequestURI().getQuery();

        if (query != null) {
            handleGetMoviesByYear(ex, query);
            return;
        }

        List<Movie> movies = store.getAllMovies();

        sendJson(ex, 200, gson.toJson(movies));
    }

    private void handleGetMoviesByYear(HttpExchange ex, String query) throws IOException {
        if (!query.startsWith("year=")) {
            sendInvalidYearQueryResponse(ex);
            return;
        }

        String yearPart = query.substring("year=".length());

        int year;

        try {
            year = Integer.parseInt(yearPart);
        } catch (NumberFormatException e) {
            sendInvalidYearQueryResponse(ex);
            return;
        }

        List<Movie> movies = store.getMoviesByYear(year);

        sendJson(ex, 200, gson.toJson(movies));
    }

    private void handleGetMovieById(HttpExchange ex, String path) throws IOException {
        String idPart = path.substring("/movies/".length());

        int id;

        try {
            id = Integer.parseInt(idPart);
        } catch (NumberFormatException e) {
            ErrorResponse errorResponse = new ErrorResponse("Некорректный ID");

            sendJson(ex, 400, gson.toJson(errorResponse));
            return;
        }

        Movie movie = store.getMovieById(id);

        if (movie == null) {
            ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");

            sendJson(ex, 404, gson.toJson(errorResponse));
            return;
        }

        sendJson(ex, 200, gson.toJson(movie));
    }

    private void handleCreateMovie(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");

        if (!isJsonContentType(contentType)) {
            ErrorResponse errorResponse = new ErrorResponse("Неподдерживаемый Content-Type");

            sendJson(ex, 415, gson.toJson(errorResponse));

            return;
        }

        Movie movie;

        try (Reader reader = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8)) {
            movie = gson.fromJson(reader, Movie.class);
        } catch (JsonParseException e) {
            sendBadJsonResponse(ex);
            return;
        }

        if (movie == null) {
            sendBadJsonResponse(ex);
            return;
        }

        List<String> validationErrors = new ArrayList<>();

        validateTitle(movie, validationErrors);
        validateYear(movie, validationErrors);

        if (!validationErrors.isEmpty()) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", validationErrors);

            sendJson(ex, 422, gson.toJson(errorResponse));

            return;
        }

        Movie createdMovie = store.addMovie(movie);

        sendJson(ex, 201, gson.toJson(createdMovie));
    }

    private void sendInvalidYearQueryResponse(HttpExchange ex) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse("Некорректный параметр запроса — 'year'");

        sendJson(ex, 400, gson.toJson(errorResponse));
    }

    private boolean isJsonContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String mediaType = contentType.split(";", 2)[0].trim();

        return mediaType.equalsIgnoreCase("application/json");
    }

    private void validateTitle(Movie movie, List<String> validationErrors) {
        String title = movie.getTitle();

        if (title == null || title.isBlank()) {
            validationErrors.add("название не должно быть пустым");
        } else if (title.length() > 100) {
            validationErrors.add("название не должно быть длиннее 100 символов");
        }
    }

    private void validateYear(Movie movie, List<String> validationErrors) {
        Integer year = movie.getYear();
        int maxYear = LocalDate.now().getYear() + 1;

        if (year == null) {
            validationErrors.add("Год должен быть указан");
            return;
        }

        if (year < 1888) {
            validationErrors.add("Год не должен быть ранее 1888");
            return;
        }

        if (year > maxYear) {
            validationErrors.add("Год не должен быть позднее текущего более чем на один год");
        }
    }

    private void sendBadJsonResponse(HttpExchange ex) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse("Некорректный JSON");

        sendJson(ex, 400, gson.toJson(errorResponse));
    }

    public MoviesStore getStore() {
        return store;
    }

    public Gson getGson() {
        return gson;
    }
}
